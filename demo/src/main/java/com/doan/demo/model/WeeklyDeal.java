package com.doan.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Bảng lưu ưu đãi / khuyến mãi trong tuần
 * Admin tạo từ Web, App hiển thị cho khách hàng
 */
@Entity
@Table(name = "weekly_deals")
public class WeeklyDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tiêu đề ưu đãi, vd: "Combo Vừa Ý" */
    @Column(name = "title", nullable = false)
    private String title;

    /** Mô tả ngắn, vd: "Tiết kiệm 30%" */
    @Column(name = "description")
    private String description;

    /** Giá gốc (để hiển thị gạch ngang), vd: "99.000đ" */
    @Column(name = "original_price")
    private String originalPrice;

    /** Giá ưu đãi, vd: "69.000đ" */
    @Column(name = "discount_price")
    private String discountPrice;

    /** Ngày kết thúc ưu đãi */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Còn hiệu lực hay đã ẩn */
    @Column(name = "active")
    private boolean active = true;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(String originalPrice) { this.originalPrice = originalPrice; }

    public String getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(String discountPrice) { this.discountPrice = discountPrice; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}