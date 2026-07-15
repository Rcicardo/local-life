package com.hmdp.mq.consumer;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstants.SECKILL_ORDER_TOPIC,
        consumerGroup = "seckill_order_group"
)
public class SeckillOrderConsumer implements RocketMQListener<VoucherOrder> {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Override
    public void onMessage(VoucherOrder voucherOrder) {
        log.info("收到秒杀订单消息，订单ID: {}, 用户: {}, 券ID: {}",
                voucherOrder.getId(), voucherOrder.getUserId(), voucherOrder.getVoucherId());
        voucherOrderService.createVoucherOrder(voucherOrder);
    }
}
