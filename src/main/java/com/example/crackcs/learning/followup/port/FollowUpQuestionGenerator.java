package com.example.crackcs.learning.followup.port;

import com.example.crackcs.learning.followup.domain.FollowUpResult;

/**
 * 후속 질문 생성 계약. Processor는 구현체 대신 이 계약에 의존한다.
 * 구현체: OpenAiFollowUpQuestionAdapter(실제 호출), StubFollowUpQuestionAdapter(로컬·테스트).
 */
public interface FollowUpQuestionGenerator {
    FollowUpResult generate(FollowUpRequest request);
}
