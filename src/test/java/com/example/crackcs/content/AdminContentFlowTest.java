package com.example.crackcs.content;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.repository.AuthAccountRepository;
import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminContentFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired KnowledgeDocumentRepository documentRepository;
    @Autowired QuestionRepository questionRepository;

    @Test
    @DisplayName("ADMIN이 Topic과 문서를 만든 뒤 검수하고 공개한다")
    void adminPublishesDocument() throws Exception {
        AuthenticatedMember admin = savePrincipal(MemberRole.ADMIN, "admin-document-flow");
        long topicId = createTopic(admin, "NETWORK", "네트워크");
        long documentId = createDocument(admin, topicId, "TCP 공식 문서", "공개 원문");

        mockMvc.perform(post("/api/admin/knowledge-documents/{id}/publish", documentId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_CONTENT_STATE"));
        mockMvc.perform(post("/api/admin/knowledge-documents/{id}/review", documentId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());
        mockMvc.perform(post("/api/admin/knowledge-documents/{id}/publish", documentId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("ADMIN이 Topic과 Concept과 문제를 만든 뒤 검수하고 공개한다")
    void adminPublishesQuestion() throws Exception {
        AuthenticatedMember admin = savePrincipal(MemberRole.ADMIN, "admin-question-flow");
        long topicId = createTopic(admin, "QUESTION_NETWORK", "문제용 네트워크");
        long conceptId = createConcept(admin, topicId, "TCP_RELIABILITY", "TCP 신뢰성");

        long questionId = createQuestion(admin, topicId);
        mockMvc.perform(put("/api/admin/questions/{id}/concepts", questionId)
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "concepts", List.of(Map.of(
                                        "conceptId", conceptId,
                                        "weight", 1.00,
                                        "required", true
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concepts[0].conceptId").value(conceptId));
        mockMvc.perform(post("/api/admin/questions/{id}/publish", questionId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/questions/{id}/review", questionId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/questions/{id}/publish", questionId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/questions/{id}", questionId).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("TCP가 신뢰성을 보장하는 방법은 무엇인가요?"))
                .andExpect(jsonPath("$.referenceAnswer").doesNotExist());
    }

    @Test
    @DisplayName("PUBLISHED 문서의 새 버전 API는 기존 버전을 보존하고 버전 번호를 증가시킨다")
    void preservesDocumentVersionThroughApi() throws Exception {
        AuthenticatedMember admin = savePrincipal(MemberRole.ADMIN, "admin-version");
        long topicId = createTopic(admin, "JAVA", "Java");
        long firstId = createDocument(admin, topicId, "Java 문서", "첫 버전");
        mockMvc.perform(post("/api/admin/knowledge-documents/{id}/review", firstId)
                        .with(user(admin)).with(csrf())).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/knowledge-documents/{id}/publish", firstId)
                        .with(user(admin)).with(csrf())).andExpect(status().isOk());

        Map<String, Object> nextVersion = documentRequest(topicId, "Java 문서", "둘째 버전");
        MvcResult result = mockMvc.perform(post("/api/admin/knowledge-documents/{id}/versions", firstId)
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nextVersion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentVersion").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        long secondId = body(result).get("id").asLong();

        mockMvc.perform(post("/api/admin/knowledge-documents/{id}/review", secondId)
                        .with(user(admin)).with(csrf())).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/knowledge-documents/{id}/publish", secondId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(documentRepository.findById(firstId).orElseThrow().getContent()).isEqualTo("첫 버전");
        assertThat(documentRepository.findById(firstId).orElseThrow().getStatus().name()).isEqualTo("RETIRED");
        assertThat(documentRepository.findById(secondId).orElseThrow().getContent()).isEqualTo("둘째 버전");
        assertThat(documentRepository.findPublishedCandidatesByTopicId(topicId))
                .extracting(document -> document.getId())
                .containsExactly(secondId);
    }

    @Test
    @DisplayName("PUBLISHED 문제의 새 버전 API는 평가 기준을 복사하고 이전 공개본을 보존한다")
    void preservesQuestionVersionThroughApi() throws Exception {
        AuthenticatedMember admin = savePrincipal(MemberRole.ADMIN, "admin-question-version");
        long topicId = createTopic(admin, "NETWORK_VERSION", "네트워크 버전");
        long conceptId = createConcept(admin, topicId, "TCP_VERSION", "TCP 버전");
        long firstId = createQuestion(admin, topicId);
        mockMvc.perform(put("/api/admin/questions/{id}/concepts", firstId)
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "concepts", List.of(Map.of(
                                        "conceptId", conceptId,
                                        "weight", 1.00,
                                        "required", true
                                ))
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/questions/{id}/review", firstId)
                        .with(user(admin)).with(csrf())).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/questions/{id}/publish", firstId)
                        .with(user(admin)).with(csrf())).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/admin/questions/{id}/versions", firstId)
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "difficulty", "INTERMEDIATE",
                                "content", "TCP 신뢰성의 다음 버전 질문",
                                "referenceAnswer", "순서 번호, 확인 응답과 재전송을 설명합니다."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionVersion").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.concepts[0].conceptId").value(conceptId))
                .andReturn();
        long secondId = body(result).get("id").asLong();

        mockMvc.perform(post("/api/admin/questions/{id}/review", secondId)
                        .with(user(admin)).with(csrf())).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/questions/{id}/publish", secondId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(questionRepository.findById(firstId).orElseThrow().getStatus().name())
                .isEqualTo("RETIRED");
        assertThat(questionRepository.findById(secondId).orElseThrow().getContent())
                .isEqualTo("TCP 신뢰성의 다음 버전 질문");
        mockMvc.perform(get("/api/questions/{id}", firstId).with(user(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/questions/{id}", secondId).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("TCP 신뢰성의 다음 버전 질문"));
    }

    @Test
    @DisplayName("USER는 관리자 DRAFT 상세과 상태 변경 API에 접근할 수 없다")
    void userCannotAccessAdminDraft() throws Exception {
        AuthenticatedMember admin = savePrincipal(MemberRole.ADMIN, "admin-boundary");
        AuthenticatedMember user = savePrincipal(MemberRole.USER, "user-boundary");
        long topicId = createTopic(admin, "DATABASE", "데이터베이스");
        long questionId = createQuestion(admin, topicId);

        mockMvc.perform(get("/api/admin/questions/{id}", questionId).with(user(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(post("/api/admin/topics/{id}/deactivate", topicId)
                        .with(user(user)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN은 회원 목록을 조회하고 회원 상태를 변경한다")
    void adminChangesMemberStatus() throws Exception {
        AuthenticatedMember admin = savePrincipal(MemberRole.ADMIN, "admin-member");
        AuthenticatedMember user = savePrincipal(MemberRole.USER, "target-member");

        mockMvc.perform(get("/api/admin/members").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nickname")
                        .value(hasItems("admin-member", "target-member")));
        mockMvc.perform(patch("/api/admin/members/{id}/status", user.memberId())
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "BLOCKED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    private long createTopic(AuthenticatedMember admin, String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/topics")
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code, "name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private long createConcept(AuthenticatedMember admin, long topicId, String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/concepts")
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "topicId", topicId, "code", code, "name", name, "description", "설명"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private long createDocument(AuthenticatedMember admin, long topicId, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/knowledge-documents")
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(documentRequest(topicId, title, content))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private Map<String, Object> documentRequest(long topicId, String title, String content) {
        return Map.of(
                "topicId", topicId,
                "title", title,
                "sourceType", "OFFICIAL_DOC",
                "sourceUrl", "https://example.com/docs",
                "technologyVersion", "Java 21",
                "licenseNote", "인용 가능",
                "content", content
        );
    }

    private long createQuestion(AuthenticatedMember admin, long topicId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/questions")
                        .with(user(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "topicId", topicId,
                                "difficulty", "BASIC",
                                "content", "TCP가 신뢰성을 보장하는 방법은 무엇인가요?",
                                "referenceAnswer", "순서 번호와 확인 응답 및 재전송을 사용합니다."
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private AuthenticatedMember savePrincipal(MemberRole role, String loginPrefix) {
        Member member = memberRepository.save(Member.builder().nickname(loginPrefix).role(role).build());
        AuthAccount account = authAccountRepository.save(AuthAccount.builder()
                .member(member)
                .loginId(loginPrefix + "@example.com")
                .passwordHash("{noop}password")
                .build());
        return AuthenticatedMember.from(account);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
