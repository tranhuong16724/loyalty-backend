package com.doan.demo;

import com.doan.demo.security.AdminDetailsService;
import com.doan.demo.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private AdminDetailsService adminDetailsService;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Value("${app.allowed-origins:http://10.0.2.2}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private DaoAuthenticationProvider buildAuthProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(adminDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    // ── Security chain 1: Web Admin (Form Login + Session) ───────────────────
    // Xử lý tất cả request KHÔNG bắt đầu bằng /api/
    // /api/verify-json được chuyển vào đây để dùng session admin, không cần JWT
    @Bean
    @Order(1)
    public SecurityFilterChain webAdminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(request -> {
                    String uri = request.getRequestURI();
                    // /api/verify-json thuộc về Web Admin (session), không phải REST API
                    return !uri.startsWith("/api/") || uri.equals("/api/verify-json");
                })
                .authenticationProvider(buildAuthProvider())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                );

        return http.build();
    }

    // ── Security chain 2: REST API (JWT Stateless) ───────────────────────────
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // ── Auth ────────────────────────────────────
                                "/api/customers/register",
                                "/api/customers/login",
                                "/api/customers/loginV2",
                                "/api/customers/forgot-password",
                                "/api/customers/forgot-password/request",
                                "/api/customers/forgot-password/verify",

                                // ── Menu & Deals (public) ────────────────────
                                "/api/menu",
                                "/api/menu/**",
                                "/api/deals",
                                "/api/deals/**",

                                // ── Tier config (public) ─────────────────────
                                "/api/tiers/config",

                                // ── Voucher-codes: generate & verify QR ──────
                                // Controller prefix thực tế: /api/voucher-codes
                                // (App Android đang gọi /api/vouchers/* — xem ghi chú bên dưới)
                                "/api/voucher-codes/generate-code",
                                "/api/voucher-codes/verify-code"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}