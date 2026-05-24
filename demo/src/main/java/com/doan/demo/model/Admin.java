package com.doan.demo.model;

import jakarta.persistence.*;

/**
 * Entity ánh xạ bảng Admin trong DB.
 * Dùng để xác thực đăng nhập Web Admin qua Spring Security.
 */
@Entity
@Table(name = "Admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Lưu BCrypt hash — KHÔNG lưu plain text. */
    @Column(nullable = false, length = 255)
    private String password;

    public Admin() {}

    public Long getId()                  { return id; }
    public String getUsername()          { return username; }
    public void   setUsername(String u)  { this.username = u; }
    public String getPassword()          { return password; }
    public void   setPassword(String p)  { this.password = p; }
}