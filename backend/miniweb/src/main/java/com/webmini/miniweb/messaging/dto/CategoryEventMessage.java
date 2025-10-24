package com.webmini.miniweb.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEventMessage implements Serializable {
    
    private Long categoryId;
    private String categoryName;
    private String eventType;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime eventTime;
    
    public enum EventType {
        CREATED,
        STATUS_CHANGED
    }
}
