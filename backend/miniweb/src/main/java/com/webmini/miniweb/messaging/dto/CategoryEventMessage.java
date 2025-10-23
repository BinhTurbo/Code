package com.webmini.miniweb.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message gửi qua RabbitMQ khi có event liên quan đến Category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEventMessage implements Serializable {
    
    private Long categoryId;
    private String categoryName;
    private String eventType; // CREATED, STATUS_CHANGED
    private String oldStatus; // Chỉ dùng khi eventType = STATUS_CHANGED
    private String newStatus;
    private LocalDateTime eventTime;
    
    // Enum để định nghĩa các loại event
    public enum EventType {
        CREATED,           // Category mới được tạo
        STATUS_CHANGED     // Status của category thay đổi
    }
}
