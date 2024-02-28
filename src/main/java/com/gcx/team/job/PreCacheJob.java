package com.gcx.team.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcx.team.mapper.UserMapper;
import com.gcx.team.model.domain.User;
import com.gcx.team.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 *
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // 重点用户
    private List<Long> mainUserList = Arrays.asList(1L); // 初始化一个包含单个用户ID的列表，这个用户是我们的重点用户

    // 每天执行，预热推荐用户
    @Scheduled(cron = "0 31 0 * * *") // 定时任务注解，每天的00:31执行这个方法
    public void doCacheRecommendUser() { // 方法定义，用于执行用户推荐数据的缓存预热
        RLock lock = redissonClient.getLock("team:precachejob:docache:lock"); // 从Redisson客户端获取一个分布式锁
        try {
            // 只有一个线程能获取到锁
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) { // 尝试立即获取锁，不等待，锁的持有时间无限
                System.out.println("getLock: " + Thread.currentThread().getId()); // 打印当前获取锁的线程ID
                for (Long userId : mainUserList) { // 遍历重点用户列表
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 创建一个查询包装对象
                    Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper); // 分页查询用户，每页20条数据
                    String redisKey = String.format("team:user:recommend:%s", userId); // 格式化Redis键，包含用户ID
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue(); // 获取Redis的值操作对象
                    // 写缓存
                    try {
                        valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS); // 将查询结果存入Redis，过期时间为30秒
                    } catch (Exception e) {
                        log.error("redis set key error", e); // 如果操作Redis发生异常，记录错误日志
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e); // 如果尝试获取锁时线程被中断，记录错误日志
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) { // 如果当前线程持有锁
                System.out.println("unLock: " + Thread.currentThread().getId()); // 打印解锁的线程ID
                lock.unlock(); // 释放锁
            }
        }
    }


}
