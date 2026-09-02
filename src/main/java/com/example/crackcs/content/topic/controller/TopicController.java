package com.example.crackcs.content.topic.controller;

import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.content.topic.controller.request.TopicCreateRequest;
import com.example.crackcs.content.topic.controller.request.TopicSearchRequest;
import com.example.crackcs.content.topic.controller.request.TopicUpdateRequest;
import com.example.crackcs.content.topic.controller.response.TopicResponse;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.service.TopicService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/topics")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public PageResponse<TopicResponse> findAll(@Valid @ModelAttribute TopicSearchRequest request) {
        return PageResponse.from(
                topicService.findAll(request.parentId(), request.active(), request.toPageable()),
                TopicResponse::from
        );
    }

    @PostMapping
    public ResponseEntity<TopicResponse> create(@Valid @RequestBody TopicCreateRequest request) {
        Topic topic = topicService.create(request.parentId(), request.code(), request.name());
        return ResponseEntity.created(URI.create("/api/admin/topics/" + topic.getId()))
                .body(TopicResponse.from(topic));
    }

    @GetMapping("/{topicId}")
    public TopicResponse findById(
            @Positive(message = "topicId는 양수여야 합니다.") @PathVariable Long topicId
    ) {
        return TopicResponse.from(topicService.findById(topicId));
    }

    @PatchMapping("/{topicId}")
    public TopicResponse update(
            @Positive(message = "topicId는 양수여야 합니다.") @PathVariable Long topicId,
            @Valid @RequestBody TopicUpdateRequest request
    ) {
        return TopicResponse.from(topicService.update(
                topicId,
                request.parentId(),
                request.code(),
                request.name()
        ));
    }

    @PostMapping("/{topicId}/deactivate")
    public ResponseEntity<Void> deactivate(
            @Positive(message = "topicId는 양수여야 합니다.") @PathVariable Long topicId
    ) {
        topicService.deactivate(topicId);
        return ResponseEntity.noContent().build();
    }
}
