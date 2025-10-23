# 🧪 Test API - Redis & RabbitMQ

## 🔧 Setup

Trước khi test, chạy các lệnh sau:

```bash
# 1. Khởi động Docker services
cd d:\Code
docker-compose up -d

# 2. Chạy backend
cd backend\miniweb
mvnw spring-boot:run

# 3. Kiểm tra services
# MySQL: localhost:3306
# Redis: localhost:6379
# RabbitMQ: localhost:5672
# RabbitMQ UI: http://localhost:15672 (admin/admin123)
```

---

## 📋 Test Cases

### 1️⃣ Test Redis Cache - Product

#### Test Case 1.1: Cache Miss (Lần đầu query)

**Request:**

```http
GET http://localhost:8081/api/products?q=laptop&status=ACTIVE&page=0&size=10
Content-Type: application/json
```

**Expected:**

- Response time: ~100ms (query DB)
- Log backend:

```
Hibernate: SELECT ... FROM products WHERE ...
```

**Verify in Redis:**

```bash
docker exec -it wedmini-redis redis-cli
> KEYS products::*
# Output: "products::laptop_null_null_ACTIVE_null_0_10"
```

---

#### Test Case 1.2: Cache Hit (Lần thứ 2 query cùng điều kiện)

**Request:**

```http
GET http://localhost:8081/api/products?q=laptop&status=ACTIVE&page=0&size=10
Content-Type: application/json
```

**Expected:**

- Response time: ~5ms (lấy từ cache)
- KHÔNG có log Hibernate query
- Data giống với lần 1

---

#### Test Case 1.3: Cache Eviction (Tạo product mới)

**Request:**

```http
POST http://localhost:8081/api/products
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "sku": "SKU001",
  "name": "Laptop Dell XPS 13",
  "categoryId": 1,
  "price": 25000000,
  "stock": 10,
  "status": "ACTIVE"
}
```

**Expected:**

- Status: 200 OK
- Product được tạo
- Cache product bị xóa

**Verify:**

```bash
docker exec -it wedmini-redis redis-cli
> KEYS products::*
# Output: (empty array) - Cache đã bị xóa
```

**Re-test Cache:**

```http
GET http://localhost:8081/api/products?q=laptop&status=ACTIVE&page=0&size=10
# Lần này sẽ query DB lại (cache miss)
```

---

### 2️⃣ Test Redis Cache - Category

#### Test Case 2.1: Get Category by ID (Cache)

**Request:**

```http
GET http://localhost:8081/api/categories/1
Content-Type: application/json
```

**Expected (Lần 1):**

- Response time: ~50ms (query DB)
- Data returned

**Expected (Lần 2):**

- Response time: ~3ms (cache hit)
- Data giống lần 1

**Verify:**

```bash
docker exec -it wedmini-redis redis-cli
> GET "categories::1"
# Output: JSON của category
```

---

### 3️⃣ Test RabbitMQ - Category Created Event

#### Test Case 3.1: Tạo Category Mới

**Request:**

```http
POST http://localhost:8081/api/categories
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "name": "Điện thoại",
  "status": "ACTIVE"
}
```

**Expected Backend Log:**

```
📤 Đã gửi message: Category created - ID: 1, Name: Điện thoại
📥 Nhận message: CategoryEventMessage(categoryId=1, categoryName=Điện thoại, ...)
🎉 Xử lý event: Category created - Điện thoại
✅ Đã gửi email đến: admin@example.com
```

**Verify RabbitMQ:**

1. Mở http://localhost:15672
2. Login: admin/admin123
3. Tab **Queues** → Click `category.queue`
4. Xem section **Message rates**:
   - **Publish**: 1 message
   - **Deliver**: 1 message
   - **Ack**: 1 message

**Verify Email (nếu đã config):**

- Check email `admin@example.com`
- Subject: "🎉 Danh mục mới được tạo: Điện thoại"

---

### 4️⃣ Test RabbitMQ - Category Status Changed Event

#### Setup: Tạo category và products

**Step 1: Tạo Category**

```http
POST http://localhost:8081/api/categories
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "name": "Laptop Gaming",
  "status": "ACTIVE"
}

# Response: {id: 1, name: "Laptop Gaming", status: "ACTIVE"}
```

**Step 2: Tạo 3 Products thuộc Category này**

```http
POST http://localhost:8081/api/products
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "sku": "LAPTOP-001",
  "name": "Asus ROG",
  "categoryId": 1,
  "price": 30000000,
  "stock": 5,
  "status": "ACTIVE"
}

# Repeat 2 lần nữa với SKU khác: LAPTOP-002, LAPTOP-003
```

**Step 3: Verify trước khi test**

```sql
-- Vào MySQL
SELECT * FROM categories WHERE id = 1;
-- status = ACTIVE

SELECT * FROM products WHERE category_id = 1;
-- Có 3 products, tất cả status = ACTIVE
```

---

#### Test Case 4.1: Sửa Status Category ACTIVE → INACTIVE

**Request:**

```http
PUT http://localhost:8081/api/categories/1
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "name": "Laptop Gaming",
  "status": "INACTIVE"
}
```

**Expected Backend Log:**

```
📤 Đã gửi message: Category status changed - ID: 1, ACTIVE -> INACTIVE
📥 Nhận message: CategoryEventMessage(...)
🔄 Xử lý event: Category status changed - Laptop Gaming (ACTIVE -> INACTIVE)
✅ Đã cập nhật 3 sản phẩm sang INACTIVE
✅ Đã gửi email đến: admin@example.com
```

**Verify Database:**

```sql
-- Category status đã thay đổi
SELECT * FROM categories WHERE id = 1;
-- status = INACTIVE

-- TẤT CẢ products thuộc category cũng INACTIVE
SELECT * FROM products WHERE category_id = 1;
-- Cả 3 products đều status = INACTIVE
```

**Verify RabbitMQ:**

- Tab **Queues** → `category.queue`
- Message rates: Publish +1, Deliver +1, Ack +1

**Verify Cache:**

```bash
docker exec -it wedmini-redis redis-cli
> KEYS categories::*
# Empty - Cache category đã xóa

> KEYS products::*
# Empty - Cache product đã xóa (do listener gọi @CacheEvict)
```

**Verify Email:**

- Subject: "🔄 Trạng thái danh mục thay đổi: Laptop Gaming"
- Body chứa:
  - Trạng thái cũ: ACTIVE
  - Trạng thái mới: INACTIVE
  - Số sản phẩm bị ảnh hưởng: 3

---

#### Test Case 4.2: Sửa Status Category INACTIVE → ACTIVE

**Request:**

```http
PUT http://localhost:8081/api/categories/1
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "name": "Laptop Gaming",
  "status": "ACTIVE"
}
```

**Expected:**

- Category status → ACTIVE
- Message gửi vào RabbitMQ
- Email gửi thông báo
- **Products KHÔNG tự động chuyển về ACTIVE** (chỉ áp dụng cho INACTIVE)

**Verify:**

```sql
SELECT * FROM categories WHERE id = 1;
-- status = ACTIVE

SELECT * FROM products WHERE category_id = 1;
-- Vẫn INACTIVE (không auto-activate)
```

---

### 5️⃣ Test Performance - Cache vs No Cache

#### Test Case 5.1: Benchmark Cache Performance

**Tool:** Sử dụng cURL hoặc Postman

**Without Cache (Lần đầu):**

```bash
# Xóa cache trước
docker exec -it wedmini-redis redis-cli FLUSHALL

# Đo thời gian
curl -w "\nTime: %{time_total}s\n" \
  "http://localhost:8081/api/products?q=&page=0&size=100"

# Expected: ~100-200ms (query DB với 100 records)
```

**With Cache (Lần 2):**

```bash
# Gọi lại cùng request
curl -w "\nTime: %{time_total}s\n" \
  "http://localhost:8081/api/products?q=&page=0&size=100"

# Expected: ~5-10ms (từ Redis cache)
```

**Performance Gain:**

```
Improvement = (200ms - 10ms) / 200ms * 100% = 95% faster!
```

---

### 6️⃣ Test RabbitMQ Dead Letter Queue (Advanced)

#### Test Case 6.1: Simulate Message Processing Failure

**Setup:** Tạm thời throw exception trong listener

**File:** `CategoryEventListener.java`

```java
@RabbitListener(queues = RabbitMQConfig.CATEGORY_QUEUE)
public void handleCategoryEvent(CategoryEventMessage message) {
    log.info("📥 Nhận message: {}", message);

    // ⚠️ TEST: Throw exception để simulate failure
    if (message.getCategoryName().contains("Test")) {
        throw new RuntimeException("Simulate failure for testing");
    }

    // Normal processing...
}
```

**Request:**

```http
POST http://localhost:8081/api/categories
Content-Type: application/json

{
  "name": "Test Category",
  "status": "ACTIVE"
}
```

**Expected:**

- Category được tạo
- Message gửi vào queue
- Listener nhận message → throw exception
- RabbitMQ retry message (default: infinite retry)

**Verify RabbitMQ:**

- Tab **Queues** → `category.queue`
- **Message rates**: Redeliver rate tăng lên (message bị retry)

**Fix:** Remove exception trong code và restart app

- Message sẽ được xử lý thành công

---

### 7️⃣ Monitor Tools

#### Monitor Redis

**Real-time commands:**

```bash
docker exec -it wedmini-redis redis-cli MONITOR
# Xem tất cả commands được execute trong Redis
```

**Info:**

```bash
docker exec -it wedmini-redis redis-cli INFO
# Xem stats: memory, clients, keyspace, etc.
```

**Keys:**

```bash
docker exec -it wedmini-redis redis-cli KEYS "*"
# List tất cả keys
```

---

#### Monitor RabbitMQ

**Web UI:** http://localhost:15672

**Tabs quan trọng:**

1. **Overview**: Tổng quan message rates
2. **Connections**: Active connections từ app
3. **Channels**: Channels đang mở
4. **Queues**: Chi tiết từng queue
   - Ready: Message chưa consume
   - Unacked: Message đang process
   - Total: Tổng message
5. **Exchanges**: Routing rules

---

## 🎯 Expected Results Summary

| Test Case               | Expected Result                            |
| ----------------------- | ------------------------------------------ |
| Cache Miss              | Query DB, response ~100ms                  |
| Cache Hit               | From Redis, response ~5ms                  |
| Cache Evict             | Cache cleared after create/update          |
| Category Created        | Message → Queue → Email sent               |
| Category Status Changed | Message → Queue → Products updated + Email |
| Performance             | 95% faster with cache                      |

---

## 🐛 Common Issues

### Issue 1: Email không gửi được

```
❌ Lỗi khi gửi email đến admin@example.com
```

**Solution:**

- Check `application.properties` email config
- Dùng Gmail App Password
- Hoặc comment code gửi email để test các tính năng khác

---

### Issue 2: Cache không hoạt động

```
# Vẫn thấy log Hibernate query mặc dù đã query lần 2
```

**Solution:**

```bash
# Check Redis đang chạy
docker ps | grep redis

# Test Redis connection
docker exec -it wedmini-redis redis-cli ping
# Expected: PONG

# Check cache config
# File: RedisConfig.java phải có @EnableCaching
```

---

### Issue 3: Message không được consume

```
# Message vào queue nhưng listener không xử lý
```

**Solution:**

```bash
# Check RabbitMQ connection
docker logs wedmini-rabbitmq

# Check listener có @RabbitListener annotation
# Check queue name đúng: RabbitMQConfig.CATEGORY_QUEUE
```

---

## 📊 Test Report Template

```
Date: ___________
Tester: __________

| Test Case | Status | Time | Notes |
|-----------|--------|------|-------|
| Cache Miss | ✅ | 120ms | OK |
| Cache Hit | ✅ | 8ms | 93% faster |
| Create Category | ✅ | - | Email sent |
| Status Changed | ✅ | - | 3 products updated |

Issues Found:
- None

Overall: PASS ✅
```

---

Happy Testing! 🧪🚀
