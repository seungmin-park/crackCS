package com.example.crackcs.learning.mastery.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.crackcs.auth.config.SecurityConfiguration;
import com.example.crackcs.auth.security.ApiAccessDeniedHandler;
import com.example.crackcs.auth.security.ApiAuthenticationEntryPoint;
import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.auth.security.SecurityErrorResponseWriter;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.learning.mastery.domain.KnowledgeStatus;
import com.example.crackcs.learning.mastery.service.KnowledgeQueryService;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult.ConceptState;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult.TopicState;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult;
import com.example.crackcs.learning.progress.controller.LearningProgressController;
import com.example.crackcs.learning.progress.service.LearningProgressService;
import com.example.crackcs.learning.progress.service.result.LearningProgressResult.RecentEvaluation;
import com.example.crackcs.learning.progress.service.result.LearningProgressResult;
import com.example.crackcs.learning.recommendation.controller.RecommendationController;
import com.example.crackcs.learning.recommendation.service.RecommendationService;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult.Reason;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({KnowledgeController.class, RecommendationController.class, LearningProgressController.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class})
class KnowledgeControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    KnowledgeQueryService service;
    @MockitoBean
    RecommendationService recommendations;
    @MockitoBean
    LearningProgressService progress;
    @MockitoBean
    UserDetailsService userDetailsService;
    @MockitoBean
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("지식 지도는 요청의 회원 ID를 무시하고 인증 회원의 미평가와 0점을 구분해 직렬화한다")
    void returnsKnowledgeForPrincipalOnly() throws Exception {
        TopicState topic = new TopicState(1L, "운영체제", KnowledgeStatus.LEARNING, 0.0, 12.5, 1, 1, 0,
                List.of(new ConceptState(2L, "스레드", KnowledgeStatus.UNKNOWN, null, 0, 0, null),
                        new ConceptState(3L, "프로세스", KnowledgeStatus.LEARNING, 0.0, 25, 1, null)));
        given(service.knowledgeStates(41L)).willReturn(new KnowledgeStatesResult(List.of(topic)));
        mvc.perform(get("/api/members/me/knowledge-states").param("memberId", "99").with(user(principal("USER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.topics[0].topicId").value(1))
                .andExpect(jsonPath("$.topics[0].confidenceScore").value(12.5))
                .andExpect(jsonPath("$.topics[0].concepts[0].masteryScore").isEmpty())
                .andExpect(jsonPath("$.topics[0].concepts[1].masteryScore").value(0));
        verify(service).knowledgeStates(41L);
    }

    @Test
    @DisplayName("추천 문제 응답은 인증 회원의 추천과 한국어 이유를 반환한다")
    void returnsRecommendationForPrincipalOnly() throws Exception {
        given(recommendations.recommendation(41L)).willReturn(recommendation());
        mvc.perform(get("/api/recommendations/next-question").param("memberId", "99").with(user(principal("USER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionId").value(5))
                .andExpect(jsonPath("$.title").value("문제"))
                .andExpect(jsonPath("$.conceptName").value("스레드"))
                .andExpect(jsonPath("$.reason").value("UNASSESSED_CONCEPT"))
                .andExpect(jsonPath("$.reasonText").value("미평가 개념을 확인하세요."));
        verify(recommendations).recommendation(41L);
    }

    @Test
    @DisplayName("학습 홈 응답은 인증 회원의 풀이 수와 상태 및 추천을 반환한다")
    void returnsProgressForPrincipalOnly() throws Exception {
        RecentEvaluation recent = new RecentEvaluation(7L, "스레드 문제", EvaluationStatus.EVALUATED,
                Verdict.CORRECT, 100, LocalDateTime.of(2026, 9, 13, 12, 0));
        TopicState topic = new TopicState(1L, "운영체제", KnowledgeStatus.LEARNING, 50.0, 25.0, 0, 1, 0,
                List.of(new ConceptState(2L, "스레드", KnowledgeStatus.LEARNING, 50.0, 25, 1, null)));
        given(progress.progress(41L)).willReturn(
                new LearningProgressResult(8, 3, List.of(recent), List.of(topic), recommendation()));
        mvc.perform(get("/api/members/me/progress").param("memberId", "99").with(user(principal("USER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAnswers").value(8))
                .andExpect(jsonPath("$.recentAnswerCount").value(3))
                .andExpect(jsonPath("$.recentEvaluations[0].answerId").value(7))
                .andExpect(jsonPath("$.recentEvaluations[0].questionTitle").value("스레드 문제"))
                .andExpect(jsonPath("$.recentEvaluations[0].status").value("EVALUATED"))
                .andExpect(jsonPath("$.recentEvaluations[0].verdict").value("CORRECT"))
                .andExpect(jsonPath("$.recentEvaluations[0].score").value(100))
                .andExpect(jsonPath("$.recentEvaluations[0].submittedAt").value("2026-09-13T12:00:00"))
                .andExpect(jsonPath("$.topics[0].topicId").value(1))
                .andExpect(jsonPath("$.topics[0].concepts[0].conceptId").value(2))
                .andExpect(jsonPath("$.recommendation.questionId").value(5));
        verify(progress).progress(41L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/members/me/knowledge-states", "/api/members/me/progress",
            "/api/recommendations/next-question"})
    @DisplayName("학습 API는 비로그인 요청을 401로 거부한다")
    void rejectsAnonymous(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, recommendations, progress);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/members/me/knowledge-states", "/api/members/me/progress",
            "/api/recommendations/next-question"})
    @DisplayName("학습 API는 관리자 요청을 403으로 거부한다")
    void rejectsAdmin(String path) throws Exception {
        mvc.perform(get(path).with(user(principal("ADMIN")))).andExpect(status().isForbidden());
        verifyNoInteractions(service, recommendations, progress);
    }

    private RecommendationResult recommendation() {
        return new RecommendationResult(5L, "문제", 2L, "스레드", Reason.UNASSESSED_CONCEPT, "미평가 개념을 확인하세요.");
    }

    private AuthenticatedMember principal(String role) {
        AuthenticatedMember principal = mock(AuthenticatedMember.class);
        given(principal.memberId()).willReturn(41L);
        given(principal.getUsername()).willReturn("member@example.com");
        given(principal.getPassword()).willReturn("encoded");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_" + role))).when(principal).getAuthorities();
        given(principal.isEnabled()).willReturn(true);
        return principal;
    }
}
