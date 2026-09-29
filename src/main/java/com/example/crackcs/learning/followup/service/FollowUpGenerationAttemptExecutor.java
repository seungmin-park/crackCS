package com.example.crackcs.learning.followup.service;

import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.exception.ProviderRequestRejectedException;
import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FollowUpGenerationAttemptExecutor {

    private final ObjectProvider<FollowUpQuestionGenerator> followUpQuestionGeneratorProvider;

    FollowUpGenerationAttemptOutcome execute(FollowUpRequest request) {
        try {
            FollowUpQuestionGenerator generator = followUpQuestionGeneratorProvider.getIfAvailable();
            if (generator == null) {
                return retry(FollowUpReason.PROVIDER_ERROR);
            }
            FollowUpGenerationResult generationResult = generator.generate(request);
            if (generationResult == null) {
                throw new IllegalArgumentException("follow-up result is required");
            }
            generationResult.validateAgainst(
                    request.conceptId(),
                    request.evidence().stream().map(FollowUpRequest.Evidence::chunkId).toList()
            );
            return new FollowUpGenerationAttemptOutcome.Completed(generationResult);
        } catch (ProviderRequestRejectedException rejected) {
            return new FollowUpGenerationAttemptOutcome.FailureRequired(FollowUpReason.PROVIDER_ERROR);
        } catch (EvaluationTimeoutException timeout) {
            return retry(FollowUpReason.PROVIDER_TIMEOUT);
        } catch (IllegalArgumentException invalidResult) {
            return new FollowUpGenerationAttemptOutcome.FailureRequired(FollowUpReason.INVALID_RESULT);
        } catch (RuntimeException providerFailure) {
            return retry(FollowUpReason.PROVIDER_ERROR);
        }
    }

    private FollowUpGenerationAttemptOutcome retry(FollowUpReason reason) {
        return new FollowUpGenerationAttemptOutcome.RetryRequired(reason);
    }
}
