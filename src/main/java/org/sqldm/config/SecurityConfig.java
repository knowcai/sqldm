package org.sqldm.config;

import lombok.RequiredArgsConstructor;
import org.sqldm.entity.SysUser;
import org.sqldm.repository.SysUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SysUserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowedOriginPatterns(java.util.List.of("*"));
                config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(java.util.List.of("*"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);
                return config;
            }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/logout").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/init/**").permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/open/**")).permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/topics/mine").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN", "USER")
                .requestMatchers("/api/topics/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN")
                .requestMatchers("/api/approvals/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN", "USER")
                .requestMatchers("/api/metrics/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN", "USER")
                .requestMatchers("/api/users/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN", "USER")
                .requestMatchers("/", "/index.html", "/login.html", "/admin.html", "/theme.css", "/static/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jsonAuthEntryPoint())
                    .accessDeniedHandler(jsonAccessDeniedHandler()));

        return http.build();
    }

    private AuthenticationEntryPoint jsonAuthEntryPoint() {
        return (request, response, authException) -> writeJsonError(response,
                HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
    }

    private AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeJsonError(response,
                HttpServletResponse.SC_FORBIDDEN, "无访问权限");
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"success\":false,\"message\":\"" + message + "\"}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            SysUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

            if (!user.getIsActive()) {
                throw new RuntimeException("用户已禁用: " + username);
            }

            List<String> roles = new ArrayList<>();
            roles.add("ROLE_" + user.getRole());
            
            // 主题管理员还需要USER角色
            if ("TOPIC_ADMIN".equals(user.getRole())) {
                roles.add("ROLE_USER");
            }
            // 超级管理员需要所有角色
            if ("SUPER_ADMIN".equals(user.getRole())) {
                roles.add("ROLE_TOPIC_ADMIN");
                roles.add("ROLE_USER");
            }

            return User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .roles(roles.stream().map(r -> r.replace("ROLE_", "")).toArray(String[]::new))
                    .build();
        };
    }
}
