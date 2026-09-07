package com.finpay.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final String SQL_SERVER_IMAGE =
            "mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04";

    @Bean
    @ServiceConnection
    MSSQLServerContainer sqlServerContainer() {
        return new MSSQLServerContainer(SQL_SERVER_IMAGE)
                .acceptLicense();
    }
}
