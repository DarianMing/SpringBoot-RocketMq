package com.lm.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Configuration
public abstract class RocketMqConfigure {

    private String namesrvAddr = "127.0.0.1:9876";
    
    private String groupName = "lm-rocketMq";

    /**
     * 创建普通消息发送者实例
     *
     * @return
     * @throws MQClientException
     */
//    @Bean
//    //@ConditionalOnProperty(prefix = "rocketmq.producer", value = "default", havingValue = "true")
//    public DefaultMQProducer defaultProducer() throws MQClientException {
//        log.info("defaultProducer 正在创建---------------------------------------");
//        DefaultMQProducer producer = new DefaultMQProducer(groupName);
//        producer.setNamesrvAddr(namesrvAddr);
//        producer.setVipChannelEnabled(false);
//        producer.setRetryTimesWhenSendAsyncFailed(10);
//        producer.start();
//        log.info("rocketmq producer server开启成功---------------------------------.");
//        return producer;
//    }

    // 开启消费者监听服务
    public void listener(String topic, String tag) throws MQClientException {
        log.info("开启" + topic + ":" + tag + "消费者-------------------");

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(groupName);

        consumer.setNamesrvAddr(namesrvAddr);

        consumer.subscribe(topic, tag);

        // 开启内部类实现监听
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {

                return RocketMqConfigure.this.dealBody(msgs);
            }
        });
        consumer.start();

        log.info("rocketmq启动成功---------------------------------------");

    }

    /**
     * 创建事务消息发送者实例
     *
     * @return
     * @throws MQClientException
     */
    @Bean
    //@ConditionalOnProperty(prefix = "rocketmq.producer", value = "transaction", havingValue = "true")
    public TransactionMQProducer transactionMQProducer() throws MQClientException {
        log.info("TransactionMQProducer 正在创建---------------------------------------");
        TransactionMQProducer producer = new TransactionMQProducer(groupName);
        ExecutorService executorService = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("client-transaction-msg-check-thread");
                return thread;
            }
        });
        producer.setNamesrvAddr(namesrvAddr);
        producer.setExecutorService(executorService);
        producer.start();
        log.info("TransactionMQProducer server开启成功---------------------------------.");
        return producer;
    }

    // 处理body的业务
    public abstract ConsumeConcurrentlyStatus dealBody(List<MessageExt> msgs);


}