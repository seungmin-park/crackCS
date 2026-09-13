package com.example.crackcs.learning.mastery.service;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationConcept;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.mastery.domain.AppliedEvaluationConcept;
import com.example.crackcs.learning.mastery.domain.KnowledgeState;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultKnowledgeStateService implements KnowledgeStateService {
    private final EvaluationRepository evaluations;
    private final KnowledgeStateRepository states;
    private final AppliedEvaluationConceptRepository appliedConcepts;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyInCurrentTransaction(Long evaluationId) {
        Evaluation evaluation = evaluations.findLockedById(evaluationId).orElse(null);
        if (evaluation == null || !evaluation.isKnowledgeStateEligible()) {
            return;
        }
        // Completing an evaluation cascades new concept rows; their generated IDs are the delivery keys.
        evaluations.flush();
        evaluation.getConcepts().stream().sorted(Comparator.comparing(EvaluationConcept::getConceptId))
                .filter(concept -> concept.getVerdict() != Verdict.NEEDS_REVIEW)
                .forEach(concept -> applyOnce(evaluation, concept));
    }

    private void applyOnce(Evaluation evaluation, EvaluationConcept concept) {
        if (appliedConcepts.existsByEvaluationConceptId(concept.getId())) {
            return;
        }
        KnowledgeState state = findOrCreateState(evaluation, concept);
        state.observe(concept.getId(), concept.getVerdict(), evaluation.getEvaluatedAt());
        states.save(state);
        appliedConcepts.save(AppliedEvaluationConcept.builder().evaluationConcept(concept).build());
    }

    private KnowledgeState findOrCreateState(Evaluation evaluation, EvaluationConcept concept) {
        Long memberId = evaluation.getAnswer().getMember().getId();
        return states.findByMemberIdAndConceptId(memberId, concept.getConceptId())
                .orElseGet(() -> KnowledgeState.builder()
                        .member(evaluation.getAnswer().getMember()).concept(concept.getConcept()).build());
    }
}
