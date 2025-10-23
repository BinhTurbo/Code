# ⚡ Quick Start - Redis & RabbitMQ

## 🚀 3 Bước Chạy Dự Án

### Bước 1: Start Docker

```bash
cd d:\Code
docker-compose up -d
```

### Bước 2: Config Email (Optional)

Mở file: `backend/miniweb/src/main/resources/application.properties`

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

> ⚠️ Bỏ qua nếu chưa muốn test email

### Bước 3: Run Backend

```bash
cd backend\miniweb
mvnw spring-boot:run
```

✅ Done! Backend chạy tại: http://localhost:8081

---

## 🧪 Test Nhanh

### Test 1: Cache Product (3 phút)

```bash
# Lần 1: Query DB (~100ms)
curl "http://localhost:8081/api/products?page=0&size=10"

# Lần 2: Từ Redis (~5ms, nhanh hơn 95%)
curl "http://localhost:8081/api/products?page=0&size=10"
```

**Verify Cache:**

```bash
docker exec -it wedmini-redis redis-cli
> KEYS products::*
```

---

### Test 2: Tạo Category + Gửi Email (5 phút)

Cần JWT token trước:

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Copy accessToken
```

Tạo category:

```bash
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"name":"Laptop","status":"ACTIVE"}'
```

**Check Log Backend:**

```
📤 Đã gửi message: Category created
📥 Nhận message: ...
✅ Đã gửi email đến: admin@example.com
```

**Check RabbitMQ UI:**

- http://localhost:15672 (admin/admin123)
- Tab **Queues** → `category.queue` → Xem message rates

---

### Test 3: Sửa Status Category → Update Products (10 phút)

**Setup:** Tạo category ID=1 với 3 products (status=ACTIVE)

**Update category:**

```bash
curl -X PUT http://localhost:8081/api/categories/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"name":"Laptop","status":"INACTIVE"}'
```

**Check Database:**

```sql
SELECT * FROM categories WHERE id=1;  -- status=INACTIVE
SELECT * FROM products WHERE category_id=1;  -- All INACTIVE
```

**Check Log:**

```
🔄 Xử lý event: Category status changed
✅ Đã cập nhật 3 sản phẩm sang INACTIVE
✅ Đã gửi email
```

---

## 📁 Files Đã Thêm/Sửa

### ✅ Đã Thêm (Mới)

```
backend/miniweb/src/main/java/com/webmini/miniweb/
├── config/
│   ├── RedisConfig.java              ⭐ NEW
│   └── RabbitMQConfig.java           ⭐ NEW
│
└── messaging/
    ├── dto/
    │   └── CategoryEventMessage.java ⭐ NEW
    ├── service/
    │   ├── EmailService.java         ⭐ NEW
    │   └── CategoryEventPublisher.java ⭐ NEW
    └── listener/
        └── CategoryEventListener.java ⭐ NEW
```

### ✏️ Đã Sửa (Updated)

```
backend/miniweb/
├── pom.xml                           ✏️ Added Redis, RabbitMQ, Email
├── src/main/resources/
│   └── application.properties        ✏️ Added Redis, RabbitMQ, Email config
│
└── src/main/java/.../catalog/
    ├── category/service/
    │   └── CategoryService.java      ✏️ Added @Cacheable, @CacheEvict, RabbitMQ
    └── product/service/
        └── ProductService.java       ✏️ Added @Cacheable, @CacheEvict

docker-compose.yml                    ✏️ Added RabbitMQ
```

### 📖 Docs (Hướng dẫn)

```
d:\Code\
├── REDIS_RABBITMQ_GUIDE.md          📖 Hướng dẫn chi tiết
├── ARCHITECTURE_DIAGRAM.md          📊 Sơ đồ kiến trúc
├── TEST_GUIDE.md                    🧪 Hướng dẫn test
└── QUICK_START.md                   ⚡ File này
```

---

## 🔍 Kiểm Tra Services

### Redis

```bash
docker exec -it wedmini-redis redis-cli ping
# Expected: PONG
```

### RabbitMQ

```bash
docker exec -it wedmini-rabbitmq rabbitmq-diagnostics ping
# Expected: Ping succeeded
```

### MySQL

```bash
docker exec -it wedmini-mysql mysql -uroot -p123456 -e "SELECT 1"
# Expected: +---+
#           | 1 |
#           +---+
```

---

## 🎯 Chức Năng Đã Triển Khai

### ✅ Redis Cache

- [x] Cache product search
- [x] Cache category by ID
- [x] Cache category search
- [x] Auto evict cache khi create/update/delete
- [x] TTL 10 phút (auto expire)

### ✅ RabbitMQ

- [x] Queue: `category.queue`
- [x] Exchange: `category.exchange` (TOPIC)
- [x] Event: Category Created → Send Email
- [x] Event: Category Status Changed → Send Email + Update Products

### ✅ Email

- [x] Template email tiếng Việt
- [x] Email khi tạo category
- [x] Email khi sửa status category (có số products bị ảnh hưởng)

---

## 🐛 Quick Troubleshooting

| Vấn đề                 | Giải pháp                                                       |
| ---------------------- | --------------------------------------------------------------- |
| Redis không connect    | `docker restart wedmini-redis`                                  |
| RabbitMQ không connect | `docker restart wedmini-rabbitmq`                               |
| Email không gửi        | Check `application.properties` hoặc comment code email          |
| Cache không work       | Check Redis: `docker exec -it wedmini-redis redis-cli KEYS "*"` |
| Message không consume  | Check log backend, check RabbitMQ UI                            |

---

## 📚 Đọc Thêm

1. **REDIS_RABBITMQ_GUIDE.md**: Hướng dẫn chi tiết, giải thích từng bước
2. **ARCHITECTURE_DIAGRAM.md**: Sơ đồ kiến trúc, luồng dữ liệu
3. **TEST_GUIDE.md**: Các test case chi tiết

---

## 💡 Workflow Điển Hình

### Kịch Bản: Admin tạo category mới và thêm sản phẩm

```
1. Tạo Category "Laptop Gaming" (status=ACTIVE)
   → RabbitMQ gửi email thông báo admin

2. Thêm 5 products vào category
   → Cache product bị xóa

3. Khách hàng search products
   → Lần 1: Query DB (100ms)
   → Lần 2+: Từ Redis (5ms)

4. Admin set category status=INACTIVE
   → RabbitMQ tự động:
      - Gửi email thông báo
      - Cập nhật 5 products → INACTIVE
      - Xóa cache

5. Khách hàng search lại
   → Không thấy products (vì INACTIVE)
```

---

## 🎓 Tóm Tắt Cho Người Mới

### Redis Cache = Tủ lạnh

- Lưu data tạm thời để lấy nhanh
- Không cần query DB liên tục
- Tự động xóa khi data thay đổi

### RabbitMQ = Hệ thống thư tín

- Gửi message không đồng bộ
- Xử lý task nặng (email, update nhiều records)
- Không làm chậm response trả về client

### Lợi Ích

- ⚡ **Performance**: Nhanh hơn 95% với cache
- 📧 **Email**: Tự động gửi thông báo
- 🔄 **Automation**: Tự động cập nhật products khi category thay đổi
- 🎯 **Async**: Không làm chậm API response

---

**Chúc bạn học tốt! 🚀**

> Nếu có câu hỏi, check log backend và RabbitMQ UI trước.
> Hầu hết lỗi đều có log chi tiết.
