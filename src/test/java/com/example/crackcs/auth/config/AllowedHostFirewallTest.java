package com.example.crackcs.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowedHostFirewallTest {

    @Test
    @DisplayName("허용 호스트 목록이 있으면 목록 밖의 Host 요청을 거부한다")
    void rejectsHostOutsideAllowedList() {
        HttpFirewall firewall = new SecurityConfiguration().httpFirewall(List.of("localhost", "127.0.0.1"));
        MockHttpServletRequest rebindRequest = new MockHttpServletRequest("GET", "/api/auth/csrf");
        rebindRequest.setServerName("rebind.example");

        assertThatThrownBy(() -> firewall.getFirewalledRequest(rebindRequest))
                .isInstanceOf(RequestRejectedException.class);
    }

    @Test
    @DisplayName("허용 호스트 목록에 있는 Host 요청은 통과시킨다")
    void acceptsAllowedHost() {
        HttpFirewall firewall = new SecurityConfiguration().httpFirewall(List.of("localhost", "127.0.0.1"));
        MockHttpServletRequest loopbackRequest = new MockHttpServletRequest("GET", "/api/auth/csrf");
        loopbackRequest.setServerName("127.0.0.1");

        assertThatCode(() -> firewall.getFirewalledRequest(loopbackRequest)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("허용 호스트 목록이 비어 있으면 Host로 요청을 거부하지 않는다")
    void acceptsAnyHostWithoutAllowedList() {
        HttpFirewall firewall = new SecurityConfiguration().httpFirewall(List.of());
        MockHttpServletRequest publicHostRequest = new MockHttpServletRequest("GET", "/api/auth/csrf");
        publicHostRequest.setServerName("crackcs.example");

        assertThatCode(() -> firewall.getFirewalledRequest(publicHostRequest)).doesNotThrowAnyException();
    }
}
