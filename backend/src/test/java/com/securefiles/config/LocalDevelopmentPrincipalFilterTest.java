package com.securefiles.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LocalDevelopmentPrincipalFilterTest {

    @Test
    void doFilter_shouldExposeConfiguredOwnerAsPrincipal() throws ServletException, IOException {
        LocalDevelopmentPrincipalFilter filter = new LocalDevelopmentPrincipalFilter("local-test-user");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isInstanceOf(LocalDevelopmentPrincipalRequest.class);
        LocalDevelopmentPrincipalRequest filteredRequest = (LocalDevelopmentPrincipalRequest) chain.getRequest();
        assertThat(filteredRequest.getUserPrincipal()).isNotNull();
        assertThat(filteredRequest.getUserPrincipal().getName()).isEqualTo("local-test-user");
    }
}