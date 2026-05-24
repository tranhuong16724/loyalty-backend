package com.doan.demo.repository;

import com.doan.demo.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /** App chỉ lấy món đang active */
    List<MenuItem> findByActiveTrueOrderByIdAsc();

    /** Lọc theo category (App dùng) */
    List<MenuItem> findByCategoryAndActiveTrueOrderByIdAsc(String category);
}