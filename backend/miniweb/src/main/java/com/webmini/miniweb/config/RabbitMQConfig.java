package com.webmini.miniweb.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ
 * - Tạo Queue (hàng đợi) để lưu message
 * - Tạo Exchange để định tuyến message
 * - Tạo Binding để kết nối Queue và Exchange
 */
@Configuration
public class RabbitMQConfig {

    // Tên các Queue
    public static final String CATEGORY_QUEUE = "category.queue";
    
    // Tên Exchange
    public static final String CATEGORY_EXCHANGE = "category.exchange";
    
    // Routing keys
    public static final String CATEGORY_CREATED_KEY = "category.created";
    public static final String CATEGORY_STATUS_CHANGED_KEY = "category.status.changed";

    /**
     * Tạo Queue để lưu trữ message
     * durable = true: Queue không bị mất khi RabbitMQ restart
     */
    @Bean
    public Queue categoryQueue() {
        return new Queue(CATEGORY_QUEUE, true);
    }

    /**
     * Tạo Topic Exchange
     * Topic Exchange cho phép routing message dựa trên pattern
     */
    @Bean
    public TopicExchange categoryExchange() {
        return new TopicExchange(CATEGORY_EXCHANGE);
    }

    /**
     * Binding: Kết nối Queue với Exchange thông qua routing key
     * Tất cả message có routing key bắt đầu với "category." sẽ được gửi vào categoryQueue
     */
    @Bean
    public Binding categoryBinding(Queue categoryQueue, TopicExchange categoryExchange) {
        return BindingBuilder
                .bind(categoryQueue)
                .to(categoryExchange)
                .with("category.*"); // Pattern: category.created, category.status.changed, etc.
    }

    /**
     * Message Converter: Chuyển đổi object Java thành JSON và ngược lại
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate: Công cụ để gửi message
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
