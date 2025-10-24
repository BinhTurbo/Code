# 🧪 Hướng dẫn Test Cache Backend

## Hiện trạng:

- ❌ Method `search()` (pagination/list) **KHÔNG được cache**
- ✅ Method `get(id)` **CÓ cache** với Redis

## Test 1: Verify `search()` không có cache (hiện tại)

### Bước 1: Mở Redis CLI

```bash
# Trong terminal
docker exec -it redis redis-cli

# Xem tất cả keys hiện có
127.0.0.1:6379> KEYS *
```

### Bước 2: Gọi API search categories

```bash
# Gọi page 0
GET http://localhost:8081/api/categories?page=0&size=5

# Gọi page 1
GET http://localhost:8081/api/categories?page=1&size=5

# Quay lại page 0
GET http://localhost:8081/api/categories?page=0&size=5
```

### Bước 3: Kiểm tra log backend

```
➡️ Mỗi lần gọi API đều thấy SQL query
➡️ Không có cache hit
```

### Bước 4: Kiểm tra Redis

```bash
127.0.0.1:6379> KEYS categories::*
(empty array)  # ❌ Không có cache nào cho search
```

---

## Test 2: Verify `get(id)` CÓ cache

### Bước 1: Gọi API get category by ID

```bash
# Lần 1: Gọi category ID 1
GET http://localhost:8081/api/categories/1
```

### Bước 2: Xem log backend

```
SELECT ... FROM categories WHERE id = 1  # ✅ Query database
```

### Bước 3: Kiểm tra Redis

```bash
127.0.0.1:6379> KEYS categories::*
1) "categories::1"  # ✅ Đã có cache!

# Xem nội dung cache
127.0.0.1:6379> GET categories::1
# Sẽ thấy JSON của category
```

### Bước 4: Gọi lại API lần 2

```bash
GET http://localhost:8081/api/categories/1
```

### Bước 5: Xem log backend

```
# ✅ KHÔNG thấy SQL query!
# ✅ Data được lấy từ Redis cache
```

---

## 📊 So sánh Performance

### Search (không cache):

```
Request 1 → Database query → 50-100ms
Request 2 (same params) → Database query lại → 50-100ms  ❌
Request 3 → Database query lại → 50-100ms  ❌
```

### Get by ID (có cache):

```
Request 1 → Database query → 50ms
Request 2 (same ID) → Redis cache → 1-5ms  ✅ Nhanh gấp 10-50 lần!
Request 3 → Redis cache → 1-5ms  ✅
```

---

## 🎯 Kết luận

**Hiện tại backend của bạn:**

- ✅ Đã cấu hình Redis đúng
- ✅ Đã enable caching cho `get(id)`
- ❌ CHƯA cache cho `search()`/pagination

**Đó là lý do bạn thấy:**

- Mỗi lần chuyển trang vẫn query database
- Logs đầy SQL queries
- Không thấy sự khác biệt về performance

**Để có backend cache cho pagination, cần:**

1. Implement custom cache logic cho Page<T>
2. Hoặc cache DTO list thay vì Page object
3. Hoặc dùng frontend cache (như đã implement)

---

## 💡 Recommendation

Vì cache pagination phức tạp và có nhiều biến số (page, size, sort, filters),
**nên kết hợp:**

1. **Backend cache:** Chỉ cache `get by ID` (đã có) ✅
2. **Frontend cache:** Cache pagination results trong 3-5 phút ⭐

Đây là best practice trong industry!
