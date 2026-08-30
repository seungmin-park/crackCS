package com.example.crackcs.content.question.controller;

import com.example.crackcs.content.question.controller.request.QuestionCreateRequest;
import com.example.crackcs.content.question.controller.request.QuestionIdRequest;
import com.example.crackcs.content.question.controller.request.QuestionSearchRequest;
import com.example.crackcs.content.question.controller.request.QuestionUpdateRequest;
import com.example.crackcs.content.question.controller.response.QuestionPageResponse;
import com.example.crackcs.content.question.controller.response.QuestionResponse;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/questions")
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public ResponseEntity<QuestionResponse> create(@Valid @RequestBody QuestionCreateRequest request) {
        Question question = questionService.create(
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
    public QuestionPageResponse findAll(@Valid @ModelAttribute QuestionSearchRequest request) {
        return QuestionPageResponse.from(
                questionService.findAll(
                        request.topicId(),
                        request.statusValue(),
                        request.difficultyValue(),
                        request.originValue(),
                        request.toPageable()
                )
        );
    }

    @GetMapping("/{questionId}")
    public QuestionResponse findById(@Valid @ModelAttribute QuestionIdRequest request) {
        return QuestionResponse.from(questionService.findById(request.questionId()));
    }

    @PatchMapping("/{questionId}")
    public QuestionResponse update(
            @Valid @ModelAttribute QuestionIdRequest questionIdRequest,
            @Valid @RequestBody QuestionUpdateRequest request
    ) {
        return QuestionResponse.from(questionService.update(
                questionIdRequest.questionId(),
                request.topicId(),
                request.difficultyValue(),
                request.content(),
                request.referenceAnswer()
        ));
    }
}
