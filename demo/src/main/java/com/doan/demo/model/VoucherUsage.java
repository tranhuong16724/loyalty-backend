package com.doan.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Voucher_Usage")   // khớp đúng tên bảng trong DB
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "voucher_id")
    private Long voucherId;

    /**
     * Mã 6 ký tự để hiển thị thay cho usageId số.
     * DB mới đã có cột này: code VARCHAR(6) NOT NULL UNIQUE
     * Backend tự tạo khi insert.
     */
    @Column(name = "code", length = 6)
    private String code;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "used_date")
    private LocalDateTime used_date;

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public Long getCustomerId()                 { return customerId; }
    public void setCustomerId(Long v)           { this.customerId = v; }
    public Long getVoucherId()                  { return voucherId; }
    public void setVoucherId(Long v)            { this.voucherId = v; }
    public String getCode()                     { return code; }
    public void setCode(String v)               { this.code = v; }
    public boolean isUsed()                     { return used; }
    public void setUsed(boolean v)              { this.used = v; }
    public LocalDateTime getUsed_date()         { return used_date; }
    public void setUsed_date(LocalDateTime v)   { this.used_date = v; }
}