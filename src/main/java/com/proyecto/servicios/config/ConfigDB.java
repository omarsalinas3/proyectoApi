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
                "com.proyecto.servicios.repositorys.gestopago"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {

    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        HikariConfig config=new HikariConfig();
        try{
            config.setJdbcUrl(env.getProperty("spring.datasource.url"));
            config.setPassword(env.getProperty("spring.datasource.password"));
            config.setUsername(env.getProperty("spring.datasource.username"));
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
                    "com.proyecto.servicios.entity.gestopago"
            );
            em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);

            Map<String, Object> properties=new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.show-sql", false);
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
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

