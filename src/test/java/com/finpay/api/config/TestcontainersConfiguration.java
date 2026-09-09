package com.finpay.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final String SQL_SERVER_IMAGE =
            "mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04";
    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:7.4-alpine");

    @Bean
    @ServiceConnection
    MSSQLServerContainer sqlServerContainer() {
        return new MSSQLServerContainer(SQL_SERVER_IMAGE)
                .acceptLicense();
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379);
    }
}
