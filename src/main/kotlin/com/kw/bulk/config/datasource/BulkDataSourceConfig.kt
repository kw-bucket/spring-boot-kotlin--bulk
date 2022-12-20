package com.kw.bulk.config.datasource

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.kw.bulk.repository.bulk"],
    entityManagerFactoryRef = "bulkEntityManagerFactory",
    transactionManagerRef = "bulkTransactionManager",
)
@EnableTransactionManagement
class BulkDataSourceConfig {

    @Primary
    @Bean(name = ["bulkDataSource"])
    @ConfigurationProperties(prefix = "config.datasource.bulk")
    fun dataSource(): HikariDataSource {
        return DataSourceBuilder.create().type(HikariDataSource::class.java).build()
    }

    @Primary
    @Bean(name = ["bulkEntityManagerFactory"])
    fun bulkEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("bulkDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        return builder.dataSource(dataSource)
            .packages("com.kw.bulk.entity.bulk")
            .persistenceUnit("Bulk")
            .build()
    }

    @Primary
    @Bean(name = ["bulkTransactionManager"])
    fun bulkTransactionManager(
        @Qualifier("bulkEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): JpaTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}
