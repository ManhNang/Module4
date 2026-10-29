package com.codegym.security;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Đăng ký springSecurityFilterChain với Servlet container.
 * Lớp này kế thừa AbstractSecurityWebApplicationInitializer để tự động nạp
 * filter chain của Spring Security vào mọi request.
 */
public class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {
}

