package com.doan.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Lưu lịch sử Admin điều chỉnh điểm thủ công (cộng/trừ điểm kèm lý do)
 */
@Entity
@Table(name = "point_logs")
public class PointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    /** Số điểm thay đổi — dương = cộng, âm = trừ */
    @Column(name = "delta")
    private int delta;

    /** Lý do điều chỉnh (ghi chú của admin) */
    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}