package com.codegym.repository;

import com.codegym.model.BorrowTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BorrowTicketRepository extends JpaRepository<BorrowTicket, Long> {
    Optional<BorrowTicket> findByBorrowCode(String borrowCode);
    boolean existsByBorrowCode(String borrowCode);
}
