package com.doan.demo.repository;

import com.doan.demo.model.WeeklyDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeeklyDealRepository extends JpaRepository<WeeklyDeal, Long> {
    List<WeeklyDeal> findByActiveTrue();
}