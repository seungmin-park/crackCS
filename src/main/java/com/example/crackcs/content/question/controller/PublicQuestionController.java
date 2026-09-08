package com.example.crackcs.content.question.controller;

import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.content.question.controller.request.PublicQuestionSearchRequest;
import com.example.crackcs.content.question.controller.request.QuestionIdRequest;
import com.example.crackcs.content.question.controller.response.PublicQuestionResponse;
import com.example.crackcs.content.question.service.PublicQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/questions")
public class PublicQuestionController {

    private final PublicQuestionService publicQuestionService;

    @GetMapping
    public PageResponse<PublicQuestionResponse> findAll(@Valid @ModelAttribute PublicQuestionSearchRequest request) {
        return PageResponse.from(
                publicQuestionService.findAll(
                        request.topicId(),
                        request.difficultyValue(),
                        request.toPageable()
                ),
                PublicQuestionResponse::from
        );
    }

    @GetMapping("/{questionId}")
    public PublicQuestionResponse findById(@Valid @ModelAttribute QuestionIdRequest request) {
        return PublicQuestionResponse.from(publicQuestionService.findById(request.questionId()));
    }
}
