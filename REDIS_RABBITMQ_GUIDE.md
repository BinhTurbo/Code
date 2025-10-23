# 🚀 Hướng Dẫn Sử Dụng Redis Cache & RabbitMQ

## 📌 Tổng Quan

Dự án đã được tích hợp:

- **Redis Cache**: Cache dữ liệu sản phẩm và danh mục để tăng hiệu suất
- **RabbitMQ**: Xử lý bất đồng bộ khi tạo/sửa danh mục (gửi email + cập nhật sản phẩm)

---

## 🏗️ Kiến Trúc

```
┌─────────────┐      ┌─────────────┐      ┌──────────────┐
│   Client    │─────▶│  Controller │─────▶│   Service    │
│  (Angular)  │      │             │      │              │
└─────────────┘      └─────────────┘      └──────┬───────┘
                                                  │
                     ┌────────────────────────────┼────────────────┐
                     │                            │                │
                     ▼                            ▼                ▼
              ┌─────────────┐            ┌──────────────┐  ┌─────────────┐
              │   Database  │            │ Redis Cache  │  │  RabbitMQ   │
              │   (MySQL)   │            │              │  │             │
              └─────────────┘            └──────────────┘  └──────┬──────┘
                                                                   │
                                                                   ▼
                                                          ┌─────────────────┐
                                                          │ Event Listener  │
                                                          │ - Send Email    │
                                                          │ - Update Products│
                                                          └─────────────────┘
```

---

## 🔧 Cài Đặt & Chạy

### 1️⃣ Khởi động Docker Services

```bash
cd d:\Code
docker-compose up -d
```

Các service được khởi động:

- **MySQL**: Port 3306
- **Redis**: Port 6379
- **RabbitMQ**: Port 5672 (AMQP), Port 15672 (Management UI)
- **Adminer**: Port 8080

### 2️⃣ Cấu hình Email (Quan trọng!)

Mở file: `backend/miniweb/src/main/resources/application.properties`

```properties
# Thay đổi thông tin email của bạn
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password  # ⚠️ Dùng App Password, không phải password thường
```

#### Cách tạo Gmail App Password:

1. Vào https://myaccount.google.com/security
2. Bật **2-Step Verification**
3. Vào **App passwords** → Tạo password mới
4. Copy password và dán vào `application.properties`

⚠️ **Nếu không muốn dùng email**: Có thể bỏ qua, hệ thống sẽ log error nhưng vẫn hoạt động bình thường.

### 3️⃣ Chạy Backend

```bash
cd backend/miniweb
mvnw clean install
mvnw spring-boot:run
```

Hoặc dùng IDE (IntelliJ/VSCode) để run `MiniwebApplication.java`

### 4️⃣ Kiểm tra RabbitMQ Management UI

Mở trình duyệt: http://localhost:15672

- Username: `admin`
- Password: `admin123`

Bạn sẽ thấy:

- **Queues**: `category.queue`
- **Exchanges**: `category.exchange`
- **Bindings**: Kết nối giữa queue và exchange

---

## 📖 Cách Hoạt Động

### 🔹 Redis Cache

#### Cache Product

```java
// Khi gọi search products lần đầu → Lấy từ DB và lưu vào Redis
GET /api/products?q=laptop&status=ACTIVE

// Lần sau cùng điều kiện → Lấy trực tiếp từ Redis (nhanh hơn)
GET /api/products?q=laptop&status=ACTIVE
```

#### Cache Category

```java
// Lấy category theo ID → Cache vào Redis với key = id
GET /api/categories/1

// Lần sau lấy cùng ID → Lấy từ Redis
GET /api/categories/1
```

#### Xóa Cache Tự Động

- Khi **create/update/delete** product → Cache product bị xóa
- Khi **create/update/delete** category → Cache category bị xóa
- Cache tự động expire sau **10 phút** (nếu không dùng)

---

### 🔹 RabbitMQ Message Queue

#### Kịch Bản 1: Tạo Category Mới

```
Client ──POST /api/categories──▶ CategoryService
                                      │
                                      ├─ Lưu vào DB
                                      │
                                      └─ Gửi message vào RabbitMQ
                                           │
                                           ▼
                                    RabbitMQ Queue
                                           │
                                           ▼
                                 CategoryEventListener
                                           │
                                           └─ Gửi email thông báo
```

**API Request:**

```bash
POST http://localhost:8081/api/categories
Content-Type: application/json

{
  "name": "Điện thoại",
  "status": "ACTIVE"
}
```

**Kết quả:**

- ✅ Category được tạo trong DB
- ✅ Message gửi vào RabbitMQ
- ✅ Email gửi đến admin: "Danh mục mới được tạo: Điện thoại"
- ✅ Cache category bị xóa

---

#### Kịch Bản 2: Sửa Status Category (ACTIVE → INACTIVE)

```
Client ──PUT /api/categories/1──▶ CategoryService
                                       │
                                       ├─ Cập nhật status → INACTIVE
                                       │
                                       └─ Gửi message vào RabbitMQ
                                            │
                                            ▼
                                     RabbitMQ Queue
                                            │
                                            ▼
                                  CategoryEventListener
                                            │
                                            ├─ Gửi email thông báo
                                            │
                                            └─ Cập nhật TẤT CẢ products
                                               của category sang INACTIVE
                                               │
                                               └─ Xóa cache product
```

**API Request:**

```bash
PUT http://localhost:8081/api/categories/1
Content-Type: application/json

{
  "name": "Điện thoại",
  "status": "INACTIVE"
}
```

**Kết quả:**

- ✅ Category status → INACTIVE
- ✅ Message gửi vào RabbitMQ
- ✅ Tất cả products thuộc category → INACTIVE (bất đồng bộ)
- ✅ Email gửi đến admin: "Trạng thái danh mục thay đổi + số sản phẩm bị ảnh hưởng"
- ✅ Cache category và product bị xóa

---

## 🧪 Test & Verify

### 1. Test Redis Cache

#### a) Test cache product search

```bash
# Lần 1: Lấy từ DB (chậm)
curl "http://localhost:8081/api/products?q=laptop&page=0&size=10"

# Lần 2: Lấy từ Redis (nhanh hơn)
curl "http://localhost:8081/api/products?q=laptop&page=0&size=10"
```

#### b) Xem cache trong Redis

```bash
# Connect vào Redis container
docker exec -it wedmini-redis redis-cli

# Xem tất cả keys
KEYS *

# Xem giá trị của 1 key
GET "products::laptop_null_null_ACTIVE_null_0_10"

# Xóa tất cả cache (test)
FLUSHALL
```

---

### 2. Test RabbitMQ

#### a) Tạo category mới

```bash
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Category",
    "status": "ACTIVE"
  }'
```

**Kiểm tra:**

1. Vào RabbitMQ UI: http://localhost:15672
2. Vào tab **Queues** → Click `category.queue`
3. Xem **Message rates** → Có message được publish và consume

**Xem log backend:**

```
📤 Đã gửi message: Category created - ID: 1, Name: Test Category
📥 Nhận message: CategoryEventMessage(...)
🎉 Xử lý event: Category created - Test Category
✅ Đã gửi email đến: admin@example.com
```

#### b) Sửa status category

```bash
# Giả sử có category ID=1 với status=ACTIVE
# Và có 3 products thuộc category này

curl -X PUT http://localhost:8081/api/categories/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Category",
    "status": "INACTIVE"
  }'
```

**Kiểm tra:**

1. Check database:

```sql
SELECT * FROM categories WHERE id = 1;  -- status = INACTIVE
SELECT * FROM products WHERE category_id = 1;  -- Tất cả status = INACTIVE
```

2. Check log:

```
📤 Đã gửi message: Category status changed - ID: 1, ACTIVE -> INACTIVE
📥 Nhận message: CategoryEventMessage(...)
🔄 Xử lý event: Category status changed - Test Category (ACTIVE -> INACTIVE)
✅ Đã cập nhật 3 sản phẩm sang INACTIVE
✅ Đã gửi email đến: admin@example.com
```

---

## 📂 Cấu Trúc Code Mới

```
backend/miniweb/src/main/java/com/webmini/miniweb/
│
├── config/
│   ├── RedisConfig.java             # ⭐ Cấu hình Redis Cache
│   └── RabbitMQConfig.java          # ⭐ Cấu hình RabbitMQ
│
├── messaging/
│   ├── dto/
│   │   └── CategoryEventMessage.java    # ⭐ DTO cho message
│   │
│   ├── service/
│   │   ├── EmailService.java            # ⭐ Service gửi email
│   │   └── CategoryEventPublisher.java  # ⭐ Gửi message vào RabbitMQ
│   │
│   └── listener/
│       └── CategoryEventListener.java   # ⭐ Nhận & xử lý message
│
└── catalog/
    ├── category/service/
    │   └── CategoryService.java     # ✏️ Đã cập nhật: Cache + RabbitMQ
    │
    └── product/service/
        └── ProductService.java      # ✏️ Đã cập nhật: Cache
```

---

## 🎯 Annotations Quan Trọng

### Redis Cache Annotations

```java
// Cache kết quả method với key = id
@Cacheable(value = "products", key = "#id")
public ProductResponse get(Long id) { ... }

// Xóa tất cả cache trong "products"
@CacheEvict(value = "products", allEntries = true)
public void create(...) { ... }
```

### RabbitMQ Annotations

```java
// Lắng nghe message từ queue
@RabbitListener(queues = RabbitMQConfig.CATEGORY_QUEUE)
public void handleCategoryEvent(CategoryEventMessage message) { ... }
```

---

## ⚙️ Tùy Chỉnh

### Thay đổi thời gian cache

File: `application.properties`

```properties
# Cache expire sau 10 phút (mặc định)
spring.cache.redis.time-to-live=600000

# Thay đổi thành 30 phút
spring.cache.redis.time-to-live=1800000
```

### Thay đổi email admin

File: `EmailService.java`

```java
// Dòng 47 và 71
sendEmail("admin@example.com", subject, content);

// Thay đổi thành
sendEmail("your-email@gmail.com", subject, content);
```

### Tắt email (chỉ test)

Comment code gửi email trong `CategoryEventListener.java`:

```java
// emailService.sendCategoryCreatedEmail(message.getCategoryName());
```

---

## 🐛 Troubleshooting

### Lỗi: Cannot connect to Redis

```
Caused by: io.lettuce.core.RedisConnectionException
```

**Giải pháp:**

```bash
# Kiểm tra Redis đang chạy
docker ps | grep redis

# Restart Redis
docker restart wedmini-redis

# Test kết nối
docker exec -it wedmini-redis redis-cli ping
# Kết quả: PONG
```

---

### Lỗi: Cannot connect to RabbitMQ

```
Caused by: java.net.ConnectException: Connection refused
```

**Giải pháp:**

```bash
# Kiểm tra RabbitMQ
docker ps | grep rabbitmq

# Restart RabbitMQ
docker restart wedmini-rabbitmq

# Xem log
docker logs wedmini-rabbitmq
```

---

### Email không được gửi

```
❌ Lỗi khi gửi email đến admin@example.com
```

**Nguyên nhân:**

- Chưa cấu hình email trong `application.properties`
- App Password không đúng
- Gmail chặn "Less secure apps"

**Giải pháp:**

1. Sử dụng Gmail App Password (không phải password thường)
2. Hoặc comment code gửi email để test các chức năng khác trước

---

## 📊 Monitoring

### Redis Monitor

```bash
# Real-time monitor Redis commands
docker exec -it wedmini-redis redis-cli monitor

# Xem thông tin
docker exec -it wedmini-redis redis-cli info
```

### RabbitMQ Monitor

- UI: http://localhost:15672
- Username: `admin` / Password: `admin123`
- Xem:
  - Queues → Message rate
  - Connections → Active connections
  - Exchanges → Bindings

---

## 💡 Best Practices

### 1. Cache Strategy

- ✅ Cache data ít thay đổi (category list, product detail)
- ✅ Xóa cache khi có update/delete
- ❌ Không cache data thay đổi liên tục (stock real-time)

### 2. Message Queue

- ✅ Dùng cho task nặng (send email, update nhiều records)
- ✅ Dùng cho xử lý bất đồng bộ
- ❌ Không dùng cho logic đồng bộ quan trọng

### 3. Error Handling

- ✅ Log lỗi rõ ràng
- ✅ Try-catch trong listener để tránh mất message
- ✅ Có thể implement Dead Letter Queue cho message failed

---

## 🎓 Giải Thích Cho Người Mới

### Redis Cache là gì?

**Hình dung:** Như cái tủ lạnh trong nhà bạn.

- Bạn mua rau từ chợ (Database) → Tốn thời gian
- Bạn cất rau vào tủ lạnh (Redis Cache)
- Lần sau cần rau → Lấy từ tủ lạnh, không cần đi chợ lại → Nhanh hơn

**Trong code:**

```java
// Lần 1: Query từ DB (chậm)
products = database.findAll();  // 100ms
redis.save("products", products);

// Lần 2: Lấy từ Redis (nhanh)
products = redis.get("products");  // 5ms
```

---

### RabbitMQ là gì?

**Hình dung:** Như hệ thống thư tín trong công ty.

- Boss (CategoryService) gửi thư (Message) vào hộp thư (Queue)
- Nhân viên (EventListener) lấy thư ra và xử lý (gửi email, cập nhật DB)
- Boss không cần chờ nhân viên xử lý xong → Tiếp tục làm việc khác

**Trong code:**

```java
// CategoryService (Boss)
eventPublisher.send("Tạo category mới: Laptop");
return "Success";  // Trả về ngay, không chờ

// CategoryEventListener (Nhân viên)
@RabbitListener
void handleMessage(String message) {
    sendEmail(message);  // Xử lý bất đồng bộ
    updateProducts();
}
```

---

## 🚀 Next Steps

Sau khi hiểu rõ, bạn có thể mở rộng:

1. ✅ Thêm Dead Letter Queue để xử lý message failed
2. ✅ Cache phức tạp hơn với Spring Cache + Redis
3. ✅ Gửi email HTML template thay vì plain text
4. ✅ Thêm notification real-time với WebSocket
5. ✅ Monitor cache hit/miss rate

---

## 📞 Support

Nếu gặp vấn đề:

1. Kiểm tra log backend
2. Kiểm tra RabbitMQ UI: http://localhost:15672
3. Test Redis: `docker exec -it wedmini-redis redis-cli ping`

Happy Coding! 🎉
