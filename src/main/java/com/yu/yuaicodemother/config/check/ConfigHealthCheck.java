package com.yu.yuaicodemother.config.check;

import com.yu.yuaicodemother.config.RedisChatMemoryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigHealthCheck implements ApplicationRunner {

    private final RedisChatMemoryProperties redisProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("================ 配置健康检查 ================");
        checkRedisConfig();
        log.info("================ 检查结束 ================");
    }

    private void checkRedisConfig() {
        log.info("检查 RedisChatMemoryProperties...");
        boolean passed = true;
        
        if (!StringUtils.hasText(redisProperties.getHost())) {
            log.error("[FAIL] Redis Host 未配置或为空");
            passed = false;
        } else {
            log.info("[PASS] Redis Host: {}", redisProperties.getHost());
        }

        if (redisProperties.getPort() <= 0 || redisProperties.getPort() > 65535) {
            log.error("[FAIL] Redis Port 无效: {}", redisProperties.getPort());
            passed = false;
        } else {
            log.info("[PASS] Redis Port: {}", redisProperties.getPort());
        }

        if (passed) {
            log.info("Redis 配置检查通过");
        } else {
            log.error("Redis 配置检查未通过，请检查 application.yml");
        }
    }
}
