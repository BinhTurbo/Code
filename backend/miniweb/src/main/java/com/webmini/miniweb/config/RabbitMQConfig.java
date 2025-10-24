package com.webmini.miniweb.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CATEGORY_QUEUE = "category.queue";
    public static final String CATEGORY_EXCHANGE = "category.exchange";
    public static final String CATEGORY_CREATED_KEY = "category.created";
    public static final String CATEGORY_STATUS_CHANGED_KEY = "category.status.changed";

    @Bean
    public Queue categoryQueue() {
        return new Queue(CATEGORY_QUEUE, true);
    }

    @Bean
    public TopicExchange categoryExchange() {
        return new TopicExchange(CATEGORY_EXCHANGE);
    }

    @Bean
    public Binding categoryBinding(Queue categoryQueue, TopicExchange categoryExchange) {
        return BindingBuilder
                .bind(categoryQueue)
                .to(categoryExchange)
                .with("category.*");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
