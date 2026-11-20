package com.pokemon.blindbox.event;

import com.pokemon.blindbox.dto.event.AuctionBidUpdateEventDTO;
import com.pokemon.blindbox.dto.event.BoxStateUpdateEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lắng nghe sự kiện sau khi Transaction đã COMMIT thành công (TransactionPhase.AFTER_COMMIT)
 * và phát sóng thời gian thực qua SimpMessagingTemplate theo đúng .agent/rules.md Mục 4:
 * 1. /topic/boxes/{boxId} -> BoxStateUpdateEventDTO
 * 2. /topic/auctions/{auctionId} -> AuctionBidUpdateEventDTO
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBoxStateUpdated(BoxStateUpdateEventDTO event) {
        String destination = "/topic/boxes/" + event.boxId();
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WS-BROADCAST] Sent BoxStateUpdateEventDTO to {}: soldPos={}, remaining={}, canBuyWholeBox={}",
                destination, event.soldPackPosition(), event.remainingPacks(), event.canBuyWholeBox());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionBidUpdated(AuctionBidUpdateEventDTO event) {
        String destination = "/topic/auctions/" + event.auctionId();
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WS-BROADCAST] Sent AuctionBidUpdateEventDTO to {}: newHighestBid={}, bidder={}",
                destination, event.newHighestBid(), event.maskedBidderName());
    }
}
