package com.doan.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "points_required")
    private int pointsRequired;

    /**
     * Hạng tối thiểu để đổi voucher này.
     * NULL = không giới hạn hạng (ai cũng đổi được).
     * Giá trị hợp lệ: "BRONZE" | "SILVER" | "GOLD" | "PLATINUM"
     */
    @Column(name = "min_tier", length = 20)
    private String minTier;

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long   getId()                  { return id; }

    public String getName()                { return name; }
    public void   setName(String name)     { this.name = name; }

    public int    getPointsRequired()                      { return pointsRequired; }
    public void   setPointsRequired(int pointsRequired)    { this.pointsRequired = pointsRequired; }

    public String getMinTier()             { return minTier; }
    public void   setMinTier(String minTier) { this.minTier = minTier; }
}