package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.event.BoxStateUpdateEventDTO;
import com.pokemon.blindbox.dto.response.BoosterPackResponseDTO;
import com.pokemon.blindbox.dto.response.BoxPurchaseResponseDTO;
import com.pokemon.blindbox.entity.BoosterBox;
import com.pokemon.blindbox.entity.BoosterPack;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.enums.BoxStatus;
import com.pokemon.blindbox.entity.enums.PackStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.BoosterBoxRepository;
import com.pokemon.blindbox.repository.BoosterPackRepository;
import com.pokemon.blindbox.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BoxPurchaseService quản lý giao dịch mua Pack lẻ và mua nguyên Box:
 * - Thực thi trong một @Transactional duy nhất.
 * - Dùng Atomic SQL Update trên BoosterBox khi mua Pack lẻ để chống Hot-Row @Version contention (P1-02).
 * - Khi mua 1 Pack lẻ thành công, lập tức khóa cờ canBuyWholeBox = false trên BoosterBox.
 * - Khi mua nguyên Box, chỉ cho phép nếu canBuyWholeBox == true và chưa có Pack lẻ nào bị bán.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoxPurchaseService {

    private static final Long DEFAULT_SYSTEM_USER_ID = 1L;

    private final BoosterBoxRepository boosterBoxRepository;
    private final BoosterPackRepository boosterPackRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Overload hỗ trợ chữ ký rút gọn buySinglePack(Long boxId, Integer packPosition).
     */
    @Transactional
    public BoxPurchaseResponseDTO buySinglePack(Long boxId, Integer packPosition) {
        return buySinglePack(DEFAULT_SYSTEM_USER_ID, boxId, packPosition);
    }

    /**
     * Mua 1 Pack lẻ tại vị trí packPosition trên khay BoosterBox:
     * 1. Kiểm tra BoosterPack đang ở trạng thái AVAILABLE.
     * 2. Trừ tiền snapshotPackPrice trong Wallet của người chơi.
     * 3. Chuyển trạng thái BoosterPack sang SOLD (kích hoạt kiểm tra @Version).
     * 4. Lập tức cập nhật boosterBox.setCanBuyWholeBox(false) và giảm remainingPacks đi 1.
     */
    @Transactional
    public BoxPurchaseResponseDTO buySinglePack(Long userId, Long boxId, Integer packPosition) {
        BoosterPack pack = boosterPackRepository.findByBoosterBoxIdAndPositionInBox(boxId, packPosition)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Không tìm thấy gói bài ở vị trí #%d trong hộp boxId = %d", packPosition, boxId)
                ));

        if (pack.getStatus() != PackStatus.AVAILABLE) {
            throw new BusinessValidationException(
                    String.format("Gói bài ở vị trí #%d đã được bán hoặc đã mở. Vui lòng chọn gói khác!", packPosition)
            );
        }

        BoosterBox box = pack.getBoosterBox();
        if (box.getRemainingPacks() <= 0 || box.getStatus() == BoxStatus.SOLD_OUT) {
            throw new BusinessValidationException("Hộp bài này đã bán hết toàn bộ gói bài.");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người chơi với userId = " + userId));

        BigDecimal price = pack.getSnapshotPackPrice();
        wallet.debit(price);
        walletRepository.saveAndFlush(wallet);

        walletService.recordTransaction(
                wallet,
                TransactionType.PURCHASE_PACK,
                price,
                String.format("Mua gói lẻ #%d thuộc hộp %s", packPosition, box.getBoxCode())
        );

        LocalDateTime now = LocalDateTime.now();
        pack.setStatus(PackStatus.SOLD);
        pack.setBuyerId(userId);
        pack.setPurchasedAt(now);
        boosterPackRepository.saveAndFlush(pack);

        // P1-02 & REG-01: Atomic SQL Update trên BoosterBox (tuyệt đối KHÔNG gọi setter lên managed entity box để tránh Hibernate dirty-check bắn thêm UPDATE gây 409!)
        int rowsUpdated = boosterBoxRepository.decrementRemainingPackAtomically(box.getId());
        if (rowsUpdated == 0) {
            throw new BusinessValidationException("Hộp bài này đã bán hết toàn bộ gói bài.");
        }
        int updatedRemaining = Math.max(0, box.getRemainingPacks() - 1);
        BoxStatus updatedStatus = updatedRemaining == 0 ? BoxStatus.SOLD_OUT : BoxStatus.PARTIALLY_SOLD;

        // Phát sự kiện để WebSocketBroadcastListener bắn tới /topic/boxes/{boxId} sau khi DB commit
        eventPublisher.publishEvent(BoxStateUpdateEventDTO.builder()
                .boxId(box.getId())
                .soldPackPosition(packPosition)
                .remainingPacks(updatedRemaining)
                .canBuyWholeBox(false)
                .build());

        log.info("User {} purchased single pack boxId={}, pos={}, remainingPacks={}, canBuyWholeBox=false",
                userId, boxId, packPosition, updatedRemaining);

        return BoxPurchaseResponseDTO.builder()
                .boxId(box.getId())
                .boxCode(box.getBoxCode())
                .canBuyWholeBox(false)
                .remainingPacks(updatedRemaining)
                .boxStatus(updatedStatus)
                .totalCharged(price)
                .remainingWalletBalance(wallet.getBalance())
                .purchasedPacks(List.of(BoosterPackResponseDTO.fromEntity(pack)))
                .build();
    }

    /**
     * Overload hỗ trợ chữ ký rút gọn buyWholeBox(Long boxId).
     */
    @Transactional
    public BoxPurchaseResponseDTO buyWholeBox(Long boxId) {
        return buyWholeBox(DEFAULT_SYSTEM_USER_ID, boxId);
    }

    /**
     * Mua nguyên hộp BoosterBox:
     * Chỉ được phép thực thi khi boosterBox.isCanBuyWholeBox() == true
     * và remainingPacks.equals(totalPacks) (chưa có bất kỳ Pack lẻ nào bị bán).
     */
    @Transactional
    public BoxPurchaseResponseDTO buyWholeBox(Long userId, Long boxId) {
        BoosterBox box = boosterBoxRepository.findById(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hộp bài với boxId = " + boxId));

        if (!box.isCanBuyWholeBox()
                || !box.getRemainingPacks().equals(box.getTotalPacks())
                || box.getStatus() != BoxStatus.AVAILABLE) {
            throw new BusinessValidationException(
                    "Không thể mua nguyên hộp vì hộp bài này đã có gói lẻ được bán ra!"
            );
        }

        List<BoosterPack> packs = boosterPackRepository.findByBoosterBoxIdOrderByPositionInBoxAsc(boxId);
        boolean anyPackNotAvailable = packs.stream().anyMatch(p -> p.getStatus() != PackStatus.AVAILABLE);
        if (packs.isEmpty() || anyPackNotAvailable) {
            throw new BusinessValidationException(
                    "Không thể mua nguyên hộp vì đã có gói lẻ không còn ở trạng thái AVAILABLE!"
            );
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người chơi với userId = " + userId));

        BigDecimal boxPrice = box.getSnapshotBoxPrice();
        wallet.debit(boxPrice);
        walletRepository.saveAndFlush(wallet);

        walletService.recordTransaction(
                wallet,
                TransactionType.PURCHASE_BOX,
                boxPrice,
                String.format("Mua nguyên hộp Booster Box %s (%d gói)", box.getBoxCode(), box.getTotalPacks())
        );

        LocalDateTime now = LocalDateTime.now();
        for (BoosterPack pack : packs) {
            pack.setStatus(PackStatus.SOLD);
            pack.setBuyerId(userId);
            pack.setPurchasedAt(now);
        }
        boosterPackRepository.saveAllAndFlush(packs);

        box.setCanBuyWholeBox(false);
        box.setRemainingPacks(0);
        box.setStatus(BoxStatus.SOLD_OUT);
        boosterBoxRepository.saveAndFlush(box);

        eventPublisher.publishEvent(BoxStateUpdateEventDTO.builder()
                .boxId(box.getId())
                .soldPackPosition(null)
                .remainingPacks(0)
                .canBuyWholeBox(false)
                .build());

        log.info("User {} purchased whole boxId={}, boxCode={}, totalPacks={}",
                userId, boxId, box.getBoxCode(), packs.size());

        return BoxPurchaseResponseDTO.builder()
                .boxId(box.getId())
                .boxCode(box.getBoxCode())
                .canBuyWholeBox(box.isCanBuyWholeBox())
                .remainingPacks(box.getRemainingPacks())
                .boxStatus(box.getStatus())
                .totalCharged(boxPrice)
                .remainingWalletBalance(wallet.getBalance())
                .purchasedPacks(packs.stream().map(BoosterPackResponseDTO::fromEntity).toList())
                .build();
    }
}
