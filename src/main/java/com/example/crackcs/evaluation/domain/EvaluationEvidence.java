package com.example.crackcs.evaluation.domain;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "evaluation_evidence", uniqueConstraints = @UniqueConstraint(
        name = "uk_evaluation_evidence_evaluation_chunk", columnNames = {"evaluation_id", "chunk_id"}
))
public class EvaluationEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private KnowledgeChunk chunk;

    static EvaluationEvidence from(Evaluation evaluation, KnowledgeChunk chunk) {
        EvaluationEvidence evidence = new EvaluationEvidence();
        evidence.evaluation = evaluation;
        evidence.chunk = chunk;
        return evidence;
    }

    public Long getChunkId() {
        return chunk.getId();
    }

    public String getDocumentTitle() {
        return chunk.getDocument().getTitle();
    }

    public int getDocumentVersion() {
        return chunk.getDocument().getDocumentVersion();
    }

    public int getStartOffset() {
        return chunk.getStartOffset();
    }

    public int getEndOffset() {
        return chunk.getEndOffset();
    }

    public String getContent() {
        return chunk.getContent();
    }
}
