package com.diponegoro.ddb.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ddb")
public record DdbProperties(Security security, boolean exposeOtp, boolean seedDemoData) {
    public record Security(String jwtSecret, long accessTokenMinutes, long refreshTokenDays, String dataKey) {}
}
