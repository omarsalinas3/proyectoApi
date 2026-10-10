package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

// CORRECCIÓN: Se corrigió maxLifetime a 30000ms (mínimo permitido por HikariCP).
// CORRECCIÓN: Se agregó 'em.setJpaPropertyMap(properties);' para que las propiedades de Hibernate realmente se apliquen.
// CORRECCIÓN: Se inyectó DataSource como parámetro en sfEntityManagerFactory en lugar de invocar el método @Bean directamente.
// CORRECCIÓN: Se corrigió el typo 'conexcion' por 'conexion' en los logs.

/**
 * Configuración de la base de datos principal y el EntityManagerFactory.
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.proyecto.servicios.repositorys.sf",
                "com.proyecto.servicios.repositorys.gestopago",
                "com.proyecto.servicios.repositorys.producto",
                "com.proyecto.servicios.repositorys.cliente"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {

    // [jdbc:]postgres[ql]://user:pass@host[:port]/db[?query]
    private static final java.util.regex.Pattern EMBEDDED_CREDENTIALS_URL = java.util.regex.Pattern.compile(
            "^(?:jdbc:)?postgres(?:ql)?://([^:/@]+):(.+)@([^:/@]+)(?::(\\d+))?(/[^?]*)(\\?.*)?$");

    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        HikariConfig config = new HikariConfig();
        try {
            String rawUrl = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String password = env.getProperty("spring.datasource.password");

            if (rawUrl != null && !rawUrl.isBlank()) {
                rawUrl = rawUrl.trim();
                // Si la URL contiene credenciales embebidas estilo Render (postgresql://user:pass@host/db)
                // el driver JDBC la rechaza: se extraen las credenciales y se reconstruye una URL limpia.
                // Se parsea con regex (no java.net.URI) para tolerar caracteres especiales en el password
                // y para no volcar credenciales en los logs.
                java.util.regex.Matcher m = EMBEDDED_CREDENTIALS_URL.matcher(rawUrl);
                if (m.matches()) {
                    username = java.net.URLDecoder.decode(m.group(1), java.nio.charset.StandardCharsets.UTF_8);
                    password = java.net.URLDecoder.decode(m.group(2), java.nio.charset.StandardCharsets.UTF_8);
                    String host = m.group(3);
                    String port = m.group(4) != null ? m.group(4) : "5432";
                    String path = m.group(5);
                    String query = m.group(6) != null ? m.group(6) : "";
                    rawUrl = "jdbc:postgresql://" + host + ":" + port + path + query;
                } else if (rawUrl.startsWith("postgresql://") || rawUrl.startsWith("postgres://")) {
                    rawUrl = "jdbc:postgresql://" + rawUrl.substring(rawUrl.indexOf("://") + 3);
                }
            }

            config.setJdbcUrl(rawUrl);
            config.setUsername(username);
            config.setPassword(password);
            String driverClass = env.getProperty("spring.datasource.driver-class-name");
            if (driverClass != null && !driverClass.isBlank()) {
                config.setDriverClassName(driverClass);
            }
            config.setMaximumPoolSize(10);

            // CORRECCIÓN: maxLifetime mínimo recomendado por HikariCP es 30000 ms.
            config.setMaxLifetime(30000);
            config.setConnectionTimeout(5000);
            config.setValidationTimeout(5000);
            config.setMinimumIdle(2);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("sfDatasource");

        }catch (Exception e){
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de: ", e);
            throw new IllegalStateException("No fue posible inicializar el DataSource sfDatasource", e);
        }
        return new HikariDataSource(config);
    }

    @Bean(name="sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(@Qualifier("sfDatasource") DataSource dataSource){
        LocalContainerEntityManagerFactoryBean em= new LocalContainerEntityManagerFactoryBean();
        try{
            em.setDataSource(dataSource);
            em.setPackagesToScan(
                    "com.proyecto.servicios.entity.sf",
                    "com.proyecto.servicios.entity.gestopago",
                    "com.proyecto.servicios.entity.producto",
                    "com.proyecto.servicios.entity.cliente"
            );
            em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);

            Map<String, Object> properties=new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto", "update"));
            properties.put("hibernate.show-sql", false);
            String dialect = env.getProperty("spring.jpa.properties.hibernate.dialect",
                    env.getProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect"));
            if (dialect != null && !dialect.isBlank()) {
                properties.put("hibernate.dialect", dialect);
            }
            properties.put("jakarta.persistence.query.timeout", 600000);
            // CORRECCIÓN: Se asigna el mapa de propiedades al EntityManagerFactory
            em.setJpaPropertyMap(properties);


        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de: ", e);
            throw new IllegalStateException("No fue posible inicializar sfEntityManagerFactory", e);
        }
        return em;
    }

    @Bean(name="sfTransactionManager")
    public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory){
        return new JpaTransactionManager(sfEntityManagerFactory);
    }
}

