package com.yu.yuaicodemother.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local") // 假设本地有 application-local.yml 或者使用 application.yml
class RedisChatMemoryConfigTest {

    @Autowired
    private RedisChatMemoryProperties properties;

    @Autowired
    private RedisChatMemoryStoreConfig config;

    @Test
    void testPropertiesBinding() {
        assertNotNull(properties, "配置属性类不应为空");
        assertNotNull(properties.getHost(), "Host 不应为空");
        System.out.println("Loaded Host: " + properties.getHost());
        assertEquals("192.168.100.128", properties.getHost());
        assertEquals(6379, properties.getPort());
    }

    @Test
    void testBeanCreation() {
        assertDoesNotThrow(() -> config.redisChatMemoryStore(properties), "Bean 创建不应抛出异常");
    }
}
