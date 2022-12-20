package com.kw.bulk.config.datasource

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.kw.bulk.repository.raw"],
    entityManagerFactoryRef = "rawEntityManagerFactory",
    transactionManagerRef = "rawTransactionManager",
)
@EnableTransactionManagement
class RawDataSourceConfig {

    @Bean(name = ["rawDataSource"])
    @ConfigurationProperties(prefix = "config.datasource.raw")
    fun dataSource(): HikariDataSource = DataSourceBuilder.create().type(HikariDataSource::class.java).build()

    @Bean(name = ["rawEntityManagerFactory"])
    fun rawEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("rawDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean =
        builder.dataSource(dataSource)
            .packages("com.kw.bulk.entity.raw")
            .persistenceUnit("Raw")
            .build()

    @Bean(name = ["rawTransactionManager"])
    fun rawTransactionManager(
        @Qualifier("rawEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): JpaTransactionManager =
        JpaTransactionManager(entityManagerFactory)
}
