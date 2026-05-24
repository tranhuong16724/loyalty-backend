// ── FILE 1: TierConfig.java ──────────────────────────────────────────────────
package com.doan.demo.model;
import jakarta.persistence.*;

@Entity
@Table(name = "tier_config")
public class TierConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tier", unique = true)   private String tier;       // DONG/BAC/VANG
    @Column(name = "min_points")            private int    minPoints;
    @Column(name = "label")                 private String label;      // "Đồng"/"Bạc"/"Vàng"
    @Column(name = "color")                 private String color;      // hex
    @Column(name = "bonus_points")          private int    bonusPoints;

    public Long   getId()           { return id; }
    public String getTier()         { return tier; }
    public void   setTier(String v) { this.tier = v; }
    public int    getMinPoints()    { return minPoints; }
    public void   setMinPoints(int v){ this.minPoints = v; }
    public String getLabel()        { return label; }
    public void   setLabel(String v){ this.label = v; }
    public String getColor()        { return color; }
    public void   setColor(String v){ this.color = v; }
    public int    getBonusPoints()  { return bonusPoints; }
    public void   setBonusPoints(int v){ this.bonusPoints = v; }
}