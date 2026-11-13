package com.diponegoro.ddb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Backend Diponegoro Digital Bank — modular monolith (SRS 2.1).
 * Satu paket per modul bisnis: auth, account, transfer, payment, topup, card, history, profile,
 * notification; ditambah ledger (Financial Transaction Core) yang dipakai bersama.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaRepositories(considerNestedRepositories = true)
@EnableAsync
@EnableScheduling
public class DdbBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdbBackendApplication.class, args);
    }
}
