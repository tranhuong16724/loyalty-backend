package com.doan.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng lưu lịch sử TỪNG giao dịch điểm:
 *   - Cộng điểm khi mua hàng  → type = "EARN",  points = +X
 *   - Trừ điểm khi đổi voucher → type = "REDEEM", points = -X
 *
 * SQL tạo bảng (chạy trong MySQL):
 * CREATE TABLE IF NOT EXISTS point_transactions (
 *     id           INT AUTO_INCREMENT PRIMARY KEY,
 *     customer_id  INT NOT NULL,
 *     points       INT NOT NULL,          -- dương = cộng, âm = trừ
 *     type         VARCHAR(20) NOT NULL,  -- 'EARN' hoặc 'REDEEM'
 *     description  VARCHAR(255),
 *     created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     FOREIGN KEY (customer_id) REFERENCES Customers(id) ON DELETE CASCADE
 * );
 */
@Entity
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Dương = cộng điểm, Âm = trừ điểm */
    @Column(name = "points", nullable = false)
    private int points;

    /** "EARN" hoặc "REDEEM" */
    @Column(name = "type", nullable = false)
    private String type;

    /** Mô tả giao dịch, vd "Mua hàng 89.000đ" hoặc "Đổi: Khoai chiên nhỏ" */
    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId()                  { return id; }
    public Long getCustomerId()          { return customerId; }
    public void setCustomerId(Long v)    { this.customerId = v; }
    public int  getPoints()              { return points; }
    public void setPoints(int v)         { this.points = v; }
    public String getType()              { return type; }
    public void setType(String v)        { this.type = v; }
    public String getDescription()       { return description; }
    public void setDescription(String v) { this.description = v; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}