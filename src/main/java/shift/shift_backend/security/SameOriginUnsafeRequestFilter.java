package shift.shift_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SameOriginUnsafeRequestFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.TRACE.name()
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (!SAFE_METHODS.contains(request.getMethod()) && origin != null && !origin.isBlank()
                && !isSameOrigin(request, origin)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Cross-origin state-changing requests are not allowed.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSameOrigin(HttpServletRequest request, String origin) {
        try {
            URI originUri = URI.create(origin);
            String requestScheme = request.getScheme();
            String requestHost = request.getServerName();
            int requestPort = normalizePort(requestScheme, request.getServerPort());
            int originPort = normalizePort(originUri.getScheme(), originUri.getPort());

            return requestScheme.equalsIgnoreCase(originUri.getScheme())
                    && requestHost.equalsIgnoreCase(originUri.getHost())
                    && requestPort == originPort;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private int normalizePort(String scheme, int port) {
        if (port > 0) {
            return port;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return port;
    }
}
