package com.yu.yuaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@EnableAspectJAutoProxy(exposeProxy = true)
//启用AspectJ风格的 AOP（面向切面编程）代理机制。
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.yu.yuaicodemother.mapper")
@EnableCaching
public class YuAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuAiCodeMotherApplication.class, args);
    }

}
