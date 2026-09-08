package com.example.crackcs.evaluation.retrieval;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private final KnowledgeChunkRepository chunks;

    @Override
    public RetrievalResult retrieve(RetrievalQuery query, int limit) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("limit must be between 1 and 20");
        }
        Set<String> concepts = tokens(String.join(" ", query.conceptNames()));
        Set<String> searchTokens = tokens(query.searchText());
        List<RetrievedChunk> selected = chunks.findPublishedSearchableByTopicId(query.topicId()).stream()
                .filter(chunk -> sharesAnyToken(chunk.getContent(), concepts))
                .map(chunk -> new RetrievedChunk(chunk, score(chunk, concepts, searchTokens)))
                .filter(result -> result.relevanceScore() > 0)
                .sorted(Comparator.comparingDouble(RetrievedChunk::relevanceScore).reversed()
                        .thenComparing(result -> result.chunk().getDocumentId())
                        .thenComparing(result -> result.chunk().getSequenceNo()))
                .limit(limit)
                .toList();
        return new RetrievalResult(selected, selected.isEmpty(), hasExplicitConflict(selected));
    }

    private double score(KnowledgeChunk chunk, Set<String> concepts, Set<String> searchTokens) {
        String content = chunk.getContent().toLowerCase(Locale.ROOT);
        long conceptMatches = concepts.stream()
                .filter(content::contains)
                .count();
        long queryMatches = searchTokens.stream()
                .filter(content::contains)
                .count();
        return conceptMatches * 3.0 + queryMatches;
    }

    private boolean sharesAnyToken(String content, Set<String> expected) {
        if (expected.isEmpty()) {
            return false;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        return expected.stream().anyMatch(normalized::contains);
    }

    private boolean hasExplicitConflict(List<RetrievedChunk> selected) {
        for (int left = 0; left < selected.size(); left++) {
            for (int right = left + 1; right < selected.size(); right++) {
                Set<String> leftTokens = tokens(selected.get(left).chunk().getContent());
                Set<String> rightTokens = tokens(selected.get(right).chunk().getContent());
                long overlap = leftTokens.stream().filter(rightTokens::contains).count();
                if (overlap >= 2 && isNegated(selected.get(left).chunk().getContent())
                        != isNegated(selected.get(right).chunk().getContent())) {
                    return true;
                }
            }
        }
        return false;
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
