package com.doan.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;


@Entity
@Table(name = "weekly_deals")
public class WeeklyDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "original_price")
    private String originalPrice;

    @Column(name = "discount_price")
    private String discountPrice;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "active")
    private boolean active = true;
    @Column(name = "deal_type")
    private String dealType = "MULTI_DAY";
    @Column(name = "allowed_day_of_week")
    private Integer allowedDayOfWeek;

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
    public String getDealType() { return dealType; }
    public void setDealType(String dealType) { this.dealType = dealType; }

    public Integer getAllowedDayOfWeek() { return allowedDayOfWeek; }
    public void setAllowedDayOfWeek(Integer allowedDayOfWeek) { this.allowedDayOfWeek = allowedDayOfWeek; }
}