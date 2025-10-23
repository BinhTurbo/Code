package com.webmini.miniweb.messaging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service gửi email
 * Đơn giản hóa: Chỉ gửi text email, không dùng HTML template
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email đơn giản
     * 
     * @param to Email người nhận
     * @param subject Tiêu đề
     * @param content Nội dung
     */
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@webmini.com"); // Email người gửi
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("✅ Đã gửi email đến: {}, Tiêu đề: {}", to, subject);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email đến {}: {}", to, e.getMessage());
            // Không throw exception để tránh làm gián đoạn luồng chính
        }
    }

    /**
     * Gửi email thông báo category mới được tạo
     */
    public void sendCategoryCreatedEmail(String categoryName) {
        String subject = "🎉 Danh mục mới được tạo: " + categoryName;
        String content = String.format(
            "Xin chào Admin,\n\n" +
            "Một danh mục mới vừa được tạo trong hệ thống:\n\n" +
            "📂 Tên danh mục: %s\n" +
            "⏰ Thời gian: %s\n\n" +
            "Trân trọng,\n" +
            "Hệ thống WebMini",
            categoryName,
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );
        
        // Thay đổi email admin tại đây
        sendEmail("buibinh2200@gmail.com", subject, content);
    }

    /**
     * Gửi email thông báo status của category thay đổi
     */
    public void sendCategoryStatusChangedEmail(String categoryName, String oldStatus, String newStatus, int affectedProducts) {
        String subject = "🔄 Trạng thái danh mục thay đổi: " + categoryName;
        String content = String.format(
            "Xin chào Admin,\n\n" +
            "Trạng thái của danh mục vừa được cập nhật:\n\n" +
            "📂 Tên danh mục: %s\n" +
            "📊 Trạng thái cũ: %s\n" +
            "📊 Trạng thái mới: %s\n" +
            "📦 Số sản phẩm bị ảnh hưởng: %d\n" +
            "⏰ Thời gian: %s\n\n" +
            "Trân trọng,\n" +
            "Hệ thống WebMini",
            categoryName,
            oldStatus,
            newStatus,
            affectedProducts,
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );
        
        // Thay đổi email admin tại đây
        sendEmail("buibinh2200@gmail.com", subject, content);
    }
}
