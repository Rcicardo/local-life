package com.hmdp.mq.consumer;// 文件位置：com.hmdp.mq.consumer.CacheDropConsumer

import com.hmdp.utils.CacheClient;
import com.hmdp.utils.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.apache.rocketmq.spring.annotation.MessageModel;

import javax.annotation.Resource;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstants.SHOP_CACHE_TOPIC,          // 必须与发送端一致
        consumerGroup = "hmdp_cache_drop_group",   // 每个节点启动后都会加入这个组
        messageModel = MessageModel.BROADCASTING   // 【核心】广播模式，确保每个 JVM 都能收到
)
public class CacheDropConsumer implements RocketMQListener<String> {

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(String cacheKey) {
        log.info("监听到缓存失效通知，正在清理本地缓存: {}", cacheKey);
        // 1. 删除 Redis（二级缓存）
        stringRedisTemplate.delete(cacheKey);
        // 2. 删除 Caffeine（一级缓存）
        cacheClient.invalidateLocal(cacheKey);

    }
}