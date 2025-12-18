package main.java.CalenderApp.demo.config.redis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPool jedisPool(
            @Value("${app.redis.host:localhost}") String host,
            @Value("${app.redis.port:6379}") int port
    ) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(32);
        config.setMaxIdle(16);
        return new JedisPool(config, host, port);
    }
}
