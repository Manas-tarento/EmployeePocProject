package com.tarento.EmployeePocProject.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class CacheService {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private ObjectMapper objectMapper;

    public Jedis getJedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis;
        }
    }

    public void putCache(int key, Object object) {
        try {
            String data = objectMapper.writeValueAsString(object);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set("nsdc_" + key, data);
                long cacheTtl = 60;
                jedis.expire("nsdc_" + key, cacheTtl);
                System.out.println("Cache_key_value nsdc_" + key + " is saved in Redis");
            }
        } catch (Exception e) {
            System.out.println("Error while putting data in Redis cache: " + e.getMessage());
        }
    }

    public String getCache(String key) {
        try {
            return getJedis().get("nsdc_" + key);
        } catch (Exception e) {
            return null;
        }
    }
}
