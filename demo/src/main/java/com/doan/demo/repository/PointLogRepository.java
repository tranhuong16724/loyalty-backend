package com.doan.demo.repository;

import com.doan.demo.model.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {
    List<PointLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}