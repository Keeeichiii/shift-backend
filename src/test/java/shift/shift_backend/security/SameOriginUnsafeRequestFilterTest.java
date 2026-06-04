package shift.shift_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SameOriginUnsafeRequestFilterTest {

    private final SameOriginUnsafeRequestFilter filter = new SameOriginUnsafeRequestFilter();

    @Test
    void allowsSameOriginPost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/me");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8090);
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:8090");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsCrossOriginPost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/me");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8090);
        request.addHeader(HttpHeaders.ORIGIN, "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
