package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.exception.EvaluationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAdminEvaluationService implements AdminEvaluationService {

    private static final List<EvaluationStatus> REVIEW_STATUSES =
            List.of(EvaluationStatus.FAILED, EvaluationStatus.NEEDS_REVIEW);

    private final EvaluationRepository evaluations;

    @Override
    public Page<AdminEvaluationSummary> findFailures(EvaluationStatus status, Pageable pageable) {
        List<EvaluationStatus> statuses = status == null ? REVIEW_STATUSES : List.of(requireReviewStatus(status));
        return evaluations.findByStatusIn(statuses, pageable).map(this::summary);
    }

    @Override
    public AdminEvaluationDetail findFailureById(Long evaluationId) {
        Evaluation evaluation = evaluations.findAdminDetailById(evaluationId)
                .filter(candidate -> REVIEW_STATUSES.contains(candidate.getStatus()))
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));
        return new AdminEvaluationDetail(
                evaluation.getId(),
                evaluation.getAnswer().getId(),
                evaluation.getAnswer().getQuestion().getId(),
                evaluation.getAnswer().getQuestion().getContent(),
                evaluation.getAnswer().getContent(),
                evaluation.getStatus(),
                evaluation.getFailureReason(),
                evaluation.getModelName(),
                evaluation.getEvaluatorVersion(),
                evaluation.getEvaluatedAt(),
                evaluation.getEvidence().stream().sorted(Comparator.comparing(evidence -> evidence.getChunkId()))
                        .map(evidence -> new AdminEvaluationEvidence(
                                evidence.getChunkId(), evidence.getDocumentTitle(), evidence.getDocumentVersion(),
                                evidence.getStartOffset(), evidence.getEndOffset(), evidence.getContent()
                        )).toList()
        );
    }

    private AdminEvaluationSummary summary(Evaluation evaluation) {
        return new AdminEvaluationSummary(
                evaluation.getId(),
                evaluation.getAnswer().getId(),
                evaluation.getAnswer().getQuestion().getId(),
                evaluation.getStatus(),
                evaluation.getFailureReason(),
                evaluation.getModelName(),
                evaluation.getEvaluatorVersion(),
                evaluation.getEvaluatedAt()
        );
    }

    private EvaluationStatus requireReviewStatus(EvaluationStatus status) {
        if (!REVIEW_STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be FAILED or NEEDS_REVIEW");
        }
        return status;
    }
}
