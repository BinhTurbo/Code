# 🎨 Sơ Đồ Kiến Trúc Redis + RabbitMQ

## 📊 Luồng Dữ Liệu Tổng Quan

```
┌────────────────────────────────────────────────────────────────────────┐
│                         CLIENT (Angular Frontend)                      │
└───────────────────────────┬────────────────────────────────────────────┘
                            │
                            │ HTTP Request
                            ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    CONTROLLER LAYER (Spring Boot)                      │
│  ┌──────────────────┐         ┌──────────────────┐                    │
│  │ CategoryController│         │ ProductController│                    │
│  └─────────┬─────────┘         └─────────┬────────┘                    │
└────────────┼────────────────────────────┼────────────────────────────┘
             │                            │
             ▼                            ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER (Business Logic)                    │
│  ┌──────────────────┐         ┌──────────────────┐                    │
│  │ CategoryService  │         │  ProductService  │                    │
│  │                  │         │                  │                    │
│  │ @Cacheable       │         │  @Cacheable      │                    │
│  │ @CacheEvict      │         │  @CacheEvict     │                    │
│  └─────────┬────────┘         └─────────┬────────┘                    │
└────────────┼──────────────────────────┼────────────────────────────────┘
             │                          │
             │                          │
    ┌────────┼──────────┐      ┌────────┼────────┐
    │        │          │      │        │        │
    ▼        ▼          ▼      ▼        ▼        ▼
┌─────────┐ ┌────────┐ ┌────────────┐ ┌─────────┐ ┌────────┐
│ MySQL   │ │ Redis  │ │  RabbitMQ  │ │ MySQL   │ │ Redis  │
│ Database│ │ Cache  │ │  Publisher │ │ Database│ │ Cache  │
└─────────┘ └────────┘ └──────┬─────┘ └─────────┘ └────────┘
                              │
                              │ Message Queue
                              ▼
                    ┌───────────────────┐
                    │   RabbitMQ Queue  │
                    │  category.queue   │
                    └─────────┬─────────┘
                              │
                              │ Auto Consume
                              ▼
                    ┌─────────────────────┐
                    │ CategoryEventListener│
                    │  @RabbitListener     │
                    └─────────┬───────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
            ┌───────────────┐   ┌──────────────┐
            │ EmailService  │   │ Update       │
            │ Send Email    │   │ Products     │
            └───────────────┘   │ Status       │
                                └──────────────┘
```

---

## 🔄 Luồng Xử Lý Chi Tiết

### Scenario 1: Tạo Category Mới

```
Step 1: Client tạo category
┌──────────┐
│  Client  │ POST /api/categories {"name": "Laptop", "status": "ACTIVE"}
└────┬─────┘
     │
     ▼
┌────────────────┐
│CategoryController│ create(@RequestBody ...)
└────┬────────────┘
     │
     ▼
┌────────────────┐
│CategoryService │
│                │
│ 1. Validate    │
│ 2. Save to DB  │ ──────────┐
│ 3. Clear Cache │            │
│ 4. Publish     │            │
│    Message     │            │
└────┬───────────┘            │
     │                        ▼
     │                  ┌───────────┐
     │                  │   MySQL   │
     │                  │ Database  │
     │                  └───────────┘
     │
     │ eventPublisher.publishCategoryCreated(...)
     │
     ▼
┌────────────────────┐
│CategoryEventPublisher│
│ rabbitTemplate     │
│ .convertAndSend()  │
└────┬───────────────┘
     │
     │ Send Message
     ▼
┌────────────────────┐
│   RabbitMQ Queue   │
│  category.queue    │
│                    │
│ Message:           │
│ {                  │
│   id: 1,           │
│   name: "Laptop",  │
│   eventType:       │
│     "CREATED",     │
│   status: "ACTIVE" │
│ }                  │
└────┬───────────────┘
     │
     │ Auto Consumed
     ▼
┌──────────────────────┐
│CategoryEventListener │
│ @RabbitListener      │
│                      │
│ handleCategoryEvent()│
└────┬─────────────────┘
     │
     ▼
┌──────────────────┐
│  EmailService    │
│                  │
│ sendCategoryCreatedEmail("Laptop")
│                  │
│ ✉️  Email sent    │
│ to admin         │
└──────────────────┘

Result:
✅ Category saved to DB
✅ Cache cleared
✅ Message published to RabbitMQ
✅ Email sent to admin
```

---

### Scenario 2: Sửa Status Category (ACTIVE → INACTIVE)

```
Step 1: Client cập nhật category
┌──────────┐
│  Client  │ PUT /api/categories/1 {"name": "Laptop", "status": "INACTIVE"}
└────┬─────┘
     │
     ▼
┌────────────────┐
│CategoryController│ update(id, ...)
└────┬────────────┘
     │
     ▼
┌─────────────────────┐
│ CategoryService     │
│                     │
│ 1. Find category    │
│ 2. Check status     │
│    changed?         │
│    ACTIVE→INACTIVE? │
│ 3. Update DB        │──────────┐
│ 4. Clear Cache      │          │
│ 5. Publish Message  │          ▼
└────┬────────────────┘    ┌───────────┐
     │                     │   MySQL   │
     │                     │  UPDATE   │
     │                     │ categories│
     │                     │ SET status│
     │                     │ = INACTIVE│
     │                     └───────────┘
     │
     │ eventPublisher.publishCategoryStatusChanged(...)
     │
     ▼
┌────────────────────┐
│CategoryEventPublisher│
│ rabbitTemplate     │
│ .convertAndSend()  │
└────┬───────────────┘
     │
     │ Send Message
     ▼
┌────────────────────┐
│   RabbitMQ Queue   │
│  category.queue    │
│                    │
│ Message:           │
│ {                  │
│   id: 1,           │
│   name: "Laptop",  │
│   eventType:       │
│     "STATUS_       │
│      CHANGED",     │
│   oldStatus:       │
│     "ACTIVE",      │
│   newStatus:       │
│     "INACTIVE"     │
│ }                  │
└────┬───────────────┘
     │
     │ Auto Consumed
     ▼
┌──────────────────────────┐
│ CategoryEventListener    │
│ @RabbitListener          │
│                          │
│ handleCategoryEvent()    │
│                          │
│ 1. Check eventType       │
│ 2. If STATUS_CHANGED     │
│    and ACTIVE→INACTIVE   │
│    then update products  │
└────┬────────┬────────────┘
     │        │
     │        │ updateProductsStatus(categoryId)
     │        │
     │        ▼
     │  ┌─────────────────────┐
     │  │ ProductRepository   │
     │  │ findAllByCategoryId │
     │  │                     │
     │  │ For each product:   │
     │  │   if ACTIVE         │
     │  │     set INACTIVE    │
     │  │                     │
     │  │ saveAll()           │
     │  └──────┬──────────────┘
     │         │
     │         ▼
     │   ┌───────────┐
     │   │   MySQL   │
     │   │  UPDATE   │
     │   │ products  │
     │   │ SET status│
     │   │ = INACTIVE│
     │   │ WHERE     │
     │   │ category  │
     │   │  _id = 1  │
     │   └───────────┘
     │
     │ sendCategoryStatusChangedEmail(...)
     │
     ▼
┌──────────────────┐
│  EmailService    │
│                  │
│ ✉️  Email sent    │
│ to admin:        │
│ "Category Laptop │
│  ACTIVE→INACTIVE │
│  3 products      │
│  affected"       │
└──────────────────┘

Result:
✅ Category status updated to INACTIVE
✅ All products of category updated to INACTIVE
✅ Cache cleared (both categories & products)
✅ Email sent to admin with details
```

---

## 🔍 Redis Cache Flow

### Cache Hit (Có trong cache)

```
Client Request
     │
     ▼
┌──────────────┐
│ Controller   │
└──────┬───────┘
       │ search(q="laptop")
       ▼
┌──────────────────┐
│ ProductService   │
│ @Cacheable       │
└──────┬───────────┘
       │
       │ Check Cache First
       ▼
┌──────────────────┐
│ Redis Cache      │
│                  │
│ Key:             │
│ "products::      │
│  laptop_null_    │
│  null_ACTIVE_    │
│  null_0_10"      │
│                  │
│ ✅ FOUND!         │
└──────┬───────────┘
       │
       │ Return cached data
       ▼
┌──────────────┐
│ Controller   │  Return to client (FAST! ~5ms)
└──────────────┘
```

### Cache Miss (Không có trong cache)

```
Client Request
     │
     ▼
┌──────────────┐
│ Controller   │
└──────┬───────┘
       │ search(q="laptop")
       ▼
┌──────────────────┐
│ ProductService   │
│ @Cacheable       │
└──────┬───────────┘
       │
       │ Check Cache First
       ▼
┌──────────────────┐
│ Redis Cache      │
│                  │
│ ❌ NOT FOUND!     │
└──────┬───────────┘
       │
       │ Execute method
       ▼
┌──────────────────┐
│ Database Query   │
│ SELECT * FROM    │
│ products WHERE   │
│ name LIKE        │
│ '%laptop%'       │
└──────┬───────────┘
       │
       │ Results
       ▼
┌──────────────────┐
│ ProductService   │
│ Save to Cache    │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Redis Cache      │
│ SET key=         │
│ "products::..."  │
│ value=results    │
│ TTL=600s         │
└──────────────────┘
       │
       │ Return results
       ▼
┌──────────────┐
│ Controller   │  Return to client (SLOW first time ~100ms)
└──────────────┘                      (FAST next time ~5ms)
```

### Cache Eviction (Xóa cache)

```
Client Request: Create/Update/Delete Product
     │
     ▼
┌──────────────┐
│ Controller   │
└──────┬───────┘
       │ create(...)
       ▼
┌──────────────────┐
│ ProductService   │
│ @CacheEvict      │
│ (allEntries=true)│
└──────┬───────────┘
       │
       │ 1. Save to DB
       ▼
┌──────────────┐
│   MySQL      │
└──────────────┘
       │
       │ 2. Clear ALL cache in "products"
       ▼
┌──────────────────┐
│ Redis Cache      │
│                  │
│ DELETE all keys  │
│ matching:        │
│ "products::*"    │
│                  │
│ ✅ Cache cleared  │
└──────────────────┘

Next search request will query DB and rebuild cache
```

---

## 📨 RabbitMQ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      RabbitMQ Broker                        │
│                                                             │
│  ┌────────────────────────────────────────────────┐        │
│  │          Exchange: category.exchange           │        │
│  │              Type: TOPIC                        │        │
│  │                                                  │        │
│  │  Routing Key Pattern: "category.*"             │        │
│  └────────────┬───────────────┬──────────────────┘        │
│               │               │                            │
│               │               │                            │
│  Routing Key: │               │ Routing Key:              │
│  "category.   │               │ "category.                │
│   created"    │               │  status.changed"          │
│               │               │                            │
│               ▼               ▼                            │
│  ┌────────────────────────────────────────────┐            │
│  │      Queue: category.queue                 │            │
│  │      (Durable: true)                       │            │
│  │                                             │            │
│  │  Messages:                                 │            │
│  │  [1] {id:1, name:"Laptop", type:"CREATED"} │            │
│  │  [2] {id:2, name:"Phone", type:"STATUS_..."}│           │
│  │  [3] ...                                   │            │
│  └────────────────────┬───────────────────────┘            │
│                       │                                    │
└───────────────────────┼────────────────────────────────────┘
                        │
                        │ Consume (Auto-acknowledge)
                        ▼
              ┌─────────────────────┐
              │CategoryEventListener│
              │  @RabbitListener    │
              │                     │
              │ Process message:    │
              │ - Send email        │
              │ - Update products   │
              └─────────────────────┘
```

---

## 🎯 Annotations Explained

### @Cacheable

```java
@Cacheable(value = "products", key = "#id")
public ProductResponse get(Long id) {
    // Chỉ chạy nếu cache MISS
    return productRepository.findById(id);
}

// Redis sẽ lưu:
// Key: products::123
// Value: {id: 123, name: "Laptop", ...}
```

### @CacheEvict

```java
@CacheEvict(value = "products", allEntries = true)
public void create(ProductCreateRequest req) {
    // Sau khi lưu DB, xóa TẤT CẢ cache "products"
    productRepository.save(...);
}

// Redis sẽ xóa:
// products::*  (tất cả keys bắt đầu với "products::")
```

### @RabbitListener

```java
@RabbitListener(queues = "category.queue")
public void handleEvent(CategoryEventMessage msg) {
    // Tự động lấy message từ queue và xử lý
    // Nếu method chạy thành công → message bị xóa khỏi queue
    // Nếu exception → message có thể retry hoặc vào DLQ
}
```

---

## 📈 Performance Comparison

### Without Cache (Trực tiếp query DB)

```
Request 1: ──┐
Request 2:   ├─→ All hit Database (100ms each)
Request 3:   │
Request 4: ──┘
Total: 400ms for 4 requests
```

### With Redis Cache

```
Request 1: ──→ Database (100ms) ──→ Cache to Redis
Request 2: ──→ Redis (5ms)
Request 3: ──→ Redis (5ms)
Request 4: ──→ Redis (5ms)
Total: 115ms for 4 requests (71% faster!)
```

---

## 🔐 Security Notes

### Redis

- **Production:** Thêm password cho Redis
- **Production:** Không expose port 6379 ra ngoài

### RabbitMQ

- **Production:** Đổi username/password mặc định
- **Production:** Cấu hình SSL/TLS

### Email

- **KHÔNG BAO GIỜ** commit password vào Git
- Dùng Environment Variables hoặc Secret Manager

---

Chúc bạn học tốt! 🚀
