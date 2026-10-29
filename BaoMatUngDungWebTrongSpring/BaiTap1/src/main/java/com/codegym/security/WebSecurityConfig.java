package com.codegym.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Lưu ý về WebSecurityConfigurerAdapter:
 * Kể từ Spring Security 5.7+ và đã bị loại bỏ hoàn toàn trong Spring Security 6+
 * (Spring MVC 6 / Tomcat 10.1+), class WebSecurityConfigurerAdapter không còn tồn tại.
 * Spring Security 6 khuyến nghị chuyển sang cấu hình dạng component-based
 * bằng cách khai báo các Bean: SecurityFilterChain và UserDetailsService.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        // Cấu hình tài khoản mẫu lưu trong bộ nhớ
        UserDetails user = User.withUsername("user")
                .password("{noop}123456")
                .roles("USER")
                .build();

        UserDetails admin = User.withUsername("admin")
                .password("{noop}123456")
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Cho phép tất cả người dùng (kể cả chưa đăng nhập) được xem danh sách bài viết và chi tiết bài viết
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/"),
                                AntPathRequestMatcher.antMatcher("/blogs"),
                                AntPathRequestMatcher.antMatcher("/blogs/{id:[0-9]+}"),
                                AntPathRequestMatcher.antMatcher("/css/**"),
                                AntPathRequestMatcher.antMatcher("/js/**")
                        ).permitAll()
                        // Chỉ người dùng đã đăng nhập (authenticated) mới được phép viết bài blog
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/blogs/create"),
                                AntPathRequestMatcher.antMatcher("/blogs/save")
                        ).authenticated()
                        // Mọi yêu cầu khác đều cần xác thực
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/blogs", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/blogs")
                        .permitAll()
                );

        return http.build();
    }
}

