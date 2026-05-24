package com.doan.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deal_codes")
public class DealCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deal_id")
    private Long dealId;

    @Column(name = "customer_id")
    private Long customerId;

    /** Mã 6 ký tự chữ+số, VD: "A3K9XZ" */
    @Column(name = "code", unique = true, length = 6)
    private String code;

    @Column(name = "used")
    private boolean used = false;

    /** Số lần dùng tối đa (mặc định 1) */
    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    /** Số lần đã dùng */
    @Column(name = "times_used", nullable = false)
    private int timesUsed = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                       { return id; }
    public Long getDealId()                   { return dealId; }
    public void setDealId(Long v)             { this.dealId = v; }
    public Long getCustomerId()               { return customerId; }
    public void setCustomerId(Long v)         { this.customerId = v; }
    public String getCode()                   { return code; }
    public void setCode(String v)             { this.code = v; }
    public boolean isUsed()                   { return used; }
    public void setUsed(boolean v)            { this.used = v; }
    public int getMaxUses()                   { return maxUses; }
    public void setMaxUses(int v)             { this.maxUses = v; }
    public int getTimesUsed()                 { return timesUsed; }
    public void setTimesUsed(int v)           { this.timesUsed = v; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getExpiresAt()       { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }

    /** Còn lượt dùng không? */
    public boolean canUse() { return timesUsed < maxUses && !used; }
}