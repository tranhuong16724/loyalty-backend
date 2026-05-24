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

    // GET /api/vouchers — App lấy danh sách phần thưởng
    @GetMapping
    public List<Voucher> getAll() {
        return voucherRepository.findAll();
    }

    // POST /api/vouchers — Admin thêm voucher mới → push realtime đến tất cả app
    @PostMapping
    public Voucher create(@RequestBody Voucher v) {
        Voucher saved = voucherRepository.save(v);
        // ← MỚI: Sau khi lưu, push WebSocket đến tất cả khách hàng đang online
        LoyaltyWebSocketServer.broadcastNewVoucher(saved.getName(), saved.getId());
        return saved;
    }
}