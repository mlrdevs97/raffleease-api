package com.raffleease.raffleease.Common.Configs;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Setter
@ConfigurationProperties(prefix = "spring.application.hosts")
public class CorsProperties {
    private String client;

    public List<String> getClientAsList() {
        return Arrays.stream(client.split(","))
                .map(String::trim)
                .toList();
    }
}
