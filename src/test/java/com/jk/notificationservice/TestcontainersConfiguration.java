package com.jk.notificationservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(MYSQL_IMAGE)
                .withDatabaseName("notification_service")
                .withUsername("application")
                .withPassword("password")
                .withInitScript("db/schema.sql");
    }

}
