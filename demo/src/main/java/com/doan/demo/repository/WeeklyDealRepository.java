package com.doan.demo.repository;

import com.doan.demo.model.WeeklyDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeeklyDealRepository extends JpaRepository<WeeklyDeal, Long> {
    /** Chỉ lấy ưu đãi đang active – dùng cho API gửi về App */
    List<WeeklyDeal> findByActiveTrue();
}