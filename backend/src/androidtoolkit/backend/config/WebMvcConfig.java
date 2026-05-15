package androidtoolkit.backend.config;

import androidtoolkit.backend.security.TenantContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;
    private final TenantFilterAspect tenantFilterAspect;

    public WebMvcConfig(TenantContextInterceptor tenantContextInterceptor,
                        TenantFilterAspect tenantFilterAspect) {
        this.tenantContextInterceptor = tenantContextInterceptor;
        this.tenantFilterAspect = tenantFilterAspect;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TenantContext must be set first, then the Hibernate filter activated
        registry.addInterceptor(tenantContextInterceptor)
                .addPathPatterns("/api/**")
                .order(1);
        registry.addInterceptor(tenantFilterAspect)
                .addPathPatterns("/api/**")
                .order(2);
    }
}
