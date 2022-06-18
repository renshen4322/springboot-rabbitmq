package com.sxw.springbootconsumer.consumer;

import com.rabbitmq.client.Channel;
import com.sxw.entity.Order;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OrderReceiver {
    //配置监听的哪一个队列，同时在没有queue和exchange的情况下会去创建并建立绑定关系
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order-queue",durable = "true"),
            exchange = @Exchange(name="order-exchange",durable = "true",type = "topic"),
            key = "order.*"
        )
    )
    @RabbitHandler//如果有消息过来，在消费的时候调用这个方法
    public void onOrderMessage(@Payload Order order, @Headers Map<String,Object> headers, Channel channel) throws IOException {
        //消费者操作
        System.out.println("---------收到消息，开始消费---------");
        System.out.println("订单ID：" + order.getId());

        /**
         * Delivery Tag 用来标识信道中投递的消息。RabbitMQ 推送消息给 Consumer 时，会附带一个 Delivery Tag，
         * 以便 Consumer 可以在消息确认时告诉 RabbitMQ 到底是哪条消息被确认了。
         * RabbitMQ 保证在每个信道中，每条消息的 Delivery Tag 从 1 开始递增。
         */
        Long deliveryTag = null;
        try {
            deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

            /**
             *  multiple 取值为 false 时，表示通知 RabbitMQ 当前消息被确认
             *  如果为 true，则额外将比第一个参数指定的 delivery tag 小的消息一并确认
             */
            boolean multiple = false;

            //ACK,确认一条消息已经被消费
            channel.basicAck(deliveryTag, multiple);
      /*  } catch (SQLException exception) {
            System.out.println("MQ消费者报错啦，这个错误我们自己处理，不需要再发了{}" + exception.getMessage());
            // Nack - 告诉MQ，我处理有点问题，但是这个问题我能处理，不用继续给我发了 丢弃或者丢到死信队列
            channel.basicNack(deliveryTag, false, false);*/
        } catch (Exception e) {
            System.out.println("出现了意料之外的异常，再重发一次" + e.getMessage());
            // Nack - 告诉MQ，我收到了，但是有意料不到的异常，再给我发一次。
            // requeue: true是继续， false是丢弃或者丢到死信队列
            channel.basicNack(deliveryTag, false, true);
            // 根据不同的异常，和业务需要，采取不通的措施
        }
        // 如果不给回复，就等这个consumer断开链接后再继续
    }
}
