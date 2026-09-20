package com.example.crackcs.evaluation.retrieval;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private static final int MIN_EVIDENCE_LIMIT = 1;
    private static final int MAX_EVIDENCE_LIMIT = 20;
    private static final double MINIMUM_RELATIVE_SCORE = 0.5;
    private static final double MINIMUM_CONFLICT_QUERY_COVERAGE = 0.5;

    private final KnowledgeChunkRepository knowledgeChunkRepository;

    @Override
    public RetrievalResult retrieve(RetrievalQuery query, int limit) {
        validateEvidenceLimit(limit);
        Set<String> concepts = tokens(String.join(" ", query.conceptNames()));
        Set<String> searchTokens = tokens(query.searchText());
        List<RetrievedChunk> ranked = knowledgeChunkRepository.findPublishedSearchableByTopicId(query.topicId()).stream()
                .map(chunk -> new RetrievedChunk(chunk, score(chunk, concepts, searchTokens)))
                .filter(result -> result.relevanceScore() > 0)
                .sorted(Comparator.comparingDouble(RetrievedChunk::relevanceScore).reversed()
                        .thenComparing(result -> result.chunk().getDocumentId())
                        .thenComparing(result -> result.chunk().getSequenceNo()))
                .toList();
        // K is an upper bound, not a quota: weak matches should not dilute stronger evidence.
        double minimumScore = ranked.isEmpty() ? 0 : ranked.getFirst().relevanceScore() * MINIMUM_RELATIVE_SCORE;
        Set<Long> reservedIds = strongestSupportedConceptEvidence(ranked, query.conceptNames(), searchTokens);
        List<RetrievedChunk> initial = selectEvidence(ranked, reservedIds, minimumScore, limit);
        if (!hasExplicitConflict(initial)) {
            // Do not hide a concise opposing source just because the agreeing source is longer.
            ranked.stream()
                    .filter(result -> supportingMatches(result.chunk().getContent(), concepts, searchTokens) >= 2)
                    .filter(result -> hasConflictQueryCoverage(result.chunk().getContent(), searchTokens))
                    .filter(result -> initial.stream().anyMatch(anchor -> conflicts(anchor.chunk(), result.chunk())))
                    .findFirst()
                    .ifPresent(result -> reservedIds.add(result.chunk().getId()));
        }
        List<RetrievedChunk> selected = selectEvidence(ranked, reservedIds, minimumScore, limit);
        return new RetrievalResult(selected, selected.isEmpty(), hasExplicitConflict(selected));
    }

    private void validateEvidenceLimit(int limit) {
        if (isOutsideAllowedEvidenceLimit(limit)) {
            throw new IllegalArgumentException("evidence limit must be between "
                    + MIN_EVIDENCE_LIMIT + " and " + MAX_EVIDENCE_LIMIT);
        }
    }

    private boolean isOutsideAllowedEvidenceLimit(int limit) {
        return limit < MIN_EVIDENCE_LIMIT || limit > MAX_EVIDENCE_LIMIT;
    }

    private List<RetrievedChunk> selectEvidence(List<RetrievedChunk> ranked, Set<Long> reservedIds,
                                               double minimumScore, int limit) {
        if (ranked.isEmpty()) {
            return List.of();
        }
        Set<Long> preferredIds = new LinkedHashSet<>(reservedIds);
        preferredIds.add(ranked.getFirst().chunk().getId());
        Set<Long> selectedIds = new LinkedHashSet<>();
        ranked.stream().filter(result -> preferredIds.contains(result.chunk().getId())).limit(limit)
                .forEach(result -> selectedIds.add(result.chunk().getId()));
        for (RetrievedChunk result : ranked) {
            if (selectedIds.size() == limit) {
                break;
            }
            if (result.relevanceScore() >= minimumScore) {
                selectedIds.add(result.chunk().getId());
            }
        }
        // Reservation changes membership, not the externally visible score/ID ordering.
        return ranked.stream().filter(result -> selectedIds.contains(result.chunk().getId())).toList();
    }

    private double score(KnowledgeChunk chunk, Set<String> concepts, Set<String> searchTokens) {
        String content = chunk.getContent().toLowerCase(Locale.ROOT);
        long conceptMatches = concepts.stream()
                .filter(content::contains)
                .count();
        long queryMatches = searchTokens.stream()
                .filter(content::contains)
                .count();
        // Without a concept match, one incidental question/reference word is not evidence.
        if (conceptMatches == 0 && queryMatches < 2) {
            return 0;
        }
        return conceptMatches * 3.0 + queryMatches;
    }

    private Set<Long> strongestSupportedConceptEvidence(List<RetrievedChunk> ranked,
                                                        List<String> conceptNames, Set<String> searchTokens) {
        Set<Long> evidenceIds = new LinkedHashSet<>();
        for (String conceptName : conceptNames) {
            Set<String> conceptTokens = tokens(conceptName);
            if (conceptTokens.isEmpty()) {
                continue;
            }
            // Preserve a concise source for a distinct concept, but never rescue a name-only match.
            ranked.stream()
                    .filter(result -> supportsConcept(result.chunk().getContent(), conceptTokens, searchTokens))
                    .findFirst()
                    .ifPresent(result -> evidenceIds.add(result.chunk().getId()));
        }
        return evidenceIds;
    }

    private boolean supportsConcept(String content, Set<String> conceptTokens, Set<String> searchTokens) {
        String normalized = content.toLowerCase(Locale.ROOT);
        return conceptTokens.stream().allMatch(normalized::contains)
                && supportingMatches(content, conceptTokens, searchTokens) >= 2;
    }

    private long supportingMatches(String content, Set<String> conceptTokens, Set<String> searchTokens) {
        String normalized = content.toLowerCase(Locale.ROOT);
        return searchTokens.stream().filter(token -> !conceptTokens.contains(token)).filter(normalized::contains).count();
    }

    private boolean hasConflictQueryCoverage(String content, Set<String> searchTokens) {
        Set<String> contentTokens = tokens(content);
        long matchingTokens = contentTokens.stream().filter(searchTokens::contains).count();
        // Only a rescue guard: normalizing the ranking score would favor short name-only fragments.
        return !contentTokens.isEmpty()
                && matchingTokens >= contentTokens.size() * MINIMUM_CONFLICT_QUERY_COVERAGE;
    }

    private boolean hasExplicitConflict(List<RetrievedChunk> selected) {
        for (int left = 0; left < selected.size(); left++) {
            for (int right = left + 1; right < selected.size(); right++) {
                if (conflicts(selected.get(left).chunk(), selected.get(right).chunk())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean conflicts(KnowledgeChunk left, KnowledgeChunk right) {
        Set<String> leftTokens = tokens(left.getContent());
        Set<String> rightTokens = tokens(right.getContent());
        long overlap = leftTokens.stream().filter(rightTokens::contains).count();
        return overlap >= 2 && isNegated(left.getContent()) != isNegated(right.getContent());
    }

    private boolean isNegated(String content) {
        return content.contains("아니다") || content.contains("않는다") || content.contains("없다");
    }

    private Set<String> tokens(String value) {
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .forEach(result::add);
        return result;
    }
}
