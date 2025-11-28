package com.tuanzeebee.springboot.demosecurity.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class DemoSecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    public DemoSecurityConfig(CustomUserDetailsService userDetailsService, 
                             CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http.authorizeHttpRequests(configurer ->
                        configurer
                                .requestMatchers("/").hasRole("USER")
                                .requestMatchers("/manager/**").hasRole("MANAGER")
                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                .requestMatchers("/assets/css/**", "/assets/js/**", "/assets/images/**", "/assets/libs/**").permitAll()
                                .requestMatchers("/register/**").permitAll()
                                .requestMatchers("/requirement-food","/").permitAll()
                                .requestMatchers("/blog","/").permitAll()
                                .requestMatchers("/relax","/").permitAll()
                                .requestMatchers("/relax/**").permitAll()
                                .requestMatchers("/api/recipes/**").permitAll()
                                .requestMatchers("/api/ingredients/**").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/manager/**").hasRole("MANAGER")
                                .requestMatchers("/python/**").hasRole("ADMIN")
                                .anyRequest().authenticated()
                )
                .formLogin(form ->
                form
                    .loginPage("/login")
                    .loginProcessingUrl("/authenticateTheUser")
                    .successHandler(customAuthenticationSuccessHandler)
                    .permitAll()
            )
            .logout(logout -> 
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(configurer ->
                configurer
                    // SỬA Ở ĐÂY: Đổi từ "/error/access-denied" thành "/access-denied"
                    .accessDeniedPage("/access-denied")
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/python/**")
            );
            
        return http.build();
    }
}