package com.example.crackcs.learning.followup.adapter;

import com.example.crackcs.learning.followup.domain.FollowUpResult;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("(local | test) & !prod & !production")
@ConditionalOnProperty(name = "crackcs.followup.openai.enabled", havingValue = "false", matchIfMissing = true)
public class StubFollowUpQuestionAdapter implements FollowUpQuestionGenerator {

    public FollowUpResult generate(FollowUpRequest request) {
        return new FollowUpResult(request.conceptName() + " 개념을 실제 상황에 어떻게 적용할 수 있나요?",
                request.evidence().getFirst().content(), request.conceptId(),
                request.evidence().stream().map(FollowUpRequest.Evidence::chunkId).toList(),
                "stub", "follow-up-v1", 0, 0, 0);
    }
}
