package com.pokemon.blindbox.service;

import com.pokemon.blindbox.config.SePayProperties;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.WalletTransaction;
import com.pokemon.blindbox.entity.enums.TransactionStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.WalletRepository;
import com.pokemon.blindbox.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service nạp Xu tự động 24/7 về tài khoản ngân hàng cá nhân qua VietQR + SePay Webhook:
 * - Tỷ lệ quy đổi đồng bộ với toàn hệ thống: 1.000 ₫ = 10 Xu (100 ₫ = 1 Xu).
 * - Xác thực Webhook bằng Authorization: Apikey <SEPAY_WEBHOOK_API_KEY> (constant-time).
 * - Khóa bi quan PESSIMISTIC_WRITE chống cộng trùng khi SePay retry webhook.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SePayDepositService {

    private static final BigDecimal MIN_DEPOSIT_VND = new BigDecimal("10000");
    private static final BigDecimal MAX_DEPOSIT_VND = new BigDecimal("100000000");
    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SePayProperties sePayProperties;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * Khởi tạo yêu cầu nạp VietQR (PENDING) và trả về link ảnh QR + thông tin chuyển khoản.
     */
    @Transactional
    public Map<String, Object> createVietQrOrder(Long userId, BigDecimal amountVnd) {
        if (amountVnd == null || amountVnd.compareTo(MIN_DEPOSIT_VND) < 0) {
            throw new BusinessValidationException("Số tiền nạp tối thiểu là 10.000 ₫.");
        }
        if (amountVnd.compareTo(MAX_DEPOSIT_VND) > 0) {
            throw new BusinessValidationException("Số tiền nạp tối đa mỗi lần là 100.000.000 ₫.");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .lockedBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .build()));

        BigDecimal normalizedVnd = amountVnd.setScale(0, RoundingMode.HALF_UP);
        // Quy đổi VNĐ -> Xu theo tỷ lệ 1.000 ₫ = 100 Xu (10 ₫ = 1 Xu)
        BigDecimal creditedXu = normalizedVnd.divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        String transferCode = generateUniqueTransferCode(userId);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .vnpTxnRef(transferCode)
                .type(TransactionType.DEPOSIT_VIETQR)
                .status(TransactionStatus.PENDING)
                .amount(creditedXu)
                .description(String.format("Nạp %,.0f ₫ (+%,.0f Xu) qua VietQR #%s", normalizedVnd, creditedXu, transferCode))
                .build();
        walletTransactionRepository.save(tx);

        String bankCode = safeTrim(sePayProperties.getBankCode(), "MB");
        String accountNumber = safeTrim(sePayProperties.getAccountNumber(), "");
        String accountName = safeTrim(sePayProperties.getAccountName(), "");
        String template = safeTrim(sePayProperties.getQrTemplate(), "compact");
        boolean configured = !accountNumber.isEmpty();

        String qrImageUrl = configured
                ? String.format(
                        "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s&template=%s",
                        urlEncode(accountNumber),
                        urlEncode(bankCode),
                        normalizedVnd.toPlainString(),
                        urlEncode(transferCode),
                        urlEncode(template)
                )
                : "";

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("transferCode", transferCode);
        resp.put("amountVnd", normalizedVnd);
        resp.put("creditedXu", creditedXu);
        resp.put("bankCode", bankCode);
        resp.put("accountNumber", accountNumber);
        resp.put("accountName", accountName);
        resp.put("qrImageUrl", qrImageUrl);
        resp.put("configured", configured);
        resp.put("status", TransactionStatus.PENDING.name());
        return resp;
    }

    /**
     * Kiểm tra trạng thái đơn nạp VietQR (dùng cho polling mỗi 3 giây trên trang /wallet).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatus(Long userId, String transferCode) {
        WalletTransaction tx = walletTransactionRepository.findByVnpTxnRef(transferCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giao dịch " + transferCode));
        if (!tx.getWallet().getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không có quyền xem giao dịch này!");
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("transferCode", transferCode);
        resp.put("status", tx.getStatus().name());
        resp.put("creditedXu", tx.getAmount());
        resp.put("walletBalance", tx.getWallet().getBalance());
        return resp;
    }

    /**
     * Xử lý Webhook tự động từ SePay khi có tiền chuyển vào tài khoản ngân hàng cá nhân của bạn.
     * Payload chuẩn SePay: { id, gateway, transactionDate, accountNumber, code, content, transferType, transferAmount, referenceCode, description }
     */
    @Transactional
    public Map<String, Object> handleSePayWebhook(String authorizationHeader, Map<String, Object> payload) {
        String configuredKey = safeTrim(sePayProperties.getWebhookApiKey(), "");
        if (configuredKey.isEmpty()) {
            log.warn("[SEPAY-WEBHOOK] Rejected because SEPAY_WEBHOOK_API_KEY is not configured!");
            throw new SecurityException("SePay Webhook API Key chưa được cấu hình trên máy chủ.");
        }

        if (!isValidApikeyHeader(authorizationHeader, configuredKey)) {
            log.warn("[SEPAY-WEBHOOK] Invalid Authorization header!");
            throw new SecurityException("Invalid SePay API Key");
        }

        if (payload == null) {
            return Map.of("success", false, "message", "Empty payload");
        }

        // Chỉ xử lý giao dịch tiền VÀO ("in")
        String transferType = String.valueOf(payload.getOrDefault("transferType", "")).trim();
        if (!"in".equalsIgnoreCase(transferType)) {
            return Map.of("success", true, "message", "Ignored non-incoming transaction");
        }

        BigDecimal transferAmount = parseBigDecimal(payload.get("transferAmount"));
        if (transferAmount.compareTo(BigDecimal.valueOf(100)) < 0) {
            return Map.of("success", true, "message", "Amount too small");
        }

        String prefix = safeTrim(sePayProperties.getCodePrefix(), "PKM").toUpperCase(Locale.ROOT);
        String transferCode = extractTransferCode(payload, prefix);
        if (transferCode == null) {
            log.info("[SEPAY-WEBHOOK] Incoming transfer {} VND without matching prefix '{}', ignored.", transferAmount, prefix);
            return Map.of("success", true, "message", "No matching deposit code found");
        }

        WalletTransaction tx = walletTransactionRepository.findByVnpTxnRefForUpdate(transferCode).orElse(null);
        if (tx == null) {
            log.warn("[SEPAY-WEBHOOK] Deposit code '{}' not found in wallet_transactions.", transferCode);
            return Map.of("success", true, "message", "Order not found");
        }

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            log.info("[SEPAY-WEBHOOK] Duplicate webhook ignored for code={}", transferCode);
            return Map.of("success", true, "message", "Already credited");
        }

        // Quy đổi theo đúng số tiền thực nhận từ ngân hàng (1.000 ₫ = 100 Xu, tức 10 ₫ = 1 Xu)
        BigDecimal actualXu = transferAmount.divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        Long userId = tx.getWallet().getUserId();

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví người dùng id=" + userId));
        wallet.setBalance(wallet.getBalance().add(actualXu));
        walletRepository.save(wallet);

        String sepayRef = String.valueOf(payload.getOrDefault("referenceCode", payload.getOrDefault("id", "")));
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setAmount(actualXu);
        tx.setDescription(String.format("Nạp %,.0f ₫ (+%,.0f Xu) qua VietQR #%s (SePay: %s)",
                transferAmount, actualXu, transferCode, sepayRef));
        walletTransactionRepository.save(tx);

        log.info("[SEPAY-WEBHOOK] Credited userId={}, vnd={}, xu={}, code={}, newBalance={}",
                userId, transferAmount, actualXu, transferCode, wallet.getBalance());

        return Map.of(
                "success", true,
                "transferCode", transferCode,
                "creditedXu", actualXu
        );
    }

    private String extractTransferCode(Map<String, Object> payload, String prefix) {
        String combined = String.join(" ",
                String.valueOf(payload.getOrDefault("code", "")),
                String.valueOf(payload.getOrDefault("content", "")),
                String.valueOf(payload.getOrDefault("description", ""))
        ).toUpperCase(Locale.ROOT);

        Pattern pattern = Pattern.compile("\\b(" + Pattern.quote(prefix) + "[A-Z0-9]{6,18})\\b");
        Matcher matcher = pattern.matcher(combined);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean isValidApikeyHeader(String authHeader, String expectedKey) {
        if (authHeader == null || authHeader.isBlank()) return false;
        String trimmed = authHeader.trim();
        String provided = trimmed;
        if (trimmed.regionMatches(true, 0, "Apikey ", 0, 7)) {
            provided = trimmed.substring(7).trim();
        } else if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            provided = trimmed.substring(7).trim();
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expectedKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String generateUniqueTransferCode(Long userId) {
        String prefix = safeTrim(sePayProperties.getCodePrefix(), "PKM").toUpperCase(Locale.ROOT);
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(prefix).append(userId).append('T');
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHANUM.charAt(SECURE_RANDOM.nextInt(ALPHANUM.length())));
            }
            String candidate = sb.toString();
            if (!walletTransactionRepository.existsByVnpTxnRef(candidate)) {
                return candidate;
            }
        }
        return prefix + userId + "T" + (System.currentTimeMillis() % 1000000L);
    }

    private BigDecimal parseBigDecimal(Object raw) {
        if (raw == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String safeTrim(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val.trim() : fallback;
    }

    private String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
