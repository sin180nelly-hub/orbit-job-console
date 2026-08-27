package com.nelly.navigatornest.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 已不再自動註冊 H2 Console，需手動掛上 JakartaWebServlet。
 * 瀏覽器開啟：http://localhost:8080/h2-console
 * JDBC URL：jdbc:h2:file:./data/navigator-nest/navigator_nest
 * User：sa　Password：（空白）
 */
@Configuration
public class H2ConsoleConfig {

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
        ServletRegistrationBean<JakartaWebServlet> registration =
                new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
        registration.setName("h2-console");
        registration.setLoadOnStartup(1);
        // 本機開發方便；正式環境請關閉
        registration.addInitParameter("webAllowOthers", "true");
        registration.addInitParameter("trace", "false");
        return registration;
    }
}
