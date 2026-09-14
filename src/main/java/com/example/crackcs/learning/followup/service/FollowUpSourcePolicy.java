package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunkSearchStatus;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConcept;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationEvidence;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class FollowUpSourcePolicy {
    /** 첫 번째 제한 사유를 반환하며, 제한이 없으면 Optional.empty()를 반환한다. */
    public Optional<FollowUpReason> findUnavailabilityReason(Evaluation evaluation) {
        if (evaluation == null) {
            throw new IllegalArgumentException("evaluation is required before checking follow-up eligibility");
        }
        Question question = evaluation.getAnswer().getQuestion();
        if (question.getType() == QuestionType.FOLLOW_UP) {
            return Optional.of(FollowUpReason.FOLLOW_UP_LIMIT);
        }
        if (hasUnusableEvaluation(evaluation)) {
            return Optional.of(FollowUpReason.EVALUATION_NOT_ELIGIBLE);
        }
        if (!hasAvailableQuestionContent(question)) {
            return Optional.of(FollowUpReason.CONTENT_UNAVAILABLE);
        }
        if (hasCompletedEvaluationWithoutEvidence(evaluation)) {
            return Optional.of(FollowUpReason.CONTENT_UNAVAILABLE);
        }
        return Optional.empty();
    }

    private boolean hasCompletedEvaluationWithoutEvidence(Evaluation evaluation) {
        return evaluation.getStatus() == EvaluationStatus.EVALUATED && availableEvidence(evaluation).isEmpty();
    }

    public FollowUpRequest request(Evaluation evaluation) {
        Question question = evaluation.getAnswer().getQuestion();
        QuestionConcept selected = selectConcept(question, evaluation);
        return new FollowUpRequest(question.getContent(), selected.getConcept().getId(),
                selected.getConcept().getName(), evaluation.getVerdict(), evaluation.getFeedback(),
                evaluation.getOmissions(), evaluation.getMisconceptions(), availableEvidence(evaluation).stream()
                .map(chunk -> new FollowUpRequest.Evidence(chunk.getId(), chunk.getContent())).toList());
    }

    public List<KnowledgeChunk> availableEvidence(Evaluation evaluation) {
        Long topicId = evaluation.getAnswer().getQuestion().getTopicId();
        return evaluation.getEvidence().stream().map(EvaluationEvidence::getChunk)
                .filter(chunk -> isAvailableEvidenceForTopic(chunk, topicId))
                .sorted(Comparator.comparing(KnowledgeChunk::getId)).toList();
    }
    private boolean hasUnusableEvaluation(Evaluation evaluation) {
        return evaluation.getStatus() == EvaluationStatus.FAILED
                || evaluation.getStatus() == EvaluationStatus.NEEDS_REVIEW;
    }

    private boolean hasAvailableQuestionContent(Question question) {
        return question.getStatus() == QuestionStatus.PUBLISHED && question.getTopic().isActive()
                && hasOnlyActiveConcepts(question);
    }

    private boolean hasOnlyActiveConcepts(Question question) {
        return !question.getQuestionConcepts().isEmpty()
                && question.getQuestionConcepts().stream().allMatch(qc -> qc.getConcept().isActive());
    }

    private QuestionConcept selectConcept(Question question, Evaluation evaluation) {
        return question.getQuestionConcepts().stream()
                .min(conceptPriority(evaluation)).orElseThrow();
    }

    private Comparator<QuestionConcept> conceptPriority(Evaluation evaluation) {
        // 종합 판정과 일치 → 필수 → 가중치 내림차순 → ID 순으로 하나를 결정한다.
        return Comparator.comparing((QuestionConcept concept) -> !matchesOverallVerdict(concept, evaluation))
                .thenComparing(concept -> !concept.isRequired())
                .thenComparing(QuestionConcept::getWeight, Comparator.reverseOrder())
                .thenComparing(concept -> concept.getConcept().getId());
    }

    private boolean matchesOverallVerdict(QuestionConcept concept, Evaluation evaluation) {
        return evaluation.getConcepts().stream()
                .anyMatch(result -> result.getConceptId().equals(concept.getConcept().getId())
                        && result.getVerdict() == evaluation.getVerdict());
    }

    private boolean isAvailableEvidenceForTopic(KnowledgeChunk chunk, Long topicId) {
        return chunk.getDocument().getStatus() == KnowledgeDocumentStatus.PUBLISHED
                && chunk.getDocument().getTopic().isActive()
                && chunk.getDocument().getTopic().getId().equals(topicId)
                && chunk.getSearchStatus() != KnowledgeChunkSearchStatus.EMBEDDING_FAILED;
    }
}
