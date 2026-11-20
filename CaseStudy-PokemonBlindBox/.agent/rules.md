### 1. Quy tắc Database Schema & Chuẩn hóa Dữ liệu

- **Chống trùng Số thứ tự thẻ trong cùng một Bộ (Unique Card Number):**
  - Bảng `CardTemplate` bắt buộc khai báo ràng buộc duy nhất kết hợp giữa Bộ sưu tập (`set_id`) và Số thứ tự thẻ (`card_number`):
    ```java
    @Table(name = "card_templates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_card_set_number", columnNames = {"set_id", "card_number"})
    })
    ```
  - Ở tầng Service, khi Admin thêm hoặc cập nhật thẻ bài, phải kiểm tra `existsByCardSetIdAndCardNumber(...)` và ném ra ngoại lệ HTTP `409 CONFLICT` rõ ràng trước khi chạm tới lỗi Database.
- **Kiểu dữ liệu Số nguyên cho Số thứ tự thẻ & Format DTO:**
  - Trường `cardNumber` trong `CardTemplate` và `baseSetTotal` trong `CardSet` BẮT BUỘC lưu kiểu `Integer` (ví dụ: `52`, `98`, `141` và `103`) để phục vụ truy vấn `ORDER BY cardNumber ASC` chuẩn xác và phân loại thẻ chuẩn (`cardNumber <= baseSetTotal`) với thẻ Secret vượt khung (`cardNumber > baseSetTotal`).
  - Tuyệt đối không lưu chuỗi `"098 / 103"` vào Database. Việc định dạng hiển thị thực hiện ở tầng DTO / Mapper bằng `String.format("%03d / %03d", cardNumber, baseSetTotal)`.
- **Đóng băng cấu hình Pack/Box (Configuration Snapshot):**
  - Entity `BoosterPack` bắt buộc có trường `snapshotCardsPerPack` (`Integer`, `nullable = false`).
  - Ngay tại thời điểm khởi tạo `BoosterBox` và sinh ra các `BoosterPack` con, phải đọc giá trị `cardsPerPack` hiện tại của `CardSet` và gán cứng vào `snapshotCardsPerPack`.
  - Khi người chơi mở Pack (`openPack`), thuật toán Gacha chỉ được phép đọc số lượng thẻ cần rút từ `BoosterPack.getSnapshotCardsPerPack()`, tuyệt đối không đọc ngược lại từ `CardSet.getCardsPerPack()`.

### 2. Quy tắc Đồng thời & Khóa Lạc quan (`@Version` - Optimistic Locking)

- **Các Entity bắt buộc gắn `@Version`:**
  Khai báo `@Version private Long version;` trên 4 Entity nhạy cảm:
  1. `BoosterBox`: Quản lý trạng thái hộp và số lượng Pack còn lại.
  2. `BoosterPack`: Quản lý trạng thái từng gói lẻ (`AVAILABLE`, `SOLD`, `OPENED`) tại từng vị trí `positionInBox`.
  3. `Wallet`: Quản lý số dư ví nội bộ của người chơi khi nạp điểm VNPay, mua Pack/Box hoặc đặt giá đấu giá.
  4. `Auction`: Quản lý giá cao nhất hiện tại (`currentHighestBid`) và người dẫn đầu phiên đấu giá.
- **Nghiệp vụ Khóa Box tự động & Bắt lỗi tranh chấp (Race Condition):**
  - Trong cùng một `@Transactional` khi hàm `buySinglePack(Long boxId, Integer positionInBox)` được gọi:
    1. Kiểm tra `BoosterPack` ở trạng thái `AVAILABLE` và trừ tiền trong `Wallet`.
    2. Chuyển trạng thái `BoosterPack` sang `SOLD`.
    3. **BẮT BUỘC** cập nhật ngay lập tức `boosterBox.setCanBuyWholeBox(false)` và giảm `remainingPacks` đi 1.
  - Hàm `buyWholeBox(Long boxId)` chỉ được phép thực thi khi `boosterBox.isCanBuyWholeBox() == true` và `remainingPacks.equals(packsPerBox)`.
  - Tại `@RestControllerAdvice` (`GlobalExceptionHandler`), bắt buộc có hàm `@ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})` trả về HTTP `409 CONFLICT` với thông báo: _"Vật phẩm hoặc mức giá vừa được người chơi khác cập nhật. Vui lòng thao tác lại!"_.

### 3. Quy tắc Tối ưu hóa với Redis Cache (`@Cacheable` & `@CacheEvict`)

- **Cấu hình Serializer chuẩn cho Spring Boot 3:**
  - Không sử dụng JDK Serialization mặc định. Cấu hình `RedisCacheConfiguration` sử dụng `StringRedisSerializer` cho Key và `GenericJackson2JsonRedisSerializer` (hỗ trợ `JavaTimeModule`) cho Value.
  - Định nghĩa rõ 3 vùng nhớ Cache: `card_sets` (TTL: 24h), `rarities` (TTL: 24h), `card_templates` (TTL: 12h).
- **Quy tắc đặt Annotation Caching:**
  - Dùng `@Cacheable(value = "card_sets", key = "#setId")` khi lấy thông tin bộ thẻ.
  - Dùng `@Cacheable(value = "rarities", key = "'all_weights'")` cho danh sách độ hiếm và trọng số `dropWeight` phục vụ thuật toán quay Gacha.
  - Dùng `@Cacheable(value = "card_templates", key = "#setId + '_' + #rarityId")` cho danh sách khuôn mẫu thẻ theo bộ và độ hiếm để Gacha Engine bốc thẻ trực tiếp từ RAM.
  - Mọi hàm thêm/sửa/xóa `CardSet`, `Rarity`, `CardTemplate` trên Admin Dashboard bắt buộc gắn `@CacheEvict(value = {...}, allEntries = true)`.
  - Tuyệt đối không gọi hàm có gắn `@Cacheable` từ một hàm khác nằm trong cùng một class (Self-invocation).

### 4. Quy tắc Đồng bộ Thời gian thực với WebSocket (`SimpMessagingTemplate`)

- **Cấu hình STOMP Broker:**
  - Kích hoạt `@EnableWebSocketMessageBroker` trong `WebSocketConfig` triển khai `WebSocketMessageBrokerConfigurer`.
  - Endpoint kết nối: `/ws-blindbox` (hỗ trợ `.withSockJS()`). Prefix: `enableSimpleBroker("/topic", "/queue")` và `setApplicationDestinationPrefixes("/app")`.
- **Chuẩn hóa Topic & Sử dụng `SimpMessagingTemplate`:**
  Sử dụng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` kết hợp `SimpMessagingTemplate` để đảm bảo Database đã commit thành công mới bắn sự kiện WebSocket:
  1. **Kênh cập nhật khay Booster Box (`/topic/boxes/{boxId}`):**
     Gửi `BoxStateUpdateEventDTO` chứa `boxId`, `soldPackPosition`, `remainingPacks`, và `canBuyWholeBox: false` ngay khi có Pack lẻ hoặc nguyên Box được bán ra.
  2. **Kênh cập nhật Phiên đấu giá (`/topic/auctions/{auctionId}`):**
     Gửi `AuctionBidUpdateEventDTO` chứa `auctionId`, `newHighestBid`, `maskedBidderName`, `bidTimestamp`, và `endTime` ngay khi có bước giá mới hợp lệ.

### 5. Quy tắc Truy vấn JPA & Chống lỗi N+1 Query

- Mọi liên kết `@ManyToOne` trong `CardTemplate` (tới `CardSet`, `Rarity`, `CardSpecies`) và `UserCard` phải để `fetch = FetchType.LAZY`.
- Khi dùng `JpaSpecificationExecutor` để lọc thẻ bài theo Tên, Số thứ tự, Độ Hiếm, Đặt và Loài, bắt buộc sử dụng `@EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"})` để lấy trọn vẹn dữ liệu trong 1 câu lệnh SQL duy nhất.
