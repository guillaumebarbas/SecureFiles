package com.securefiles.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {

    @Bean
    public TopicExchange scanExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    public Queue scanQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.exchange())
                .deadLetterRoutingKey(properties.retryQueue())
                .build();
    }

    @Bean
    public Queue retryQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.retryQueue())
                .withArgument("x-message-ttl", properties.retryDelayMillis())
                .deadLetterExchange(properties.exchange())
                .deadLetterRoutingKey(properties.routingKey())
                .build();
    }

    @Bean
    public Queue deadLetterQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    public Binding scanQueueBinding(Queue scanQueue, TopicExchange scanExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(scanQueue)
                .to(scanExchange)
                .with(properties.routingKey());
    }

    @Bean
    public Binding retryQueueBinding(Queue retryQueue, TopicExchange scanExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(retryQueue)
                .to(scanExchange)
                .with(properties.retryQueue());
    }

    @Bean
    public Binding deadLetterQueueBinding(
            Queue deadLetterQueue,
            TopicExchange scanExchange,
            RabbitMqProperties properties) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(scanExchange)
                .with(properties.deadLetterQueue());
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory scanRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            RabbitMqProperties properties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(properties.prefetch());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        return factory;
    }
}
