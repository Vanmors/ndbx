package com.vanmors.ndbx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()           // ← все запросы разрешены без авторизации
                )
                .csrf(csrf -> csrf.disable())           // отключаем CSRF (для тестов и API)
                .httpBasic(httpBasic -> httpBasic.disable())  // отключаем Basic Auth
                .formLogin(form -> form.disable());     // отключаем форму логина

        return http.build();
    }
}