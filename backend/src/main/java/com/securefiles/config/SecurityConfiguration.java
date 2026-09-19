package com.securefiles.config;

import com.securefiles.infrastructure.security.JwtAuthenticationFilter;
import com.securefiles.infrastructure.security.LoginRateLimitFilter;
import com.securefiles.infrastructure.security.UploadRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@Profile("!local")
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            LoginRateLimitFilter loginRateLimitFilter,
            UploadRateLimitFilter uploadRateLimitFilter) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/api/v1/auth/csrf",
                                "/api/v1/files",
                                "/api/v1/users/me",
                                "/api/v1/files/config")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterAfter(loginRateLimitFilter, JwtAuthenticationFilter.class);
                http.addFilterAfter(uploadRateLimitFilter, LoginRateLimitFilter.class);
        return http.build();
    }

        @Bean
        public UploadRateLimitFilter uploadRateLimitFilter(
                        com.securefiles.infrastructure.security.JdbcUploadRateLimiter rateLimiter,
                        RateLimitProperties properties,
                        java.time.Clock clock) {
                return new UploadRateLimitFilter(rateLimiter, properties, clock);
        }

        @Bean
        public LoginRateLimitFilter loginRateLimitFilter(
                        com.securefiles.infrastructure.security.JdbcUploadRateLimiter rateLimiter,
                        RateLimitProperties properties,
                        java.time.Clock clock) {
                return new LoginRateLimitFilter(rateLimiter, properties, clock);
        }
}