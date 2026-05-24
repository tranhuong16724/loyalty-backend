package com.doan.demo.repository;

import com.doan.demo.model.DealCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DealCodeRepository extends JpaRepository<DealCode, Long> {

    Optional<DealCode> findByCode(String code);

    /** Xóa các mã hết hạn */
    @Modifying
    @Transactional
    @Query("DELETE FROM DealCode d WHERE d.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}