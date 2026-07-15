package com.hmdp.task;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.MqConstants;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SeckillReconciliationTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Scheduled(cron = "0 */5 * * * ?")
    public void reconcile() {
        log.info("===== 秒杀对账任务开始 =====");

        Set<String> keys = stringRedisTemplate.keys(RedisConstants.SECKILL_ORDER_IDS_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            log.info("没有需要対账的秒杀券");
            return;
        }

        int totalMissing = 0;

        for (String key : keys) {
            String voucherIdStr = key.replace(RedisConstants.SECKILL_ORDER_IDS_KEY, "");
            Long voucherId = Long.parseLong(voucherIdStr);

            // 3. 获取 Redis 中所有 userId → orderId 映射
            Map<Object, Object> redisOrders = stringRedisTemplate.opsForHash().entries(key);
            if (redisOrders.isEmpty()) continue;

            // 4. 查询数据库中该券已有的订单
            Set<Long> dbUserIds = voucherOrderService.query()
                    .eq("voucher_id", voucherId)
                    .list()
                    .stream()
                    .map(VoucherOrder::getUserId)
                    .collect(Collectors.toSet());

            // 5. 对比：找出 Redis 有但数据库没有的
            for (Map.Entry<Object, Object> entry : redisOrders.entrySet()) {
                Long userId = Long.parseLong(entry.getKey().toString());
                Long orderId = Long.parseLong(entry.getValue().toString());

                if (!dbUserIds.contains(userId)) {
                    log.warn("发现丢失订单！userId={}, orderId={}, voucherId={}", userId, orderId, voucherId);
                    totalMissing++;

                    // 6. 补偿：重新发送 MQ 消息
                    VoucherOrder order = new VoucherOrder();
                    order.setId(orderId);
                    order.setUserId(userId);
                    order.setVoucherId(voucherId);
                    try {
                        rocketMQTemplate.syncSend(MqConstants.SECKILL_ORDER_TOPIC, order);
                        log.info("补偿消息发送成功，orderId={}", orderId);
                    } catch (Exception e) {
                        log.error("补偿消息发送失败，orderId={}", orderId, e);
                    }
                }
            }
        }

        log.info("===== 秒杀对账任务结束，发现 {} 条丢失订单 =====", totalMissing);
    }
}
