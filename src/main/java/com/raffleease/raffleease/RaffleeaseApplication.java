package com.raffleease.raffleease;

import com.raffleease.raffleease.Common.Configs.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableConfigurationProperties(CorsProperties.class)
public class RaffleeaseApplication {
	public static void main(String[] args) {
		SpringApplication.run(RaffleeaseApplication.class, args);
	}
}
