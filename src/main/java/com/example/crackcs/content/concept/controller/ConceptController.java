package com.example.crackcs.content.concept.controller;

import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.content.concept.controller.request.ConceptCreateRequest;
import com.example.crackcs.content.concept.controller.request.ConceptSearchRequest;
import com.example.crackcs.content.concept.controller.request.ConceptUpdateRequest;
import com.example.crackcs.content.concept.controller.response.ConceptResponse;
import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.service.ConceptService;
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
@RequestMapping("/api/admin/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    @GetMapping
    public PageResponse<ConceptResponse> findAll(@Valid @ModelAttribute ConceptSearchRequest request) {
        return PageResponse.from(
                conceptService.findAll(request.topicId(), request.active(), request.toPageable()),
                ConceptResponse::from
        );
    }

    @PostMapping
    public ResponseEntity<ConceptResponse> create(@Valid @RequestBody ConceptCreateRequest request) {
        Concept concept = conceptService.create(
                request.topicId(), request.code(), request.name(), request.description()
        );
        return ResponseEntity.created(URI.create("/api/admin/concepts/" + concept.getId()))
                .body(ConceptResponse.from(concept));
    }

    @GetMapping("/{conceptId}")
    public ConceptResponse findById(
            @Positive(message = "conceptId는 양수여야 합니다.") @PathVariable Long conceptId
    ) {
        return ConceptResponse.from(conceptService.findById(conceptId));
    }

    @PatchMapping("/{conceptId}")
    public ConceptResponse update(
            @Positive(message = "conceptId는 양수여야 합니다.") @PathVariable Long conceptId,
            @Valid @RequestBody ConceptUpdateRequest request
    ) {
        return ConceptResponse.from(conceptService.update(
                conceptId,
                request.topicId(),
                request.code(),
                request.name(),
                request.description()
        ));
    }

    @PostMapping("/{conceptId}/deactivate")
    public ResponseEntity<Void> deactivate(
            @Positive(message = "conceptId는 양수여야 합니다.") @PathVariable Long conceptId
    ) {
        conceptService.deactivate(conceptId);
        return ResponseEntity.noContent().build();
    }
}
