package com.appvault.server.security;

import com.appvault.server.model.ServiceToken;
import com.appvault.server.service.ServiceTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Custom authentication filter that validates X-Vault-Token header.
 * Sets the security context with the service identity (name, environment, region).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final ServiceTokenService serviceTokenService;

    @Value("${vault.admin.token}")
    private String adminToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader("X-Vault-Token");

        if (token != null && !token.isBlank()) {
            // Check admin token first
            if (token.equals(adminToken)) {
                VaultPrincipal principal = new VaultPrincipal("admin", "*", "*", true);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                                        new SimpleGrantedAuthority("ROLE_SERVICE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Admin authenticated");
            } else {
                // Validate service token
                Optional<ServiceToken> serviceToken = serviceTokenService.validateToken(token);
                if (serviceToken.isPresent()) {
                    ServiceToken st = serviceToken.get();
                    VaultPrincipal principal = new VaultPrincipal(
                            st.getServiceName(), st.getEnvironment(), st.getRegion(), false);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Service authenticated: {} [{}]", st.getServiceName(), st.getEnvironment());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Principal representing the authenticated service.
     * Controllers use this to know WHO is asking for secrets.
     */
    public record VaultPrincipal(
            String serviceName,
            String environment,
            String region,
            boolean isAdmin
    ) {}
}
