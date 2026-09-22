package com.example.crackcs.evaluation.retrieval;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class KnowledgeEvidenceSelector {

    private static final int MIN_EVIDENCE_LIMIT = 1;
    private static final int MAX_EVIDENCE_LIMIT = 20;
    private static final double MINIMUM_RELATIVE_SCORE = 0.5;
    private static final double MINIMUM_CONFLICT_QUERY_COVERAGE = 0.5;

    public RetrievalResult selectEvidence(List<KnowledgeChunk> candidates, RetrievalQuery query, int limit) {
        validateEvidenceLimit(limit);
        Set<String> concepts = tokens(String.join(" ", query.conceptNames()));
        Set<String> searchTokens = tokens(query.searchText());
        List<RetrievedChunk> rankedChunks = candidates.stream()
                .map(chunk -> new RetrievedChunk(chunk, score(chunk, concepts, searchTokens)))
                .filter(retrievedChunk -> retrievedChunk.relevanceScore() > 0)
                .sorted(Comparator.comparingDouble(RetrievedChunk::relevanceScore).reversed()
                        .thenComparing(retrievedChunk -> retrievedChunk.chunk().getDocumentId(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(retrievedChunk -> retrievedChunk.chunk().getSequenceNo()))
                .toList();
        // K is an upper bound, not a quota: weak matches should not dilute stronger evidence.
        double minimumScore = rankedChunks.isEmpty()
                ? 0
                : rankedChunks.getFirst().relevanceScore() * MINIMUM_RELATIVE_SCORE;
        Set<KnowledgeChunk> reservedChunks = strongestSupportedConceptEvidence(
                rankedChunks, query.conceptNames(), searchTokens);
        List<RetrievedChunk> initialSelection = selectRankedEvidence(
                rankedChunks, reservedChunks, minimumScore, limit);
        if (!hasExplicitConflict(initialSelection)) {
            // Do not hide a concise opposing source just because the agreeing source is longer.
            rankedChunks.stream()
                    .filter(retrievedChunk -> supportingMatches(
                            retrievedChunk.chunk().getContent(), concepts, searchTokens) >= 2)
                    .filter(retrievedChunk -> hasConflictQueryCoverage(
                            retrievedChunk.chunk().getContent(), searchTokens))
                    .filter(retrievedChunk -> initialSelection.stream().anyMatch(
                            anchor -> conflicts(anchor.chunk(), retrievedChunk.chunk())))
                    .findFirst()
                    .ifPresent(retrievedChunk -> reservedChunks.add(retrievedChunk.chunk()));
        }
        List<RetrievedChunk> selectedChunks = selectRankedEvidence(
                rankedChunks, reservedChunks, minimumScore, limit);
        return new RetrievalResult(
                selectedChunks,
                selectedChunks.isEmpty(),
                hasExplicitConflict(selectedChunks)
        );
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

    private List<RetrievedChunk> selectRankedEvidence(List<RetrievedChunk> rankedChunks,
                                                      Set<KnowledgeChunk> reservedChunks,
                                                      double minimumScore, int limit) {
        if (rankedChunks.isEmpty()) {
            return List.of();
        }
        Set<KnowledgeChunk> preferredChunks = new LinkedHashSet<>(reservedChunks);
        preferredChunks.add(rankedChunks.getFirst().chunk());
        Set<KnowledgeChunk> selectedChunks = new LinkedHashSet<>();
        rankedChunks.stream()
                .filter(retrievedChunk -> preferredChunks.contains(retrievedChunk.chunk()))
                .limit(limit)
                .forEach(retrievedChunk -> selectedChunks.add(retrievedChunk.chunk()));
        for (RetrievedChunk retrievedChunk : rankedChunks) {
            if (selectedChunks.size() == limit) {
                break;
            }
            if (retrievedChunk.relevanceScore() >= minimumScore) {
                selectedChunks.add(retrievedChunk.chunk());
            }
        }
        // Reservation changes membership, not the externally visible score/ID ordering.
        return rankedChunks.stream()
                .filter(retrievedChunk -> selectedChunks.contains(retrievedChunk.chunk()))
                .toList();
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

    private Set<KnowledgeChunk> strongestSupportedConceptEvidence(List<RetrievedChunk> rankedChunks,
                                                                  List<String> conceptNames,
                                                                  Set<String> searchTokens) {
        Set<KnowledgeChunk> evidenceChunks = new LinkedHashSet<>();
        for (String conceptName : conceptNames) {
            Set<String> conceptTokens = tokens(conceptName);
            if (conceptTokens.isEmpty()) {
                continue;
            }
            // Preserve a concise source for a distinct concept, but never rescue a name-only match.
            rankedChunks.stream()
                    .filter(retrievedChunk -> supportsConcept(
                            retrievedChunk.chunk().getContent(), conceptTokens, searchTokens))
                    .findFirst()
                    .ifPresent(retrievedChunk -> evidenceChunks.add(retrievedChunk.chunk()));
        }
        return evidenceChunks;
    }

    private boolean supportsConcept(String content, Set<String> conceptTokens, Set<String> searchTokens) {
        String normalized = content.toLowerCase(Locale.ROOT);
        return conceptTokens.stream().allMatch(normalized::contains)
                && supportingMatches(content, conceptTokens, searchTokens) >= 2;
    }

    private long supportingMatches(String content, Set<String> conceptTokens, Set<String> searchTokens) {
        String normalized = content.toLowerCase(Locale.ROOT);
        return searchTokens.stream()
                .filter(token -> !conceptTokens.contains(token))
                .filter(normalized::contains)
                .count();
    }

    private boolean hasConflictQueryCoverage(String content, Set<String> searchTokens) {
        Set<String> contentTokens = tokens(content);
        long matchingTokens = contentTokens.stream().filter(searchTokens::contains).count();
        // Only a rescue guard: normalizing the ranking score would favor short name-only fragments.
        return !contentTokens.isEmpty()
                && matchingTokens >= contentTokens.size() * MINIMUM_CONFLICT_QUERY_COVERAGE;
    }

    private boolean hasExplicitConflict(List<RetrievedChunk> selectedChunks) {
        for (int left = 0; left < selectedChunks.size(); left++) {
            for (int right = left + 1; right < selectedChunks.size(); right++) {
                if (conflicts(selectedChunks.get(left).chunk(), selectedChunks.get(right).chunk())) {
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
        Set<String> tokenSet = new LinkedHashSet<>();
        Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .forEach(tokenSet::add);
        return tokenSet;
    }
}
