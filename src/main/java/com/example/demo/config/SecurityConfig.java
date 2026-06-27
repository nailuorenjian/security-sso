package com.example.demo.config;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 一定要放在 SAML 登录请求之前
                .addFilterBefore(new DeviceCheckFilter(),
                        Saml2WebSsoAuthenticationRequestFilter.class)
                .authorizeRequests()
                .antMatchers("/test2").permitAll()

                .anyRequest().authenticated()
                .and()
                .saml2Login()
                .defaultSuccessUrl("/test", true);
        return http.build();
    }

    private static class DeviceCheckFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String uri = request.getRequestURI();
            System.out.println("URI = " + uri);
            String ua = request.getHeader("User-Agent");
            System.out.println("UA = " + ua);

            boolean isMobile = false;
            if (ua != null) {
                String lower = ua.toLowerCase();
                isMobile = lower.contains("iphone")
                        || lower.contains("ipad")
                        || lower.contains("android");
            }
            System.out.println("isMobile = " + isMobile);

            // 1. 处理错误设备的拦截
            // 手机访问PC地址
            if ((uri.equals("/pc") || uri.startsWith("/pc/")) && isMobile) {
                System.out.println("Reject -> wrong device (Mobile accessing PC)");
                sendWrongDeviceResponse(response);
                return;
            }

            // PC访问手机地址
            if ((uri.equals("/saml") || uri.startsWith("/saml/")) && !isMobile) {
                System.out.println("Reject -> wrong device (PC accessing Mobile)");
                sendWrongDeviceResponse(response);
                return;
            }

            // 2. 核心：处理正确设备的访问，去除前缀并向后传递
            String newUri = uri;
            if (uri.startsWith("/pc/")) {
                newUri = uri.substring(3); // 去除 "/pc" ，变成 "/test"
            } else if (uri.equals("/pc")) {
                newUri = "/";
            } else if (uri.startsWith("/saml/")) {
                newUri = uri.substring(5); // 去除 "/saml" ，变成 "/test"
            } else if (uri.equals("/saml")) {
                newUri = "/";
            }

            // 如果 URI 被修改过了（去除了前缀），就包装 Request 交给下一个 Filter
            if (!newUri.equals(uri)) {
                System.out.println("Rewriting URI from " + uri + " to " + newUri);

                final String finalNewUri = newUri;
                HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getRequestURI() {
                        return finalNewUri;
                    }
                    @Override
                    public String getServletPath() {
                        return finalNewUri;
                    }
                };

                // 将包装后的 request 传给后续的过滤器
                filterChain.doFilter(wrappedRequest, response);
                return;
            }

            // 没有前缀的正常请求直接放行
            filterChain.doFilter(request, response);
        }


        private void sendWrongDeviceResponse(HttpServletResponse response) throws IOException {
            // 设置响应状态码和字符集，防止乱码
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 拒绝访问
            response.setContentType("text/html;charset=UTF-8");

            // 直接输出 HTML 给用户
            String html = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head><title>设备访问错误</title></head>" +
                    "<body style='text-align:center; padding-top:50px; font-family:sans-serif;'>" +
                    "  <h2>⚠️ 访问错误</h2>" +
                    "  <p>您使用了错误的设备访问此链接。请检查后重试。</p>" +
                    "</body>" +
                    "</html>";

            response.getWriter().write(html);
            response.getWriter().flush();
        }


    }



}