package com.webmini.miniweb.messaging.listener;

import com.webmini.miniweb.catalog.product.entity.Product;
import com.webmini.miniweb.catalog.product.repo.ProductRepository;
import com.webmini.miniweb.config.RabbitMQConfig;
import com.webmini.miniweb.messaging.dto.CategoryEventMessage;
import com.webmini.miniweb.messaging.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryEventListener {

    private final EmailService emailService;
    private final ProductRepository productRepository;

    /**
     * Lắng nghe message từ category.queue
     * Annotation @RabbitListener tự động nhận message khi có message mới
     */
    @RabbitListener(queues = RabbitMQConfig.CATEGORY_QUEUE)
    public void handleCategoryEvent(CategoryEventMessage message) {
        log.info("📥 Nhận message: {}", message);

        try {
            // Xử lý theo loại event
            if (CategoryEventMessage.EventType.CREATED.name().equals(message.getEventType())) {
                handleCategoryCreated(message);
            } else if (CategoryEventMessage.EventType.STATUS_CHANGED.name().equals(message.getEventType())) {
                handleCategoryStatusChanged(message);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý message: {}", e.getMessage(), e);
            // Có thể implement retry logic hoặc dead letter queue ở đây
        }
    }

    /**
     * Xử lý khi category mới được tạo
     * - Gửi email thông báo
     */
    private void handleCategoryCreated(CategoryEventMessage message) {
        log.info("🎉 Xử lý event: Category created - {}", message.getCategoryName());
        
        // Gửi email thông báo
        emailService.sendCategoryCreatedEmail(message.getCategoryName());
    }

    /**
     * Xử lý khi status của category thay đổi
     * - Gửi email thông báo
     * - Cập nhật status của tất cả product thuộc category (nếu chuyển sang INACTIVE)
     * - Xóa cache product
     */
    private void handleCategoryStatusChanged(CategoryEventMessage message) {
        log.info("🔄 Xử lý event: Category status changed - {} ({} -> {})", 
                message.getCategoryName(), message.getOldStatus(), message.getNewStatus());

        // Nếu category chuyển sang INACTIVE, cập nhật tất cả product
        int affectedProducts = 0;
        if ("INACTIVE".equals(message.getNewStatus()) && "ACTIVE".equals(message.getOldStatus())) {
            affectedProducts = updateProductsStatus(message.getCategoryId());
        }

        // Gửi email thông báo
        emailService.sendCategoryStatusChangedEmail(
                message.getCategoryName(),
                message.getOldStatus(),
                message.getNewStatus(),
                affectedProducts
        );
    }

    /**
     * Cập nhật status của tất cả product thuộc category sang INACTIVE
     * và xóa cache
     */
    @CacheEvict(value = "products", allEntries = true)
    private int updateProductsStatus(Long categoryId) {
        List<Product> products = productRepository.findAllByCategoryId(categoryId);
        
        int count = 0;
        for (Product product : products) {
            if (product.getStatus() == Product.ProductStatus.ACTIVE) {
                product.setStatus(Product.ProductStatus.INACTIVE);
                product.setUpdatedAt(java.time.LocalDateTime.now());
                count++;
            }
        }

        if (count > 0) {
            productRepository.saveAll(products);
            log.info("✅ Đã cập nhật {} sản phẩm sang INACTIVE", count);
        }

        return count;
    }
}
