package com.doan.demo.controller;

import com.doan.demo.model.Voucher;
import com.doan.demo.repository.VoucherRepository;
import com.doan.demo.websocket.LoyaltyWebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@CrossOrigin(origins = "*")
public class VoucherController {

    @Autowired
    private VoucherRepository voucherRepository;

    @GetMapping
    public List<Voucher> getAll() {
        return voucherRepository.findAll();
    }

    @PostMapping
    public Voucher create(@RequestBody Voucher v) {
        Voucher saved = voucherRepository.save(v);
        LoyaltyWebSocketServer.broadcastNewVoucher(saved.getName(), saved.getId());
        return saved;
    }
}