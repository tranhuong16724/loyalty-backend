package com.doan.demo.repository;

import com.doan.demo.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Lọc feedback theo customerId — dùng cho trang chi tiết khách hàng
    List<Feedback> findByCustomerId(Long customerId);
}