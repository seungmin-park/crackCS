package com.example.crackcs.content.question.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.content.question.controller.request.QuestionConceptReplaceRequest;
import com.example.crackcs.content.question.controller.request.QuestionCreateRequest;
import com.example.crackcs.content.question.controller.request.QuestionIdRequest;
import com.example.crackcs.content.question.controller.request.QuestionSearchRequest;
import com.example.crackcs.content.question.controller.request.QuestionUpdateRequest;
import com.example.crackcs.content.question.controller.request.QuestionVersionRequest;
import com.example.crackcs.content.question.controller.response.QuestionResponse;
import com.example.crackcs.content.question.controller.response.QuestionSummaryResponse;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/questions")
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public ResponseEntity<QuestionResponse> create(Authentication authentication,
                                                   @Valid @RequestBody QuestionCreateRequest request) {
        Question question = questionService.create(
                memberId(authentication),
                request.topicId(),
                request.difficultyValue(),
                request.content(),
                request.referenceAnswer()
        );

        return ResponseEntity
                .created(URI.create("/api/admin/questions/" + question.getId()))
                .body(QuestionResponse.from(question));
    }

    @GetMapping
    public PageResponse<QuestionSummaryResponse> findAll(@Valid @ModelAttribute QuestionSearchRequest request) {
        return PageResponse.from(
                questionService.findAll(
                        request.topicId(),
                        request.statusValue(),
                        request.difficultyValue(),
                        request.originValue(),
                        request.toPageable()
                ),
                QuestionSummaryResponse::from
        );
    }

    @GetMapping("/{questionId}")
    public QuestionResponse findById(@Valid @ModelAttribute QuestionIdRequest request) {
        return QuestionResponse.from(questionService.findById(request.questionId()));
    }

    @PatchMapping("/{questionId}")
    public QuestionResponse update(@Valid @ModelAttribute QuestionIdRequest questionIdRequest,
                                   @Valid @RequestBody QuestionUpdateRequest request) {
        return QuestionResponse.from(questionService.update(
                questionIdRequest.questionId(),
                request.topicId(),
                request.difficultyValue(),
                request.content(),
                request.referenceAnswer()
        ));
    }

    @PutMapping("/{questionId}/concepts")
    public QuestionResponse replaceConcepts(@Valid @ModelAttribute QuestionIdRequest questionIdRequest,
                                            @Valid @RequestBody QuestionConceptReplaceRequest request) {
        return QuestionResponse.from(questionService.replaceConcepts(
                questionIdRequest.questionId(), request.toData()
        ));
    }

    @PostMapping("/{questionId}/review")
    public QuestionResponse review(
            @Valid @ModelAttribute QuestionIdRequest questionIdRequest,
            Authentication authentication
    ) {
        return QuestionResponse.from(questionService.review(
                questionIdRequest.questionId(), memberId(authentication)
        ));
    }

    @PostMapping("/{questionId}/publish")
    public QuestionResponse publish(@Valid @ModelAttribute QuestionIdRequest request) {
        return QuestionResponse.from(questionService.publish(request.questionId()));
    }

    @PostMapping("/{questionId}/retire")
    public QuestionResponse retire(@Valid @ModelAttribute QuestionIdRequest request) {
        return QuestionResponse.from(questionService.retire(request.questionId()));
    }

    @PostMapping("/{questionId}/versions")
    public ResponseEntity<QuestionResponse> createNextVersion(
            @Valid @ModelAttribute QuestionIdRequest questionIdRequest,
            Authentication authentication,
            @Valid @RequestBody QuestionVersionRequest request
    ) {
        Question question = questionService.createNextVersion(
                questionIdRequest.questionId(),
                memberId(authentication),
                request.difficulty(),
                request.content(),
                request.referenceAnswer()
        );
        return ResponseEntity
                .created(URI.create("/api/admin/questions/" + question.getId()))
                .body(QuestionResponse.from(question));
    }

    private Long memberId(Authentication authentication) {
        return ((AuthenticatedMember) authentication.getPrincipal()).memberId();
    }
}
