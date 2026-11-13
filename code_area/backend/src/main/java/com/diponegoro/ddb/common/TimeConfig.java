package com.diponegoro.ddb.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    /** Zona waktu tampilan: WIB. Penyimpanan tetap UTC. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
