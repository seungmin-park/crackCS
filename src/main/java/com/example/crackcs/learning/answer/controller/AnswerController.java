package com.example.crackcs.learning.answer.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.learning.answer.controller.request.AnswerIdRequest;
import com.example.crackcs.learning.answer.controller.request.AnswerQuestionIdRequest;
import com.example.crackcs.learning.answer.controller.request.AnswerSearchRequest;
import com.example.crackcs.learning.answer.controller.request.AnswerSubmitRequest;
import com.example.crackcs.learning.answer.controller.response.AnswerResponse;
import com.example.crackcs.learning.answer.controller.response.EvaluationResponse;
import com.example.crackcs.learning.answer.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AnswerController {
    private final AnswerService answerService;

    @PostMapping("/api/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnswerResponse submit(@AuthenticationPrincipal AuthenticatedMember member,
                                 @Valid @ModelAttribute AnswerQuestionIdRequest path,
                                 @Valid @RequestBody AnswerSubmitRequest request,
                                 @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return AnswerResponse.from(
                answerService.submit(member.memberId(), path.questionId(), request.validatedRequestId(idempotencyKey),
                        request.content()));
    }

    @GetMapping("/api/members/me/answers")
    public PageResponse<AnswerResponse> findAll(@AuthenticationPrincipal AuthenticatedMember member,
                                                @Valid @ModelAttribute AnswerSearchRequest request) {
        return PageResponse.from(answerService.findAll(member.memberId(), request.toPageable()), AnswerResponse::from);
    }

    @GetMapping("/api/answers/{answerId}")
    public AnswerResponse findById(@AuthenticationPrincipal AuthenticatedMember member,
                                   @Valid @ModelAttribute AnswerIdRequest path) {
        return AnswerResponse.from(answerService.findById(member.memberId(), path.answerId()));
    }

    @GetMapping("/api/answers/{answerId}/evaluation")
    public EvaluationResponse findEvaluation(@AuthenticationPrincipal AuthenticatedMember member,
                                             @Valid @ModelAttribute AnswerIdRequest path) {
        return EvaluationResponse.from(answerService.findEvaluation(member.memberId(), path.answerId()));
    }
}
