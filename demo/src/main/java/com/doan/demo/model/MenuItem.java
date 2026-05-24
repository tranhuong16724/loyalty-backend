package com.doan.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price")
    private String price;           // VD: "89.000đ"

    @Column(name = "emoji")
    private String emoji;           // VD: "🍔"

    @Column(name = "category")
    private String category;        // BURGER / DRINK / Chicken / Món khác

    @Column(name = "badge")
    private String badge;           // Hot / Mới / Bán chạy (có thể null)

    @Column(name = "active")
    private boolean active = true;  // true = đang bán, false = đã tắt

    // FIX: thêm @Column để JPA map đúng cột image_url trong DB
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long    getId()               { return id; }
    public String  getName()             { return name; }
    public void    setName(String v)     { this.name = v; }
    public String  getPrice()            { return price; }
    public void    setPrice(String v)    { this.price = v; }
    public String  getEmoji()            { return emoji; }
    public void    setEmoji(String v)    { this.emoji = v; }
    public String  getCategory()         { return category; }
    public void    setCategory(String v) { this.category = v; }
    public String  getBadge()            { return badge; }
    public void    setBadge(String v)    { this.badge = v; }
    public boolean isActive()            { return active; }
    public void    setActive(boolean v)  { this.active = v; }
    public String  getImageUrl()         { return imageUrl; }
    public void    setImageUrl(String v) { this.imageUrl = v; }
}