package com.webmini.miniweb.messaging.service;

import com.webmini.miniweb.config.RabbitMQConfig;
import com.webmini.miniweb.messaging.dto.CategoryEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service gửi message vào RabbitMQ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Gửi event khi category mới được tạo
     */
    public void publishCategoryCreated(Long categoryId, String categoryName, String status) {
        CategoryEventMessage message = CategoryEventMessage.builder()
                .categoryId(categoryId)
                .categoryName(categoryName)
                .eventType(CategoryEventMessage.EventType.CREATED.name())
                .newStatus(status)
                .eventTime(java.time.LocalDateTime.now())
                .build();

        // Gửi message vào RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CATEGORY_EXCHANGE,
                RabbitMQConfig.CATEGORY_CREATED_KEY,
                message
        );

        log.info("📤 Đã gửi message: Category created - ID: {}, Name: {}", categoryId, categoryName);
    }

    /**
     * Gửi event khi status của category thay đổi
     */
    public void publishCategoryStatusChanged(Long categoryId, String categoryName, String oldStatus, String newStatus) {
        CategoryEventMessage message = CategoryEventMessage.builder()
                .categoryId(categoryId)
                .categoryName(categoryName)
                .eventType(CategoryEventMessage.EventType.STATUS_CHANGED.name())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .eventTime(java.time.LocalDateTime.now())
                .build();

        // Gửi message vào RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CATEGORY_EXCHANGE,
                RabbitMQConfig.CATEGORY_STATUS_CHANGED_KEY,
                message
        );

        log.info("📤 Đã gửi message: Category status changed - ID: {}, {} -> {}", categoryId, oldStatus, newStatus);
    }
}
