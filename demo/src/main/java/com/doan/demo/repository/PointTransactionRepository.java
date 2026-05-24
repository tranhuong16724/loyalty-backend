package com.doan.demo.repository;

import com.doan.demo.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}