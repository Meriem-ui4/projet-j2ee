package com.memorygame;

import org.springframework.context.annotation.*;
import org.springframework.orm.hibernate5.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Classe de configuration Spring pour la couche persistance.
 * Declare les beans DataSource, SessionFactory et TransactionManager
 * utilises par Hibernate pour acceder a la base de donnees MySQL.
 *
 * Prerequis avant lancement :
 *   1. Demarrer WampServer (MySQL sur le port 3308)
 *   2. Creer la base de donnees :
 *        CREATE DATABASE memorygame CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 *   3. Les tables sont creees automatiquement par Hibernate (hbm2ddl.auto=update)
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.memorygame.service", "com.memorygame.repository" })
public class AppConfig {

    /**
     * Configure la source de donnees JDBC vers MySQL (WampServer).
     * Le pilote com.mysql.cj.jdbc.Driver est fourni par mysql-connector-java.
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:3308/memorygame?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8");
        ds.setUsername("root");
        ds.setPassword("");
        return ds;
    }

    /**
     * Configure la SessionFactory Hibernate.
     * Analyse le package model pour detecter les entites JPA annotees.
     * hbm2ddl.auto=update cree ou met a jour les tables au demarrage.
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sf = new LocalSessionFactoryBean();
        sf.setDataSource(dataSource());
        sf.setPackagesToScan("com.memorygame.model");

        Properties props = new Properties();
        props.put("hibernate.dialect",        "org.hibernate.dialect.MySQL8Dialect");
        props.put("hibernate.hbm2ddl.auto",   "update");
        props.put("hibernate.show_sql",        "false");
        props.put("hibernate.format_sql",      "true");
        props.put("hibernate.connection.pool_size", "10");
        props.put("hibernate.connection.characterEncoding", "utf8");
        props.put("hibernate.connection.useUnicode", "true");
        sf.setHibernateProperties(props);
        return sf;
    }

    /**
     * Configure le gestionnaire de transactions Hibernate.
     * Permet l'utilisation de l'annotation @Transactional dans les services.
     */
    @Bean
    public HibernateTransactionManager transactionManager() {
        HibernateTransactionManager tm = new HibernateTransactionManager();
        tm.setSessionFactory(sessionFactory().getObject());
        return tm;
    }
}
