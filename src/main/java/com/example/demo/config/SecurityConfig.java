package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/test2").permitAll()
                .anyRequest().authenticated()
                .and()
                .saml2Login(saml2 -> saml2
                        // 确保验证通过后跳回你的业务接口
                        .defaultSuccessUrl("/test", true)
                );

        return http.build();
    }

}
