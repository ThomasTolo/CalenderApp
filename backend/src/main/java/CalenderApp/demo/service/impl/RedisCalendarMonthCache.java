package main.java.CalenderApp.demo.service.impl;

import CalenderApp.demo.service.CalendarMonthCache;
import CalenderApp.demo.service.view.CalendarItemView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class RedisCalendarMonthCache implements CalendarMonthCache {

    private static final String PREFIX = "cal:month:";

    private final JedisPool jedisPool;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public RedisCalendarMonthCache(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Optional<List<CalendarItemView>> get(Long userId, YearMonth month) {
        String key = key(userId, month);
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            List<CalendarItemView> items = mapper.readValue(json, new TypeReference<>() {
            });
            return Optional.of(items);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void put(Long userId, YearMonth month, List<CalendarItemView> items) {
        String key = key(userId, month);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, 60 * 10, mapper.writeValueAsString(items));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void evict(Long userId, YearMonth month) {
        String key = key(userId, month);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception ignored) {
        }
    }

    private static String key(Long userId, YearMonth month) {
        return PREFIX + userId + ":" + month;
    }
}
