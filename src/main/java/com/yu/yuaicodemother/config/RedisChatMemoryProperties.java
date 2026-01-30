package com.yu.yuaicodemother.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Redis Chat Memory 配置属性类
 */
@Data
@Validated
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisChatMemoryProperties {

    @NotBlank(message = "Redis host cannot be empty")
    private String host;

    @Min(value = 1, message = "Redis port must be greater than 0")
    @Max(value = 65535, message = "Redis port must be less than or equal to 65535")
    private int port = 6379;

    private String password;

    @Min(value = 0, message = "TTL must be non-negative")
    private long ttl = 3600;

    private int database = 0;
}
