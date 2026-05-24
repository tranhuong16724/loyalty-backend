package com.doan.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "feedback") // Khớp với tên bảng trong MySQL của bạn
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "noi_dung")
    private String noi_dung;

    // Getter và Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getNoi_dung() { return noi_dung; }
    public void setNoi_dung(String noi_dung) { this.noi_dung = noi_dung; }
}