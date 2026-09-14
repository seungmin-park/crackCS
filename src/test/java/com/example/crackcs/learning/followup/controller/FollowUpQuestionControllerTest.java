package com.example.crackcs.learning.followup.controller;

import com.example.crackcs.auth.config.SecurityConfiguration;
import com.example.crackcs.auth.security.ApiAccessDeniedHandler;
import com.example.crackcs.auth.security.ApiAuthenticationEntryPoint;
import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.auth.security.SecurityErrorResponseWriter;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.exception.AnswerNotFoundException;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult.QuestionResult;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult.TopicResult;
import com.example.crackcs.learning.followup.service.FollowUpQuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowUpQuestionController.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class})
class FollowUpQuestionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FollowUpQuestionService followUpQuestionService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private PasswordEncoder encoder;

    @Test
    @DisplayName("본인 후속 질문은 공개 질문 형태로 반환하고 모범 답안을 숨긴다")
    void returnsReadyWithoutReferenceAnswer() throws Exception {
        given(followUpQuestionService.findByAnswerId(41L, 7L)).willReturn(new FollowUpQuestionResult(FollowUpStatus.READY, null,
                new QuestionResult(8L, new TopicResult(1L, "OS", "운영체제"), QuestionDifficulty.BASIC, "후속 질문")));
        mockMvc.perform(get("/api/answers/7/follow-up-question").with(user(principal())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.question.id").value(8))
                .andExpect(jsonPath("$.question.topic.code").value("OS"))
                .andExpect(jsonPath("$.question.referenceAnswer").doesNotExist());
    }

    @Test
    @DisplayName("준비되지 않은 상태는 질문 없이 반환한다")
    void returnsUnavailableReason() throws Exception {
        given(followUpQuestionService.findByAnswerId(41L, 7L)).willReturn(new FollowUpQuestionResult(
                FollowUpStatus.UNAVAILABLE, FollowUpReason.FOLLOW_UP_LIMIT, null));
        mockMvc.perform(get("/api/answers/7/follow-up-question").with(user(principal())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("FOLLOW_UP_LIMIT"))
                .andExpect(jsonPath("$.question").doesNotExist());
    }

    @Test
    @DisplayName("인증하지 않은 조회는 401이다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/answers/7/follow-up-question")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 권한만으로는 개인 후속 질문에 접근하지 못한다")
    void rejectsAdmin() throws Exception {
        mockMvc.perform(get("/api/answers/7/follow-up-question").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("없는 답변이나 타인의 답변은 404이다")
    void hidesOtherAnswers() throws Exception {
        given(followUpQuestionService.findByAnswerId(41L, 7L)).willThrow(new AnswerNotFoundException(7L));
        mockMvc.perform(get("/api/answers/7/follow-up-question").with(user(principal()))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("답변 식별자는 양수여야 한다")
    void validatesPositiveId() throws Exception {
        mockMvc.perform(get("/api/answers/0/follow-up-question").with(user(principal()))).andExpect(status().isBadRequest());
    }

    private AuthenticatedMember principal() {
        AuthenticatedMember member = mock(AuthenticatedMember.class);
        given(member.memberId()).willReturn(41L);
        given(member.getUsername()).willReturn("learner");
        given(member.getPassword()).willReturn("unused");
        given(member.isEnabled()).willReturn(true);
        given(member.getAuthorities()).willAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return member;
    }
}
