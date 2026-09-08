package com.example.crackcs.learning.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.content.question.controller.request.QuestionIdRequest;
import com.example.crackcs.learning.controller.request.AnswerIdRequest;
import com.example.crackcs.learning.controller.request.AnswerSearchRequest;
import com.example.crackcs.learning.controller.request.AnswerSubmitRequest;
import com.example.crackcs.learning.controller.response.AnswerResponse;
import com.example.crackcs.learning.controller.response.EvaluationResponse;
import com.example.crackcs.learning.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnswerController {
    private final AnswerService answers;

    @PostMapping("/api/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnswerResponse submit(@AuthenticationPrincipal AuthenticatedMember member,
                                 @Valid @ModelAttribute QuestionIdRequest path,
                                 @Valid @RequestBody AnswerSubmitRequest request,
                                 @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return answers.submit(member.memberId(), path.questionId(), request.validatedRequestId(idempotencyKey),
                request.content());
    }

    @GetMapping("/api/members/me/answers")
    public PageResponse<AnswerResponse> findAll(@AuthenticationPrincipal AuthenticatedMember member,
                                                @Valid @ModelAttribute AnswerSearchRequest request) {
        return PageResponse.from(answers.findAll(member.memberId(), request.toPageable()), answer -> answer);
    }

    @GetMapping("/api/answers/{answerId}")
    public AnswerResponse findById(@AuthenticationPrincipal AuthenticatedMember member,
                                   @Valid @ModelAttribute AnswerIdRequest path) {
        return answers.findById(member.memberId(), path.answerId());
    }

    @GetMapping("/api/answers/{answerId}/evaluation")
    public EvaluationResponse findEvaluation(@AuthenticationPrincipal AuthenticatedMember member,
                                             @Valid @ModelAttribute AnswerIdRequest path) {
        return answers.findEvaluation(member.memberId(), path.answerId());
    }
}
