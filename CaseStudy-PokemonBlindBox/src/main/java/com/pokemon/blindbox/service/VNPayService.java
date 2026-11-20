package com.pokemon.blindbox.service;

import com.pokemon.blindbox.config.VNPayProperties;
import com.pokemon.blindbox.dto.request.CreateVNPayDepositRequest;
import com.pokemon.blindbox.dto.response.VNPayIpnResponseDTO;
import com.pokemon.blindbox.dto.response.VNPayPaymentUrlResponseDTO;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * VNPayService chuyên biệt cho Cổng Thanh Toán VNPay (SOLID SRP):
 * 1. Tạo URL thanh toán VNPay Sandbox chuẩn chữ ký số HMAC-SHA512 và khởi tạo giao dịch PENDING.
 * 2. Xử lý Webhook IPN (vnpay-ipn): kiểm tra chữ ký HMAC-SHA512 (constant-time), kiểm tra vnp_TmnCode,
 *    kiểm tra số tiền và dùng Pessimistic Write Lock ở tầng DB (SELECT ... FOR UPDATE) để đảm bảo
 *    Idempotency tuyệt đối trên cả single-instance và multi-instance (P0-04).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private static final DateTimeFormatter VNP_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VNPayProperties vnPayProperties;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * Khởi tạo giao dịch nạp ví (PENDING) và sinh URL thanh toán VNPay chuẩn HMAC-SHA512.
     */
    @Transactional
    public VNPayPaymentUrlResponseDTO createPaymentUrl(CreateVNPayDepositRequest request, String clientIp) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.valueOf(10000)) < 0) {
            throw new BusinessValidationException("Số tiền nạp tối thiểu là 10,000 VND.");
        }

        Wallet wallet = walletRepository.findByUserId(request.userId())
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .userId(request.userId())
                                .balance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                                .lockedBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                                .build()
                ));

        BigDecimal normalizedAmount = request.amount().setScale(2, RoundingMode.HALF_UP);
        String vnpTxnRef = generateUniqueTxnRef(request.userId());

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .vnpTxnRef(vnpTxnRef)
                .type(TransactionType.DEPOSIT_VNPAY)
                .status(TransactionStatus.PENDING)
                .amount(normalizedAmount)
                .description("Nap diem vi Pokemon Blindbox qua VNPay #" + vnpTxnRef)
                .build();
        walletTransactionRepository.save(transaction);

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime expiresAt = now.plusMinutes(vnPayProperties.getExpireMinutes());

        long vnpAmount = normalizedAmount.multiply(BigDecimal.valueOf(100)).longValueExact();
        String locale = (request.locale() != null && !request.locale().isBlank()) ? request.locale().trim() : "vn";
        String ipAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "127.0.0.1";

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnPayProperties.getVersion());
        vnpParams.put("vnp_Command", vnPayProperties.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", vnPayProperties.getCurrCode());
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", transaction.getDescription());
        vnpParams.put("vnp_OrderType", vnPayProperties.getOrderType());
        vnpParams.put("vnp_Locale", locale);
        vnpParams.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddr);
        vnpParams.put("vnp_CreateDate", now.format(VNP_DATE_FORMATTER));
        vnpParams.put("vnp_ExpireDate", expiresAt.format(VNP_DATE_FORMATTER));

        if (request.bankCode() != null && !request.bankCode().isBlank()) {
            vnpParams.put("vnp_BankCode", request.bankCode().trim());
        }

        String hashData = buildHashData(vnpParams);
        String queryUrl = buildQueryString(vnpParams);
        String vnpSecureHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData);

        String fullPaymentUrl = vnPayProperties.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + vnpSecureHash;

        return VNPayPaymentUrlResponseDTO.builder()
                .paymentUrl(fullPaymentUrl)
                .vnpTxnRef(vnpTxnRef)
                .amount(normalizedAmount)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Xử lý Webhook IPN từ VNPay (vnpay-ipn):
     * 1. Kiểm tra chữ ký điện tử HMAC-SHA512 (constant-time).
     * 2. Kiểm tra sự tồn tại của giao dịch theo vnp_TxnRef.
     * 3. Đối chiếu số tiền vnp_Amount khớp với số tiền lưu trên Server.
     * 4. Đảm bảo Idempotency: chỉ xử lý khi trạng thái đang là PENDING.
     *    Nếu 2 request IPN trùng lặp bắn vào cùng lúc, chỉ 1 request cộng tiền ("00"),
     *    request còn lại trả về "02" (Order already confirmed).
     */
    @Transactional
    public VNPayIpnResponseDTO processIpnWebhook(Map<String, String> rawParams) {
        if (rawParams == null || rawParams.isEmpty()) {
            return VNPayIpnResponseDTO.of("99", "Invalid parameters");
        }

        // 1. Kiểm tra chữ ký điện tử HMAC-SHA512
        if (!verifySignature(rawParams)) {
            log.warn("[VNPAY-IPN] Invalid signature for params: {}", rawParams.get("vnp_TxnRef"));
            return VNPayIpnResponseDTO.of("97", "Invalid Checksum");
        }

        // 1b. Kiểm tra vnp_TmnCode khớp cấu hình server
        String receivedTmnCode = rawParams.get("vnp_TmnCode");
        if (receivedTmnCode == null || !receivedTmnCode.equals(vnPayProperties.getTmnCode())) {
            log.warn("[VNPAY-IPN] Invalid TmnCode: {}", receivedTmnCode);
            return VNPayIpnResponseDTO.of("99", "Invalid TmnCode");
        }

        String vnpTxnRef = rawParams.get("vnp_TxnRef");
        if (vnpTxnRef == null || vnpTxnRef.isBlank()) {
            return VNPayIpnResponseDTO.of("01", "Order not found");
        }

        // 2. Khóa dòng giao dịch ở tầng DB bằng SELECT ... FOR UPDATE (P0-04, an toàn đa nút)
        Optional<WalletTransaction> txOpt = walletTransactionRepository.findByVnpTxnRefForUpdate(vnpTxnRef);
        if (txOpt.isEmpty()) {
            return VNPayIpnResponseDTO.of("01", "Order not found");
        }

        WalletTransaction transaction = txOpt.get();

        // 3. Đối chiếu số tiền (vnp_Amount = amount * 100)
        String vnpAmountStr = rawParams.get("vnp_Amount");
        long expectedVnpAmount = transaction.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
        long receivedVnpAmount;
        try {
            receivedVnpAmount = Long.parseLong(vnpAmountStr);
        } catch (NumberFormatException ex) {
            return VNPayIpnResponseDTO.of("04", "Invalid Amount");
        }
        if (expectedVnpAmount != receivedVnpAmount) {
            log.warn("[VNPAY-IPN] Amount mismatch for vnpTxnRef={}: expected={}, received={}",
                    vnpTxnRef, expectedVnpAmount, receivedVnpAmount);
            return VNPayIpnResponseDTO.of("04", "Invalid Amount");
        }

        // 4. Kiểm tra trạng thái PENDING (Idempotency Check sau khi đã nắm giữ Row Lock DB)
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.info("[VNPAY-IPN] Duplicate IPN ignored for vnpTxnRef={}, currentStatus={}",
                    vnpTxnRef, transaction.getStatus());
            return VNPayIpnResponseDTO.of("02", "Order already confirmed");
        }

        // 5. Cập nhật trạng thái giao dịch & cộng điểm vào ví
        String responseCode = rawParams.get("vnp_ResponseCode");
        String transactionStatus = rawParams.getOrDefault("vnp_TransactionStatus", responseCode);
        transaction.setVnpResponseCode(responseCode);
        transaction.setVnpTransactionNo(rawParams.get("vnp_TransactionNo"));
        transaction.setVnpBankCode(rawParams.get("vnp_BankCode"));

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            Wallet wallet = walletRepository.findByUserIdForUpdate(transaction.getWallet().getUserId())
                    .orElse(transaction.getWallet());
            wallet.credit(transaction.getAmount());
            walletRepository.saveAndFlush(wallet);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setBalanceAfter(wallet.getBalance());
            walletTransactionRepository.saveAndFlush(transaction);

            log.info("[VNPAY-IPN] Credited wallet userId={}, amount={}, newBalance={}, vnpTxnRef={}",
                    wallet.getUserId(), transaction.getAmount(), wallet.getBalance(), vnpTxnRef);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setBalanceAfter(transaction.getWallet().getBalance());
            walletTransactionRepository.saveAndFlush(transaction);

            log.info("[VNPAY-IPN] Marked transaction FAILED for vnpTxnRef={}, responseCode={}",
                    vnpTxnRef, responseCode);
        }

        return VNPayIpnResponseDTO.of("00", "Confirm Success");
    }

    /**
     * Hỗ trợ giả lập gọi Webhook IPN có chữ ký HMAC-SHA512 hợp lệ cho giao dịch PENDING trên môi trường Local/Sandbox.
     */
    @Transactional
    public VNPayIpnResponseDTO simulateSignedIpnForLocalTest(String vnpTxnRef) {
        WalletTransaction tx = walletTransactionRepository.findByVnpTxnRef(vnpTxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch với vnpTxnRef = " + vnpTxnRef));

        long vnpAmount = tx.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        Map<String, String> signedParams = new TreeMap<>();
        signedParams.put("vnp_Amount", String.valueOf(vnpAmount));
        signedParams.put("vnp_BankCode", "NCB");
        signedParams.put("vnp_OrderInfo", tx.getDescription() != null ? tx.getDescription() : ("Nap vi #" + vnpTxnRef));
        signedParams.put("vnp_PayDate", LocalDateTime.now(VIETNAM_ZONE).format(VNP_DATE_FORMATTER));
        signedParams.put("vnp_ResponseCode", "00");
        signedParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        signedParams.put("vnp_TransactionNo", String.valueOf(System.currentTimeMillis() % 100000000L));
        signedParams.put("vnp_TransactionStatus", "00");
        signedParams.put("vnp_TxnRef", vnpTxnRef);

        String hashData = buildHashData(signedParams);
        String secureHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        signedParams.put("vnp_SecureHash", secureHash);

        return processIpnWebhook(signedParams);
    }

    /**
     * Kiểm tra chữ ký điện tử HMAC-SHA512 của VNPay bằng so sánh constant-time.
     */
    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equalsIgnoreCase(key)
                    && !"vnp_SecureHashType".equalsIgnoreCase(key)
                    && value != null && !value.isEmpty()) {
                sortedParams.put(key, value);
            }
        }

        String hashData = buildHashData(sortedParams);
        String computedHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData);

        return MessageDigest.isEqual(
                computedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                receivedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Tính toán mã băm HMAC-SHA512 theo chuẩn VNPay 2.1.0.
     */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể khởi tạo chữ ký số HMAC-SHA512", ex);
        }
    }

    public static String buildHashData(Map<String, String> sortedParams) {
        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = sortedParams.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            hashData.append(entry.getKey())
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            if (itr.hasNext()) {
                hashData.append('&');
            }
        }
        return hashData.toString();
    }

    private static String buildQueryString(Map<String, String> sortedParams) {
        StringBuilder query = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = sortedParams.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            if (itr.hasNext()) {
                query.append('&');
            }
        }
        return query.toString();
    }

    private String generateUniqueTxnRef(Long userId) {
        String timestamp = LocalDateTime.now(VIETNAM_ZONE).format(VNP_DATE_FORMATTER);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("VNP%d_%s_%s", userId, timestamp, randomSuffix);
    }
}
