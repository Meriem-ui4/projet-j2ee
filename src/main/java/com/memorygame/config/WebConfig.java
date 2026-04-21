package com.memorygame.config;

import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

/**
 * Configuration Spring MVC.
 * Declare le moteur de templates Thymeleaf et le gestionnaire
 * de ressources statiques (CSS, JS servis depuis le classpath).
 * Les images et backgrounds sont servis directement par le
 * DefaultServlet de Tomcat depuis le dossier webapp/.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.memorygame.controller")
public class WebConfig implements WebMvcConfigurer {

    /**
     * Resolveur de templates Thymeleaf.
     * Les fichiers HTML sont recherches dans /WEB-INF/templates/.
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        return resolver;
    }

    /**
     * Moteur de templates Thymeleaf avec support de Spring Expression Language.
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    /**
     * Resolveur de vues qui utilise Thymeleaf pour generer le HTML final.
     */
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        resolver.setCharacterEncoding("UTF-8");
        return resolver;
    }

    /**
     * Expose le dossier classpath:/static/ sous l'URL /static/.
     * Permet de servir des ressources supplementaires via le classpath si necessaire.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
    }

    /**
     * Active le DefaultServlet de Tomcat pour les ressources placees dans webapp/.
     * Cela permet a Tomcat de servir directement les dossiers images/, css/, js/
     * et backgrounds/ sans passer par Spring MVC.
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
