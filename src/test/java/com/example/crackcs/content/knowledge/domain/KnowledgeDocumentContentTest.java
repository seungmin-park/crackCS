package com.example.crackcs.content.knowledge.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeDocumentContentTest {

    @Test
    @DisplayName("문서 원문의 줄바꿈과 앞뒤 공백을 정규화한 값으로 checksum을 계산한다")
    void normalizesContentAndCalculatesChecksum() {
        KnowledgeDocumentContent content = KnowledgeDocumentContent.from("  첫 줄\r\n둘째 줄  ");

        assertThat(content.value()).isEqualTo("첫 줄\n둘째 줄");
        assertThat(content.checksum())
                .isEqualTo("5fa3a5849b103ab0cf53249fd7152ff68e3f9b039da1038b0211f28de331eee0");
    }

    @Test
    @DisplayName("공백뿐인 문서 원문을 만들 수 없다")
    void rejectsBlankContent() {
        assertThatThrownBy(() -> KnowledgeDocumentContent.from(" \r\n\t "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");
    }
}
