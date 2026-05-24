package com.doan.demo.model;
import jakarta.persistence.*;

@Entity
@Table(name = "tier_benefits")
public class TierBenefit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tier")        private String tier;
    @Column(name = "title")       private String title;
    @Column(name = "description") private String description;
    @Column(name = "active")      private boolean active = true;

    public Long    getId()             { return id; }
    public String  getTier()           { return tier; }
    public void    setTier(String v)   { this.tier = v; }
    public String  getTitle()          { return title; }
    public void    setTitle(String v)  { this.title = v; }
    public String  getDescription()    { return description; }
    public void    setDescription(String v){ this.description = v; }
    public boolean isActive()          { return active; }
    public void    setActive(boolean v){ this.active = v; }
}