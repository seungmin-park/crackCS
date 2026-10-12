package com.example.crackcs.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
@Profile({"postgres", "postgres-test"})
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SharedSessionConfiguration {
    @Bean
    CookieSerializer sessionCookieSerializer(
            @Value("${server.servlet.session.cookie.secure:true}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("CRACKCS_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseSecureCookie(secure);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }
}
