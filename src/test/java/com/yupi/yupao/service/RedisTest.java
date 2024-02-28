package com.gcx.team.service;

import com.gcx.team.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

/**
 * Redis 测试
 *
 */
@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("gcxString", "dog");
        valueOperations.set("gcxInt", 1);
        valueOperations.set("gcxDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("gcx");
        valueOperations.set("gcxUser", user);
        // 查
        Object gcx = valueOperations.get("gcxString");
        Assertions.assertTrue("dog".equals((String) gcx));
        gcx = valueOperations.get("gcxInt");
        Assertions.assertTrue(1 == (Integer) gcx);
        gcx = valueOperations.get("gcxDouble");
        Assertions.assertTrue(2.0 == (Double) gcx);
        System.out.println(valueOperations.get("gcxUser"));
        valueOperations.set("gcxString", "dog");
        redisTemplate.delete("gcxString");
    }
}
