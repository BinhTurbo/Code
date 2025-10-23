# ✅ Checklist - Redis & RabbitMQ Integration

## 📋 Trước Khi Bắt Đầu

- [ ] Đã clone/pull project mới nhất
- [ ] Đã cài Docker Desktop
- [ ] Đã cài JDK 17+
- [ ] Đã cài Maven hoặc sử dụng mvnw
- [ ] Đã đọc README.md
- [ ] Đã đọc QUICK_START.md

---

## 🐳 Docker Setup

- [ ] Chạy `docker-compose up -d`
- [ ] Verify MySQL: `docker ps | grep mysql`
- [ ] Verify Redis: `docker exec -it wedmini-redis redis-cli ping`
- [ ] Verify RabbitMQ: Mở http://localhost:15672 (admin/admin123)
- [ ] Verify Adminer: Mở http://localhost:8080

---

## 📧 Email Configuration (Optional)

- [ ] Đã tạo Gmail App Password
- [ ] Đã cập nhật `spring.mail.username` trong application.properties
- [ ] Đã cập nhật `spring.mail.password` trong application.properties
- [ ] Đã test gửi email thử

> ⚠️ Có thể bỏ qua bước này nếu chưa muốn test email

---

## 🚀 Backend Setup

- [ ] `cd backend/miniweb`
- [ ] `mvnw clean install` (hoặc rebuild trong IDE)
- [ ] Không có build error
- [ ] `mvnw spring-boot:run` (hoặc run trong IDE)
- [ ] Backend started successfully tại port 8081
- [ ] Không có connection error (MySQL, Redis, RabbitMQ)

---

## 🧪 Test Redis Cache

### Test 1: Product Search Cache

- [ ] GET `/api/products?q=laptop` (Lần 1: Cache MISS)
- [ ] Thấy log Hibernate query trong console
- [ ] Response time ~100ms
- [ ] GET `/api/products?q=laptop` (Lần 2: Cache HIT)
- [ ] KHÔNG thấy log Hibernate query
- [ ] Response time ~5ms
- [ ] Verify cache: `docker exec -it wedmini-redis redis-cli KEYS products::*`

### Test 2: Category Cache

- [ ] GET `/api/categories/1` (Lần 1: Cache MISS)
- [ ] GET `/api/categories/1` (Lần 2: Cache HIT)
- [ ] Verify cache: `docker exec -it wedmini-redis redis-cli GET "categories::1"`

### Test 3: Cache Eviction

- [ ] POST `/api/products` (tạo product mới)
- [ ] Cache product bị xóa
- [ ] Verify: `docker exec -it wedmini-redis redis-cli KEYS products::*` → Empty
- [ ] GET `/api/products` lại → Cache MISS (query DB)

---

## 🐰 Test RabbitMQ

### Test 1: Category Created Event

- [ ] POST `/api/categories` với data: `{"name":"Test Category","status":"ACTIVE"}`
- [ ] Category được tạo thành công
- [ ] Thấy log: `📤 Đã gửi message: Category created`
- [ ] Thấy log: `📥 Nhận message: ...`
- [ ] Thấy log: `✅ Đã gửi email đến: admin@example.com`
- [ ] Check RabbitMQ UI: http://localhost:15672 → Queues → category.queue
- [ ] Thấy message rate: Publish +1, Deliver +1, Ack +1
- [ ] (Optional) Check email inbox

### Test 2: Category Status Changed Event

#### Setup

- [ ] Tạo category ID=1 với status=ACTIVE
- [ ] Tạo 3 products thuộc category ID=1, tất cả status=ACTIVE

#### Test

- [ ] PUT `/api/categories/1` với data: `{"name":"...","status":"INACTIVE"}`
- [ ] Category status → INACTIVE
- [ ] Thấy log: `📤 Đã gửi message: Category status changed`
- [ ] Thấy log: `🔄 Xử lý event: Category status changed`
- [ ] Thấy log: `✅ Đã cập nhật 3 sản phẩm sang INACTIVE`
- [ ] Thấy log: `✅ Đã gửi email`
- [ ] Verify DB: `SELECT * FROM categories WHERE id=1` → status=INACTIVE
- [ ] Verify DB: `SELECT * FROM products WHERE category_id=1` → All INACTIVE
- [ ] Check RabbitMQ UI: Message processed
- [ ] (Optional) Check email với số products bị ảnh hưởng

---

## 📊 Performance Test

- [ ] Xóa cache: `docker exec -it wedmini-redis redis-cli FLUSHALL`
- [ ] GET `/api/products?page=0&size=100` → Đo thời gian (~100-200ms)
- [ ] GET `/api/products?page=0&size=100` → Đo thời gian (~5-10ms)
- [ ] Tính performance gain: (200-10)/200 \* 100% ≈ 95%

---

## 🔍 Monitoring

### Redis

- [ ] Connect: `docker exec -it wedmini-redis redis-cli`
- [ ] KEYS \*
- [ ] INFO stats
- [ ] MONITOR (real-time commands)

### RabbitMQ

- [ ] Mở UI: http://localhost:15672
- [ ] Login: admin/admin123
- [ ] Tab Overview: Xem message rates
- [ ] Tab Queues: Xem category.queue details
- [ ] Tab Exchanges: Xem category.exchange bindings

---

## 📖 Documentation Review

- [ ] Đã đọc QUICK_START.md
- [ ] Đã đọc REDIS_RABBITMQ_GUIDE.md (ít nhất phần Tổng Quan)
- [ ] Đã xem ARCHITECTURE_DIAGRAM.md
- [ ] Đã đọc TEST_GUIDE.md
- [ ] Đã xem api-tests.http
- [ ] Đã đọc IMPLEMENTATION_SUMMARY.md (phần Workflow)

---

## 🐛 Troubleshooting

### Redis Issues

- [ ] Redis không connect → `docker restart wedmini-redis`
- [ ] Cache không work → Check RedisConfig.java có @EnableCaching
- [ ] Cache không expire → Check application.properties TTL config

### RabbitMQ Issues

- [ ] RabbitMQ không connect → `docker restart wedmini-rabbitmq`
- [ ] Message không consume → Check listener có @RabbitListener
- [ ] Queue không tồn tại → Check RabbitMQConfig.java

### Email Issues

- [ ] Email không gửi → Check application.properties email config
- [ ] SMTP error → Dùng Gmail App Password, không phải password thường
- [ ] Không nhận email → Check spam folder

---

## ✨ Advanced Features (Optional)

- [ ] Implement Dead Letter Queue (DLQ)
- [ ] Add retry mechanism với exponential backoff
- [ ] HTML email template với Thymeleaf
- [ ] Cache warming on startup
- [ ] Metrics export (Prometheus)
- [ ] Custom cache key generator
- [ ] Message priority queue
- [ ] Circuit breaker cho email service

---

## 🎯 Final Verification

- [ ] Backend chạy không error
- [ ] MySQL, Redis, RabbitMQ đều connect thành công
- [ ] Cache product search work (Cache HIT/MISS)
- [ ] RabbitMQ gửi email khi tạo category
- [ ] RabbitMQ cập nhật products khi sửa status category
- [ ] Logs rõ ràng, dễ debug
- [ ] Performance tăng ~95% với cache

---

## 📝 Notes

### Những Điều Quan Trọng Cần Nhớ:

1. **Cache Key:**

   - Products: `products::{id}` hoặc `products::{q}_{sku}_{categoryId}_{status}_{...}`
   - Categories: `categories::{id}` hoặc `categories::{q}_{status}_{...}`

2. **Cache Eviction:**

   - Create/Update/Delete → `@CacheEvict(allEntries=true)`

3. **RabbitMQ Message Flow:**

   - Service → Publisher → Exchange → Queue → Listener

4. **Async Processing:**

   - Email sending không block API response
   - Product update không block API response

5. **Error Handling:**
   - Email fail → Log error, không throw exception
   - Listener fail → Log error (có thể implement retry)

---

## 🎓 Learning Outcomes

Sau khi hoàn thành checklist này, bạn đã học được:

- ✅ Cách tích hợp Redis Cache trong Spring Boot
- ✅ Sử dụng @Cacheable, @CacheEvict annotations
- ✅ Cách tích hợp RabbitMQ trong Spring Boot
- ✅ Publisher-Subscriber pattern
- ✅ Event-driven architecture
- ✅ Asynchronous processing
- ✅ Cache-Aside pattern
- ✅ Docker Compose multi-container setup
- ✅ Performance optimization techniques
- ✅ Monitoring và debugging distributed systems

---

## 📅 Timeline

### Day 1 (2-3 hours)

- [ ] Setup Docker
- [ ] Read QUICK_START.md
- [ ] Run backend
- [ ] Test basic cache (Product search)

### Day 2 (2-3 hours)

- [ ] Read REDIS_RABBITMQ_GUIDE.md
- [ ] Test RabbitMQ (Category events)
- [ ] Configure email (optional)
- [ ] Monitor RabbitMQ UI

### Day 3 (1-2 hours)

- [ ] Read ARCHITECTURE_DIAGRAM.md
- [ ] Understand workflow
- [ ] Performance testing
- [ ] Review code

### Day 4+ (Optional)

- [ ] Explore advanced features
- [ ] Customize for your needs
- [ ] Add new events/cache

---

## ✅ Completion

- [ ] **All tests passed**
- [ ] **All monitoring tools working**
- [ ] **Documentation reviewed**
- [ ] **Ready for production (with security hardening)**

---

**Congratulations! 🎉**

Bạn đã hoàn thành việc tích hợp Redis Cache và RabbitMQ vào dự án!

---

**Date Completed:** ******\_\_\_******

**Your Name:** ******\_\_\_******

**Notes/Issues:**

---

---

---
