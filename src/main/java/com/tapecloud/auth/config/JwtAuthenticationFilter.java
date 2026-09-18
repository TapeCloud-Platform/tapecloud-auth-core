package com.tapecloud.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.entity.Role;
import com.tapecloud.auth.user.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        if (!jwtService.isTokenValid(jwt)) {
            rejectWithMessage(response, "El token expiró o no es válido. Iniciá sesión de nuevo.");
            return;
        }

        String userEmail = jwtService.extractEmail(jwt);
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // El estado de la cuenta y sus roles se validan contra la base en cada request
            // (no contra lo que diga el token): así una cuenta deshabilitada, un cambio de
            // contraseña o una revocación de rol invalidan sesiones existentes de inmediato.
            Optional<AppUser> maybeUser = userRepository.findByEmailIgnoreCase(userEmail);
            if (maybeUser.isEmpty()) {
                rejectWithMessage(response, "El token expiró o no es válido. Iniciá sesión de nuevo.");
                return;
            }

            AppUser user = maybeUser.get();
            if (!user.isEnabled()) {
                rejectWithMessage(response, "Esta cuenta está deshabilitada.");
                return;
            }
            if (user.getTokenVersion() != jwtService.extractTokenVersion(jwt)) {
                rejectWithMessage(response, "Tu sesión ya no es válida (se cambió la contraseña o los permisos). Iniciá sesión de nuevo.");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userEmail,
                    null,
                    user.getRoles().stream().map(Role::getName).map(SimpleGrantedAuthority::new).toList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private void rejectWithMessage(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Map.of("message", message)));
    }
}
