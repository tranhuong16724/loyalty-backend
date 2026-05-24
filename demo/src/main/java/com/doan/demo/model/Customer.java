package com.doan.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

    // ── Hạng thành viên ───────────────────────────────────────────────────────
    public static final String TIER_BRONZE   = "BRONZE";
    public static final String TIER_SILVER   = "SILVER";
    public static final String TIER_GOLD     = "GOLD";
    public static final String TIER_PLATINUM = "PLATINUM";

    // ── Fields ────────────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @JsonProperty("name")
    private String fullName;

    @Column(name = "phone", unique = true, nullable = false)
    @JsonProperty("phone")
    private String phoneNumber;

    @Column(name = "points")
    private int points;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "tier", length = 20)
    @JsonProperty("tier")
    private String tier = TIER_BRONZE;
    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "fcm_token", length = 512)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // không trả về client
    private String fcmToken;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long   getId()               { return id; }
    public void   setId(Long id)        { this.id = id; }

    public String getFullName()         { return fullName; }
    public void   setFullName(String n) { this.fullName = n; }

    public String getPhoneNumber()         { return phoneNumber; }
    public void   setPhoneNumber(String p) { this.phoneNumber = p; }

    public int  getPoints()          { return points; }
    public void setPoints(int points) {
        this.points = points;
        this.tier   = calcTier(points);
    }

    public String getEmail()          { return email; }
    public void   setEmail(String e)  { this.email = e; }

    public String getPassword()           { return password; }
    public void   setPassword(String pw)  { this.password = pw; }

    public String getTier()            { return tier != null ? tier : TIER_BRONZE; }
    public void   setTier(String tier) { this.tier = tier; }

    public String getFcmToken()             { return fcmToken; }
    public void   setFcmToken(String token) { this.fcmToken = token; }

    // ── Tier helpers ──────────────────────────────────────────────────────────
    public static String calcTier(int points) {
        if (points >= 3000) return TIER_PLATINUM;
        if (points >= 1500) return TIER_GOLD;
        if (points >=  500) return TIER_SILVER;
        return TIER_BRONZE;
    }

    public int pointsToNextTier() {
        if (points >= 3000) return 0;
        if (points >= 1500) return 3000 - points;
        if (points >=  500) return 1500 - points;
        return 500 - points;
    }

    public String getTierBadge() {
        switch (getTier()) {
            case TIER_PLATINUM: return "💎 Platinum";
            case TIER_GOLD:     return "🥇 Gold";
            case TIER_SILVER:   return "🥈 Silver";
            default:            return "🥉 Bronze";
        }
    }

    public String getTierColor() {
        switch (getTier()) {
            case TIER_PLATINUM: return "primary";
            case TIER_GOLD:     return "warning";
            case TIER_SILVER:   return "secondary";
            default:            return "danger";
        }
    }
}
