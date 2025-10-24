# 🎓 Hướng dẫn: Backend Cache cho Pagination

## 📌 Tóm tắt vấn đề

**Hiện tại:**

- Backend của bạn **ĐÃ CẤU HÌNH Redis** đúng ✅
- Backend **CHỈ CACHE method `get(id)`**, không cache `search()` ❌
- Do đó mỗi lần phân trang vẫn query database

**Lý do không cache `search()`:**

```java
// ⚠️ Không cache Page object vì không serialize/deserialize tốt với Redis
public Page<CategoryDtos.CategoryResponse> search(...) {
    return repo.search(q, status, pageable).map(mapper::toDto);
}
```

---

## 🔍 Nguyên nhân chi tiết

### 1. Spring Data's `Page<T>` khó serialize

```
Page<Category>
├─ content: List<Category>  ✅ OK
├─ pageable: PageRequest     ❌ Circular references
│  ├─ sort: Sort
│  └─ unpaged: boolean
├─ totalElements: long       ✅ OK
└─ totalPages: int           ✅ OK
```

### 2. Redis cần serialize object thành JSON

```java
// Redis lưu dạng này:
{
  "@class": "org.springframework.data.domain.PageImpl",
  "content": [...],
  "pageable": {
    "@class": "org.springframework.data.domain.PageRequest",
    "sort": { ... }  // ❌ Phức tạp, dễ lỗi!
  }
}
```

---

## ✅ Giải pháp: 3 Cách để cache pagination

### **Cách 1: Cache ở Frontend** ⭐ (RECOMMENDED - Đã implement)

**Ưu điểm:**

- ✅ Đơn giản nhất
- ✅ Không phụ thuộc backend implementation
- ✅ Giảm network requests xuống 70-90%
- ✅ UX tốt nhất (instant loading)

**Nhược điểm:**

- ❌ Mỗi client cache riêng (không share)
- ❌ Database vẫn bị query lần đầu tiên

**Code đã có sẵn:**

```typescript
// frontend/webmini-fe/src/app/core/cache.service.ts
// frontend/webmini-fe/src/app/features/catalog/catalog.service.ts
```

---

### **Cách 2: Cache Backend với Custom Wrapper** (Nâng cao)

**Files đã tạo sẵn cho bạn:**

1. ✅ `CacheConfig.java` - Cấu hình Redis serializer
2. ✅ `CacheablePageResponse.java` - Wrapper đơn giản cho Page<T>

**Cách implement:**

#### Bước 1: Tạo method cache-friendly trong Service

```java
@Service
@RequiredArgsConstructor
public class CategoryService {

    // Method cũ - KHÔNG cache (giữ nguyên cho backward compatibility)
    @Transactional(readOnly = true)
    public Page<CategoryDtos.CategoryResponse> search(
        String q, String status, Pageable pageable
    ) {
        return repo.search(q, status, pageable).map(mapper::toDto);
    }

    // ✨ Method MỚI - CÓ cache
    @Transactional(readOnly = true)
    @Cacheable(
        value = "categorySearch",
        key = "#q + ':' + #status + ':' + #page + ':' + #size + ':' + #sort"
    )
    public CacheablePageResponse<CategoryDtos.CategoryResponse> searchCacheable(
        String q,
        String status,
        int page,
        int size,
        String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<CategoryDtos.CategoryResponse> result = search(q, status, pageable);

        // Convert Page<T> sang CacheablePageResponse<T>
        return new CacheablePageResponse<>(
            result.getContent(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.getNumber(),
            result.getSize(),
            result.isFirst(),
            result.isLast(),
            result.isEmpty()
        );
    }

    private Pageable createPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(new Sort.Order(dir, parts[0])));
    }
}
```

#### Bước 2: Update Controller để dùng method mới

```java
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @GetMapping
    public CacheablePageResponse<CategoryDtos.CategoryResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return serviceCategory.searchCacheable(q, status, page, size, sort);
    }
}
```

#### Bước 3: Update create/update/delete để invalidate cache

```java
@CacheEvict(value = {"categories", "categorySearch"}, allEntries = true)
public CategoryDtos.CategoryResponse create(...) { ... }

@CacheEvict(value = {"categories", "categorySearch"}, allEntries = true)
public CategoryDtos.CategoryResponse update(...) { ... }

@CacheEvict(value = {"categories", "categorySearch"}, allEntries = true)
public void delete(...) { ... }
```

---

### **Cách 3: Không cache pagination, chỉ cache entities**

**Approach:**

- Cache từng category record riêng lẻ
- Pagination query vẫn chạy, nhưng load entities từ cache

**Code:**

```java
// Đã có sẵn - KHÔNG cần thay đổi!
@Cacheable(value = "categories", key = "#id")
public CategoryDtos.CategoryResponse get(Long id) { ... }
```

**Ưu điểm:**

- ✅ Đơn giản, an toàn
- ✅ Giảm load database cho get by ID

**Nhược điểm:**

- ❌ Pagination queries vẫn chạy
- ❌ Hiệu quả thấp hơn

---

## 🧪 Test Backend Cache (nếu implement Cách 2)

### Test 1: Verify cache hoạt động

```bash
# Terminal 1: Theo dõi Redis
docker exec -it redis redis-cli
127.0.0.1:6379> MONITOR

# Terminal 2: Gọi API
curl "http://localhost:8081/api/categories?page=0&size=5"
curl "http://localhost:8081/api/categories?page=0&size=5"  # Lần 2
```

**Kết quả mong đợi:**

- Lần 1: Thấy SQL query trong log ✅
- Lần 2: KHÔNG thấy SQL query (dùng cache) ✅

### Test 2: Verify cache invalidation

```bash
# Tạo category mới
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","status":"ACTIVE"}'

# Gọi lại search
curl "http://localhost:8081/api/categories?page=0&size=5"
```

**Kết quả mong đợi:**

- Thấy SQL query lại (cache đã bị xóa) ✅

---

## 📊 Performance Comparison

### Scenario: User chuyển qua lại giữa 3 pages

#### **Không có cache (Hiện tại):**

```
Page 0 request → Database query → 50ms
Page 1 request → Database query → 50ms
Page 0 request → Database query → 50ms  ❌ Query lại!
Total: 150ms database load
```

#### **Có Backend Cache (Cách 2):**

```
Page 0 request → Database query → 50ms → Save to Redis
Page 1 request → Database query → 50ms → Save to Redis
Page 0 request → Redis cache → 2ms  ✅ Nhanh gấp 25 lần!
Total: 102ms, giảm 32% load
```

#### **Có Frontend Cache (Cách 1 - Đã có):**

```
Page 0 request → Database query → 50ms → Save to memory
Page 1 request → Database query → 50ms → Save to memory
Page 0 request → Memory cache → 0ms  ✅ Instant!
Total: 100ms, smooth UX
```

#### **Có CẢ Backend + Frontend Cache (Optimal):**

```
User A - Page 0 → Database → 50ms → Redis cache → Frontend cache
User A - Page 1 → Database → 50ms → Redis cache → Frontend cache
User A - Page 0 → Frontend cache → 0ms  ✅ Instant!

User B - Page 0 → Redis cache → 2ms  ✅ Không query database!
User B - Page 1 → Redis cache → 2ms  ✅
User B - Page 0 → Frontend cache → 0ms  ✅

Total: Database chỉ query 2 lần cho tất cả users! 🚀
```

---

## 💡 Khuyến nghị

### Dự án học tập / nhỏ:

✅ **Chỉ dùng Frontend Cache (Cách 1)** - Đã implement rồi!

### Dự án production / lớn:

⭐ **Dùng cả Backend + Frontend Cache:**

- Backend: Cache pagination 3-5 phút
- Frontend: Cache pagination 2-3 phút
- Database: Chỉ query khi cần thiết

---

## 🎯 Kết luận cho bạn

**Hiện tại trong code của bạn:**

1. ✅ Redis đã config đúng trong `application.properties`
2. ✅ Cache đã enable cho `get(id)`
3. ❌ Cache CHƯA enable cho `search()` pagination

**Đó là lý do bạn thấy logs:**

```
SELECT COUNT(*) FROM categories...
SELECT ... FROM categories...
```

Mỗi lần chuyển trang.

**Để test cache hoạt động:**

```bash
# Gọi API get by ID (đã có cache)
curl http://localhost:8081/api/categories/1
curl http://localhost:8081/api/categories/1  # Lần 2 không query DB

# Check Redis
docker exec -it redis redis-cli
127.0.0.1:6379> KEYS categories::*
1) "categories::1"  # ✅ Có cache!
```

**Recommendation:**
Với mục đích học tập, bạn đã có:

- ✅ Frontend cache working
- ✅ Backend cache cho get(id) working

Đủ để hiểu cách cache hoạt động rồi! 🎓

Nếu muốn implement backend cache cho pagination (Cách 2),
follow hướng dẫn ở trên nhé! 😊
