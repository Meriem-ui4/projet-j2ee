package com.memorygame;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Point d'entree de l'application web.
 * Remplace le fichier web.xml en utilisant l'API Servlet 3.0.
 * Spring MVC detecte automatiquement cette classe au demarrage de Tomcat.
 *
 * Le DispatcherServlet est mappe sur "/" pour intercepter toutes les requetes.
 */
public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{ AppConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{ WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{ "/" };
    }
}
