package com.raffleease.raffleease.Common.Configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "spring.job.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}