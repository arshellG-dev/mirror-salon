package com.mirror.salon.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByHiddenFalseOrderByReviewedAtDesc();

    @Query("SELECT r FROM Review r JOIN FETCH r.customer WHERE r.hidden = false ORDER BY r.reviewedAt DESC")
    List<Review> findVisibleWithCustomer();

    @Query("SELECT r FROM Review r JOIN FETCH r.customer ORDER BY r.reviewedAt DESC")
    List<Review> findAllWithCustomer();

    long countByReplyTextIsNull();
}
