# 📋 Báo Cáo Re-Audit — Lỗi Mới Phát Sinh (Regressions) & Điểm Còn Sót Cần Vá

> **Dự án:** Pokémon Blindbox (Spring Boot 3.3.5 / Java 21 / MySQL 8 / Redis / STOMP WebSocket)
> **Trạng thái biên dịch:** `mvn test-compile` — PASS
> **Điểm tổng quan:** **7.9 / 10** (tăng từ 4.5 / 10 sau 3 Phase Refactor)

---

## Tổng Quan Các Vấn Đề Cần Xử Lý

| Mã | Mức độ | Phân loại | Vị trí file | Tóm tắt ngắn |
| :--- | :---: | :--- | :--- | :--- |
| **REG-01** | 🔴 **P0 - Critical** | Regression | `BoxPurchaseService.java:101-109` & `BoosterBoxRepository.java:34-45` | `buySinglePack` luôn văng HTTP 409 (`ObjectOptimisticLockingFailureException`) kể cả khi chỉ có 1 user mua. |
| **REG-02** | 🟠 **P1 - High** | Regression | `WebViewController.java:230` & `vnpay-return.html:49` | Lệch tên biến Model (`amountVnd` vs `displayAmount`) khiến trang kết quả VNPay lỗi/hiện `null ₫`. |
| **GAP-01** | 🟠 **P1 - High** | Transaction Isolation | `AuctionSettlementService.java:40-59` | Chung 1 `@Transactional` cho cả batch 20 phiên đấu giá -> 1 phiên lỗi làm rollback toàn bộ 19 phiên còn lại. |
| **GAP-02** | 🟠 **P1 - High** | Deadlock Risk | `AuctionService.java:98-168` vs `AuctionSettlementService.java:43,88` | Ngược thứ tự khóa giữa `placeBid` (`Wallet -> Auction`) và `settleSingleAuction` (`Auction -> Wallet`). |
| **GAP-03** | 🟡 **P2 - Medium** | Multi-Instance / Config | `pom.xml`, `application.yml:5`, `docker-compose.yml` | Thiếu `spring-session-data-redis`, `reactor-netty` (cho STOMP Relay), và cấu hình `SPRING_PROFILES_ACTIVE=prod`. |

---

## Chi Tiết Từng Vấn Đề & Hướng Dẫn Vá Lỗi

### 🔴 REG-01 [P0 - Critical]: `buySinglePack` luôn văng lỗi 409 Conflict (Hibernate Dirty-Check vs Atomic Update)

- **Vị trí:**
  - `src/main/java/com/pokemon/blindbox/service/BoxPurchaseService.java` (dòng 101–109)
  - `src/main/java/com/pokemon/blindbox/repository/BoosterBoxRepository.java` (dòng 34–45)
- **Nguyên nhân gốc rễ:**
  1. `boosterPackRepository.findByBoosterBoxIdAndPositionInBox(...)` tải sẵn `BoosterBox` ở trạng thái **managed entity** với `version = V` (VD: `0`).
  2. Dòng 101 gọi `boosterBoxRepository.decrementRemainingPackAtomically(box.getId())` với `@Modifying(clearAutomatically = false)` -> câu SQL tăng `b.version = b.version + 1` trong MySQL (`version` thành `1`), nhưng object `box` trong Hibernate Persistence Context vẫn giữ `version = 0`.
  3. Ngay sau đó, dòng 107–109 lại gọi setter lên chính managed entity `box`:
     ```java
     box.setCanBuyWholeBox(false);
     box.setRemainingPacks(updatedRemaining);
     box.setStatus(updatedStatus);
     ```
  4. Khi transaction commit, Hibernate dirty-check phát hiện `box` bị thay đổi và tự động bắn thêm câu `UPDATE booster_boxes SET ... WHERE id = ? AND version = 0` -> 0 dòng bị ảnh hưởng -> **ném `ObjectOptimisticLockingFailureException` (HTTP 409) cho mọi lượt mua gói lẻ!**
- **Cách vá đề xuất:**
  Xóa hoàn toàn 3 dòng `box.set...` (dòng 107–109) trong `BoxPurchaseService.buySinglePack`. Chỉ sử dụng biến cục bộ `updatedRemaining` và `updatedStatus` để trả về Event và `BoxPurchaseResponseDTO`:
  ```java
  int rowsUpdated = boosterBoxRepository.decrementRemainingPackAtomically(box.getId());
  if (rowsUpdated == 0) {
      throw new BusinessValidationException("Hộp bài này đã bán hết toàn bộ gói bài.");
  }
  int updatedRemaining = Math.max(0, box.getRemainingPacks() - 1);
  BoxStatus updatedStatus = updatedRemaining == 0 ? BoxStatus.SOLD_OUT : BoxStatus.PARTIALLY_SOLD;
  // KHÔNG gọi box.setCanBuyWholeBox / setRemainingPacks / setStatus trên managed entity!
  ```

---

### 🟠 REG-02 [P1 - High]: Lệch tên biến Model trên trang biên lai VNPay (`vnpay-return.html`)

- **Vị trí:**
  - `src/main/java/com/pokemon/blindbox/controller/web/WebViewController.java` (dòng 228–230)
  - `src/main/resources/templates/wallet/vnpay-return.html` (dòng 49)
- **Nguyên nhân gốc rễ:**
  `WebViewController.vnpayReturnPage` truyền attribute tên `"amountVnd"`, nhưng template `vnpay-return.html` dòng 49 vẫn tham chiếu `${displayAmount}`:
  ```html
  th:text="${#numbers.formatDecimal(displayAmount, 0, 'POINT', 0, 'COMMA')} + ' ₫'"
  ```
  Khi `displayAmount` bị `null`, Thymeleaf render sai hoặc lỗi hiển thị số tiền nạp.
- **Cách vá đề xuất:**
  Cập nhật `WebViewController.java` dòng 230:
  ```java
  model.addAttribute("displayAmount", amountVnd);
  ```

---

### 🟠 GAP-01 [P1 - High]: Rollback dây chuyền trong `AuctionSettlementService`

- **Vị trí:**
  - `src/main/java/com/pokemon/blindbox/service/AuctionSettlementService.java` (dòng 40–59)
- **Nguyên nhân gốc rễ:**
  Method `settleExpiredAuctions()` gắn `@Transactional` bao trùm cả vòng lặp `for (Long auctionId : expiredIds)` và dùng `try/catch` bên trong. Trong Spring/JPA, nếu 1 phiên đấu giá gặp ngoại lệ tầng DB/JPA (VD: `OptimisticLockException`, `DataIntegrityViolationException`), transaction hiện tại sẽ bị đánh dấu `rollback-only`. Dù `catch` nuốt exception, khi kết thúc vòng lặp Spring vẫn rollback toàn bộ 19 phiên còn lại và ném `UnexpectedRollbackException`.
- **Cách vá đề xuất:**
  Bỏ `@Transactional` khỏi `settleExpiredAuctions()` và dùng `TransactionTemplate` (với `PROPAGATION_REQUIRES_NEW`) cho từng phiên đấu giá độc lập:
  ```java
  @Scheduled(fixedDelayString = "${app.auction.settlement-delay-ms:10000}")
  public void settleExpiredAuctions() {
      List<Long> expiredIds = auctionRepository.findExpiredActiveAuctionIdsForSettlement(
              AuctionStatus.ACTIVE, LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE)
      );
      for (Long auctionId : expiredIds) {
          try {
              transactionTemplate.executeWithoutResult(status -> settleSingleAuction(auctionId));
          } catch (Exception ex) {
              log.error("[AUCTION-SETTLEMENT] Lỗi khi tất toán phiên đấu giá #{}: {}", auctionId, ex.getMessage(), ex);
          }
      }
  }
  ```
  *(Lưu ý: Nếu dùng `FOR UPDATE SKIP LOCKED` ở query lấy `expiredIds`, chỉ nên lock bên trong `settleSingleAuction` hoặc giữ `findByIdForUpdate` với `SKIP_LOCKED` trong từng transaction con để tránh 2 node quét trùng danh sách ID).*

---

### 🟠 GAP-02 [P1 - High]: Nguy cơ InnoDB Deadlock giữa `placeBid` và `AuctionSettlementService`

- **Vị trí:**
  - `src/main/java/com/pokemon/blindbox/service/AuctionService.java` (dòng 98–168)
  - `src/main/java/com/pokemon/blindbox/service/AuctionSettlementService.java` (dòng 43, 62–95)
- **Nguyên nhân gốc rễ:**
  Khi người chơi đặt giá đúng thời điểm `endTime`:
  - `AuctionService.placeBid`: đọc `Auction` không lock -> khóa **Wallet** (`FOR UPDATE`) -> update **Auction**.
  - `AuctionSettlementService.settleSingleAuction`: khóa **Auction** (`FOR UPDATE`) -> khóa **UserCard** -> khóa **Wallet** (`FOR UPDATE`).
  Hai luồng nắm giữ và chờ khóa theo chiều ngược nhau -> nguy cơ **MySQL InnoDB Deadlock**.
- **Cách vá đề xuất:**
  Chuẩn hóa thứ tự khóa thống nhất toàn hệ thống: **`Auction -> UserCard -> Wallet(minId) -> Wallet(maxId)`**.
  Cụ thể, trong `AuctionRepository`, thêm method `findByIdForUpdate(@Param("id") Long id)` gắn `@Lock(LockModeType.PESSIMISTIC_WRITE)` và gọi ngay đầu `AuctionService.placeBid(...)`.

---

### 🟡 GAP-03 [P2 - Medium]: Hoàn thiện cấu hình Scale Multi-Instance & Production

1. **Chia sẻ HTTP Session đa node (`pom.xml`):**
   - Hiện `pom.xml` chưa có `spring-session-data-redis` -> khi scale >= 2 instance cho 10.000 CCU, `JSESSIONID` lưu trên RAM từng node sẽ bị đăng xuất liên tục nếu không có sticky session.
   - *Khuyến nghị:* Bổ sung dependency `org.springframework.session:spring-session-data-redis`.
2. **External STOMP Broker Relay (`pom.xml`):**
   - `WebSocketConfig` đã hỗ trợ toggle `app.websocket.broker-relay.enabled=true`, nhưng Spring STOMP Broker Relay yêu cầu thư viện TCP client (`io.projectreactor.netty:reactor-netty`). Nếu bật cờ này mà chưa thêm dependency trong `pom.xml`, ứng dụng sẽ báo lỗi `ClassNotFoundException` lúc khởi động.
3. **Mặc định Profile Production (`application.yml:5`, `docker-compose.yml`):**
   - `application.yml` đang đặt `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`.
   - Cần khai báo tường minh `SPRING_PROFILES_ACTIVE: prod` trong cấu hình Production / `docker-compose.yml` để chắc chắn vô hiệu hóa `DataInitializer`, `FlywayRepairConfig` và `DevSimulatePaymentController`.
