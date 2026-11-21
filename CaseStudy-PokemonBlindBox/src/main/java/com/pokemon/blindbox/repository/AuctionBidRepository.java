package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuctionIdOrderByBidTimestampDesc(Long auctionId);
    List<AuctionBid> findTop50ByAuctionIdOrderByBidTimestampDesc(Long auctionId);

    @Query("SELECT b.auction.id, COUNT(b.id) FROM AuctionBid b WHERE b.auction.id IN :auctionIds GROUP BY b.auction.id")
    List<Object[]> countBidsByAuctionIds(@Param("auctionIds") Collection<Long> auctionIds);
}
