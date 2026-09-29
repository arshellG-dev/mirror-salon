package com.mirror.salon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/services/**", "/products/**", "/reviews", "/reviews/**",
                        "/login/**", "/register/**", "/css/**", "/js/**", "/images/**", "/uploads/**",
                        "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasAnyRole("ADMIN", "STYLIST", "RECEPTIONIST")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/staff/login")
                .loginProcessingUrl("/staff/login")
                .defaultSuccessUrl("/staff/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/staff/logout")
                .logoutSuccessUrl("/staff/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/uploads/**"))
            .httpBasic(HttpBasicConfigurer::disable);

        return http.build();
    }
}
