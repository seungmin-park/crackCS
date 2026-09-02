package com.example.crackcs.content.knowledge.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.content.knowledge.controller.request.KnowledgeDocumentRequest;
import com.example.crackcs.content.knowledge.controller.request.KnowledgeDocumentSearchRequest;
import com.example.crackcs.content.knowledge.controller.response.KnowledgeDocumentResponse;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.service.KnowledgeDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/admin/knowledge-documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @GetMapping
    public PageResponse<KnowledgeDocumentResponse> findAll(
            @Valid @ModelAttribute KnowledgeDocumentSearchRequest request
    ) {
        return PageResponse.from(
                knowledgeDocumentService.findAll(
                        request.topicId(),
                        request.statusValue(),
                        request.technologyVersion(),
                        request.toPageable()
                ),
                KnowledgeDocumentResponse::from
        );
    }

    @PostMapping
    public ResponseEntity<KnowledgeDocumentResponse> create(
            Authentication authentication,
            @Valid @RequestBody KnowledgeDocumentRequest request
    ) {
        KnowledgeDocument document = knowledgeDocumentService.create(memberId(authentication), request.toData());
        return ResponseEntity.created(URI.create("/api/admin/knowledge-documents/" + document.getId()))
                .body(KnowledgeDocumentResponse.from(document));
    }

    @GetMapping("/{documentId}")
    public KnowledgeDocumentResponse findById(
            @Positive(message = "documentId는 양수여야 합니다.") @PathVariable Long documentId
    ) {
        return KnowledgeDocumentResponse.from(knowledgeDocumentService.findById(documentId));
    }

    @PatchMapping("/{documentId}")
    public KnowledgeDocumentResponse update(
            @Positive(message = "documentId는 양수여야 합니다.") @PathVariable Long documentId,
            @Valid @RequestBody KnowledgeDocumentRequest request
    ) {
        return KnowledgeDocumentResponse.from(knowledgeDocumentService.update(documentId, request.toData()));
    }

    @PostMapping("/{documentId}/versions")
    public ResponseEntity<KnowledgeDocumentResponse> createNextVersion(
            @Positive(message = "documentId는 양수여야 합니다.") @PathVariable Long documentId,
            Authentication authentication,
            @Valid @RequestBody KnowledgeDocumentRequest request
    ) {
        KnowledgeDocument document = knowledgeDocumentService.createNextVersion(
                documentId, memberId(authentication), request.toData()
        );
        return ResponseEntity.created(URI.create("/api/admin/knowledge-documents/" + document.getId()))
                .body(KnowledgeDocumentResponse.from(document));
    }

    @PostMapping("/{documentId}/review")
    public KnowledgeDocumentResponse review(
            @Positive(message = "documentId는 양수여야 합니다.") @PathVariable Long documentId,
            Authentication authentication
    ) {
        return KnowledgeDocumentResponse.from(
                knowledgeDocumentService.review(documentId, memberId(authentication))
        );
    }

    @PostMapping("/{documentId}/publish")
    public KnowledgeDocumentResponse publish(
            @Positive(message = "documentId는 양수여야 합니다.") @PathVariable Long documentId
    ) {
        return KnowledgeDocumentResponse.from(knowledgeDocumentService.publish(documentId));
    }

    @PostMapping("/{documentId}/retire")
    public KnowledgeDocumentResponse retire(
            @Positive(message = "documentId는 양수여야 합니다.") @PathVariable Long documentId
    ) {
        return KnowledgeDocumentResponse.from(knowledgeDocumentService.retire(documentId));
    }

    private Long memberId(Authentication authentication) {
        return ((AuthenticatedMember) authentication.getPrincipal()).memberId();
    }
}
