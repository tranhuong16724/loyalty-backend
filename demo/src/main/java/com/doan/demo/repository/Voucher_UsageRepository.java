package com.doan.demo.repository;

import com.doan.demo.model.VoucherUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface Voucher_UsageRepository extends JpaRepository<VoucherUsage, Long> {

    List<VoucherUsage> findByCustomerId(Long customerId);

    Optional<VoucherUsage> findByCode(String code);

    Page<VoucherUsage> findAll(Pageable pageable);

    @Query("SELECT u.voucherId, COUNT(u) FROM VoucherUsage u GROUP BY u.voucherId")
    List<Object[]> countGroupByVoucherId();
}