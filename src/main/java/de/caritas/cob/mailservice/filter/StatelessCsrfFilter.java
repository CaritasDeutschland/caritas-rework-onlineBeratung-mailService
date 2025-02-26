package de.caritas.cob.mailservice.filter;

import de.caritas.cob.mailservice.config.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** This custom filter checks CSRF cookie and header token for equality */
public class StatelessCsrfFilter extends OncePerRequestFilter {

  private final RequestMatcher requireCsrfProtectionMatcher = new DefaultRequiresCsrfMatcher();
  private final AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandlerImpl();
  private final String csrfCookieProperty;
  private final String csrfHeaderProperty;

  public StatelessCsrfFilter(String cookieProperty, String headerProperty) {
    this.csrfCookieProperty = cookieProperty;
    this.csrfHeaderProperty = headerProperty;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (requireCsrfProtectionMatcher.matches(request)) {
      final String csrfTokenValue = request.getHeader(csrfHeaderProperty);
      final Cookie[] cookies = request.getCookies();

      String csrfCookieValue = null;
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (cookie.getName().equals(csrfCookieProperty)) {
            csrfCookieValue = cookie.getValue();
          }
        }
      }

      if (csrfTokenValue == null || !csrfTokenValue.equals(csrfCookieValue)) {
        accessDeniedHandler.handle(
            request, response, new AccessDeniedException("Missing or non-matching CSRF-token"));
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  public static final class DefaultRequiresCsrfMatcher implements RequestMatcher {
    private final Pattern allowedMethods = Pattern.compile("^(HEAD|TRACE|OPTIONS)$");

    @Override
    public boolean matches(HttpServletRequest request) {

      // Allow specific whitelist items to disable CSRF protection for Swagger UI documentation
      if (SecurityConfig.WHITE_LIST.stream()
          .parallel()
          .anyMatch(request.getRequestURI().toLowerCase()::contains)) {
        return false;
      }

      return !allowedMethods.matcher(request.getMethod()).matches();
    }
  }
}
