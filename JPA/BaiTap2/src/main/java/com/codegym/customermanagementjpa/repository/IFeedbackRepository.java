package com.codegym.customermanagementjpa.repository;

import com.codegym.customermanagementjpa.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IFeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findAllByOrderByDateDescIdDesc(Pageable pageable);

    Page<Feedback> findAllByDateOrderByDateDescIdDesc(LocalDate date, Pageable pageable);

    List<Feedback> findAllByDate(LocalDate date);

    @Modifying
    @Transactional
    @Query("UPDATE Feedback f SET f.likes = f.likes + 1 WHERE f.id = :id")
    void incrementLike(@Param("id") Long id);
}
