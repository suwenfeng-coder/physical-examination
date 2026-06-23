package com.hospital.examination.config;

import com.hospital.examination.web.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/auth/sms/send",
                        "/css/**", "/js/**", "/hot-assets/**", "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String hotAssetsLocation = Path.of(System.getProperty("user.dir"), "runtime-assets")
                .toUri()
                .toString();
        registry.addResourceHandler("/hot-assets/**")
                .addResourceLocations(hotAssetsLocation)
                .setCacheControl(CacheControl.noStore());
    }
}
