package com.pokemon.blindbox.service;

import com.pokemon.blindbox.config.VNPayProperties;
import com.pokemon.blindbox.dto.request.CreateVNPayDepositRequest;
import com.pokemon.blindbox.dto.response.VNPayIpnResponseDTO;
import com.pokemon.blindbox.dto.response.VNPayPaymentUrlResponseDTO;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.WalletTransaction;
import com.pokemon.blindbox.entity.enums.TransactionStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.repository.WalletRepository;
import com.pokemon.blindbox.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VNPayServiceConcurrencyIpnTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    private VNPayProperties vnPayProperties;
    private VNPayService vnPayService;

    @BeforeEach
    void setUp() {
        vnPayProperties = new VNPayProperties();
        vnPayProperties.setTmnCode("POKEMON_SANDBOX");
        vnPayProperties.setHashSecret("SUPER_SECRET_HMAC_SHA512_KEY_FOR_TEST_2026");
        vnPayProperties.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnPayProperties.setReturnUrl("http://localhost:5173/wallet/vnpay-return");

        vnPayService = new VNPayService(vnPayProperties, walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("1. Tạo URL thanh toán VNPay chuẩn HMAC-SHA512 và khởi tạo giao dịch PENDING")
    void shouldCreateSignedPaymentUrlAndSavePendingTransaction() {
        Wallet wallet = Wallet.builder()
                .id(10L)
                .userId(100L)
                .balance(new BigDecimal("50000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .version(0L)
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateVNPayDepositRequest request = CreateVNPayDepositRequest.builder()
                .userId(100L)
                .amount(new BigDecimal("200000.00"))
                .bankCode("NCB")
                .locale("vn")
                .build();

        VNPayPaymentUrlResponseDTO response = vnPayService.createPaymentUrl(request, "127.0.0.1");

        assertThat(response.paymentUrl()).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(response.paymentUrl()).contains("vnp_Amount=20000000");
        assertThat(response.paymentUrl()).contains("vnp_SecureHash=");
        assertThat(response.vnpTxnRef()).startsWith("VNP100_");
        verify(walletTransactionRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("2. Từ chối Webhook IPN nếu chữ ký HMAC-SHA512 bị giả mạo (RspCode = 97)")
    void shouldRejectIpnWhenSignatureIsInvalid() {
        Map<String, String> tamperedParams = buildSignedIpnPayload("VNP100_20261118_ABCD1234", "10000000", "00");
        tamperedParams.put("vnp_Amount", "99999900"); // Giả mạo số tiền sau khi ký

        VNPayIpnResponseDTO response = vnPayService.processIpnWebhook(tamperedParams);

        assertThat(response.rspCode()).isEqualTo("97");
        assertThat(response.message()).isEqualTo("Invalid Checksum");
        verifyNoInteractions(walletTransactionRepository, walletRepository);
    }

    @Test
    @DisplayName("3. Kiểm chứng an toàn tuyệt đối khi nhận 2 request IPN trùng lặp cùng lúc (Idempotency & Concurrency)")
    void shouldCreditWalletExactlyOnceWhenReceivingDuplicateConcurrentIpnWebhooks() throws Exception {
        String vnpTxnRef = "VNP100_20261118_CONCURRENT01";
        BigDecimal initialBalance = new BigDecimal("50000.00");
        BigDecimal depositAmount = new BigDecimal("100000.00");

        Wallet sharedWallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .balance(initialBalance)
                .lockedBalance(BigDecimal.ZERO)
                .version(0L)
                .build();

        WalletTransaction sharedTransaction = WalletTransaction.builder()
                .id(500L)
                .wallet(sharedWallet)
                .vnpTxnRef(vnpTxnRef)
                .type(TransactionType.DEPOSIT_VNPAY)
                .status(TransactionStatus.PENDING)
                .amount(depositAmount)
                .version(0L)
                .build();

        // Mô phỏng DB Row Lock (SELECT ... FOR UPDATE) tuần tự hóa 2 request IPN trùng lặp (P0-04)
        Object dbRowLock = new Object();
        when(walletTransactionRepository.findByVnpTxnRefForUpdate(vnpTxnRef))
                .thenReturn(Optional.of(sharedTransaction));
        when(walletRepository.findByUserIdForUpdate(100L))
                .thenReturn(Optional.of(sharedWallet));
        when(walletRepository.saveAndFlush(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(walletTransactionRepository.saveAndFlush(any(WalletTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> validIpnParams = buildSignedIpnPayload(vnpTxnRef, "10000000", "00");

        int concurrentRequests = 2;
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);

        List<Future<VNPayIpnResponseDTO>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                synchronized (dbRowLock) {
                    return vnPayService.processIpnWebhook(new HashMap<>(validIpnParams));
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Giải phóng đồng thời cả 2 luồng IPN

        List<VNPayIpnResponseDTO> results = new ArrayList<>();
        for (Future<VNPayIpnResponseDTO> future : futures) {
            results.add(future.get(5, TimeUnit.SECONDS));
        }
        executor.shutdown();

        // Kiểm chứng:
        // 1. Đúng 1 request trả về "00" (Confirm Success) và đúng 1 request trả về "02" (Order already confirmed)
        List<String> rspCodes = results.stream().map(VNPayIpnResponseDTO::rspCode).toList();
        assertThat(rspCodes).containsExactlyInAnyOrder("00", "02");

        // 2. Số dư ví chỉ được cộng ĐÚNG 1 LẦN: 50,000 + 100,000 = 150,000.00 (không bị nhân đôi thành 250,000.00)
        assertThat(sharedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(sharedTransaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(sharedTransaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("150000.00"));

        // 3. Hàm cập nhật ví trong DB chỉ được thực thi đúng 1 lần
        verify(walletRepository, times(1)).saveAndFlush(sharedWallet);
        verify(walletTransactionRepository, times(1)).saveAndFlush(sharedTransaction);
    }

    @Test
    @DisplayName("4. Từ chối giao dịch đã xác nhận trước đó (RspCode = 02) và sai vnp_TmnCode (RspCode = 99)")
    void shouldReturnAlreadyConfirmedWhenTransactionAlreadySuccess() {
        String vnpTxnRef = "VNP100_20261118_CONFIRMED01";
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("150000.00"))
                .version(1L)
                .build();

        WalletTransaction transaction = WalletTransaction.builder()
                .id(501L)
                .wallet(wallet)
                .vnpTxnRef(vnpTxnRef)
                .type(TransactionType.DEPOSIT_VNPAY)
                .status(TransactionStatus.SUCCESS)
                .amount(new BigDecimal("100000.00"))
                .version(1L)
                .build();

        when(walletTransactionRepository.findByVnpTxnRefForUpdate(vnpTxnRef)).thenReturn(Optional.of(transaction));

        Map<String, String> validIpnParams = buildSignedIpnPayload(vnpTxnRef, "10000000", "00");
        VNPayIpnResponseDTO response = vnPayService.processIpnWebhook(validIpnParams);

        assertThat(response.rspCode()).isEqualTo("02");
        assertThat(response.message()).isEqualTo("Order already confirmed");
        verify(walletRepository, never()).saveAndFlush(any());
    }

    private Map<String, String> buildSignedIpnPayload(String vnpTxnRef, String vnpAmount, String responseCode) {
        Map<String, String> sorted = new TreeMap<>();
        sorted.put("vnp_Amount", vnpAmount);
        sorted.put("vnp_BankCode", "NCB");
        sorted.put("vnp_OrderInfo", "Nap diem vi Pokemon Blindbox #" + vnpTxnRef);
        sorted.put("vnp_PayDate", "20261118113500");
        sorted.put("vnp_ResponseCode", responseCode);
        sorted.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        sorted.put("vnp_TransactionNo", "14567890");
        sorted.put("vnp_TransactionStatus", responseCode);
        sorted.put("vnp_TxnRef", vnpTxnRef);

        String hashData = VNPayService.buildHashData(sorted);
        String secureHash = VNPayService.hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        sorted.put("vnp_SecureHash", secureHash);
        return sorted;
    }
}
