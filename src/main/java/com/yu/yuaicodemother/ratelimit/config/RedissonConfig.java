package com.yu.yuaicodemother.ratelimit.config;

// 1. 修正导包，使用 Redisson 的 Config
import org.redisson.config.Config;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;

        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10)
                .setIdleConnectionTimeout(30000)
                .setConnectTimeout(5000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        /*
        * .
        *
        *  rateLimiter.trySetRate(...) —— 业务规则（限流算法的核心）
这是令牌桶算法的初始化参数。它定义了“限流”本身的逻辑。
作用：告诉 Redisson 这个限流器的容量是多少，恢复速度是多少。
参数含义：
rate: 生成令牌的数量（比如 5 个）。
rateInterval: 时间窗口大小（比如 1 秒）。
含义：在这个时间窗口内，最多允许处理多少个请求。
生命周期：一旦设置成功，这个规则就会保存在 Redis 中。注意 trySetRate 的特性是：如果该 key 已经存在且配置过，它不会覆盖旧配置（除非你先删掉 key）。
类比：这就像是你告诉门卫：“每分钟只能放 5 个人进去”。这是门卫执行工作的守则。
2. rateLimiter.expire(Duration.ofHours(1)) —— 资源管理（Redis 内存清理）
这是 Redis Key 的过期时间（TTL）。它与限流算法本身无关，而是为了防止 Redis 内存爆炸。
作用：设置这个限流器对象（Redis 中的 Key）在多久没有被访问后，自动从 Redis 中删除。
为什么需要它？
假设你的 key 是基于 userId 生成的（例如 rate_limit:user:1001）。
如果不设置过期时间，每当有一个新用户访问，Redis 里就会多一个 Key。
如果系统运行了几年，有几千万个用户访问过，Redis 里就会存几千万个限流器对象，即使这些用户可能只访问了一次就再也没来过。这会导致 Redis 内存泄漏。
代码逻辑：代码中每次请求都执行 expire，意味着实现了**“续期”**机制。只要该用户在 1 小时内还在活跃，这个限流器就一直存在；如果用户停止操作超过 1 小时，Redis 就会自动清理掉这个 Key。
类比：这就像是酒店经理说：“如果这个房间连续 1 小时没有人住，就让保洁阿姨把房间彻底打扫并锁起来（回收资源）”。
*
* */


        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }

        // 2. 修正创建方法，传入 config 对象
        return Redisson.create(config);
    }
}