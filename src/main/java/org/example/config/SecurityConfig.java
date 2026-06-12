package org.example.config;

import lombok.RequiredArgsConstructor;
import org.example.entity.SysUser;
import org.example.repository.SysUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/init/**").permitAll()
                .requestMatchers("/api/open/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/api-clients/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/access-logs/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/webhooks/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/tags/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN", "USER")
                .requestMatchers("/api/topics/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN")
                .requestMatchers("/api/approvals/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN", "USER")
                .requestMatchers("/api/metrics/**").hasAnyRole("SUPER_ADMIN", "TOPIC_ADMIN", "USER")
                .requestMatchers("/api/users/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/", "/index.html", "/login.html", "/admin.html", "/theme.css", "/static/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable());

        return http.build();
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
            SysUser user = userRepository.findByUsernameAndIsDeletedFalse(username)
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
