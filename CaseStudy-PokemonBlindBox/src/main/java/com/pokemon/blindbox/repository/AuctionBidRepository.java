package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuctionIdOrderByBidTimestampDesc(Long auctionId);
    List<AuctionBid> findTop50ByAuctionIdOrderByBidTimestampDesc(Long auctionId);
}
