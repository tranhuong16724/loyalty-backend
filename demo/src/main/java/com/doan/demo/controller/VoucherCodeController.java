package com.doan.demo.controller;

import com.doan.demo.model.*;
import com.doan.demo.repository.*;
import com.doan.demo.websocket.LoyaltyWebSocketServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/voucher-codes")
@CrossOrigin(origins = "*")
public class VoucherCodeController {

    @Autowired
    private DealCodeRepository dealCodeRepository;

    @Autowired
    private Voucher_UsageRepository voucherUsageRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    // ════════════════════════════════════════════════════════════════════════
    // GENERATE CODE
    // POST /api/voucher-codes/generate-code
    // ════════════════════════════════════════════════════════════════════════

    @PostMapping("/generate-code")
    public Map<String, Object> generateCode(
            @RequestParam Long usageId,
            @RequestParam Long customerId) {

        Map<String, Object> result = new LinkedHashMap<>();

        VoucherUsage usage =
                voucherUsageRepository
                        .findById(usageId)
                        .orElse(null);

        if (usage == null) {

            result.put("success", false);
            result.put("message", "Không tìm thấy voucher!");

            return result;
        }

        if (usage.isUsed()) {

            result.put("success", false);
            result.put("message", "Voucher này đã được sử dụng rồi!");

            return result;
        }

        // ── Xóa mã hết hạn cũ ─────────────────────────────

        dealCodeRepository.deleteExpired(
                LocalDateTime.now()
        );

        // ── Tạo mã QR/code ───────────────────────────────

        String code = generateUniqueCode();

        usage.setCode(code);

        voucherUsageRepository.save(usage);

        // ── Lưu deal_code ────────────────────────────────

        DealCode dc = new DealCode();

        dc.setDealId(-usageId);

        dc.setCustomerId(customerId);

        dc.setCode(code);

        dc.setUsed(false);

        dc.setMaxUses(1);

        dc.setTimesUsed(0);

        dc.setCreatedAt(LocalDateTime.now());

        dc.setExpiresAt(
                LocalDateTime.now().plusMinutes(30)
        );

        dealCodeRepository.save(dc);

        String vName =
                voucherRepository
                        .findById(usage.getVoucherId())
                        .map(Voucher::getName)
                        .orElse("Voucher");

        result.put("success", true);

        result.put("code", code);

        result.put("usageId", usageId);

        result.put("voucherName", vName);

        result.put("expiresAt",
                dc.getExpiresAt().toString());

        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // VERIFY CODE
    // POST /api/voucher-codes/verify-code
    // ════════════════════════════════════════════════════════════════════════

    @PostMapping("/verify-code")
    public Map<String, Object> verifyCode(
            @RequestParam String code) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        try {

            code = code.trim().toUpperCase();

            if (code.startsWith("KBVOUCHER|")) {

                code = code
                        .replace("KBVOUCHER|", "")
                        .trim();
            }

            DealCode dc =
                    dealCodeRepository
                            .findByCode(code)
                            .orElse(null);

            if (dc == null) {

                result.put("success", false);

                result.put("message",
                        "❌ Mã không tồn tại: " + code);

                return result;
            }

            // ── Check expiry ────────────────────────────

            if (dc.getExpiresAt() != null
                    && dc.getExpiresAt()
                    .isBefore(LocalDateTime.now())) {

                result.put("success", false);

                result.put("message",
                        "⏰ Mã đã hết hạn!");

                return result;
            }

            // ── Check usage limit ───────────────────────

            if (dc.getTimesUsed()
                    >= dc.getMaxUses()) {

                result.put("success", false);

                result.put("message",
                        "⚠️ Mã đã dùng đủ số lần!");

                return result;
            }

            // ── Voucher flow ────────────────────────────

            if (dc.getDealId() < 0) {

                Long usageId = -dc.getDealId();

                VoucherUsage usage =
                        voucherUsageRepository
                                .findById(usageId)
                                .orElse(null);

                if (usage != null
                        && !usage.isUsed()) {

                    usage.setUsed(true);

                    usage.setUsed_date(
                            LocalDateTime.now()
                    );

                    voucherUsageRepository.save(usage);

                    String vName =
                            voucherRepository
                                    .findById(
                                            usage.getVoucherId()
                                    )
                                    .map(Voucher::getName)
                                    .orElse("Voucher");

                    dc.setTimesUsed(
                            dc.getTimesUsed() + 1
                    );

                    dc.setUsed(true);

                    dealCodeRepository.save(dc);

                    LoyaltyWebSocketServer
                            .pushVoucherUsed(
                                    usage.getCustomerId(),
                                    usage.getCode(),
                                    vName
                            );

                    result.put("success", true);

                    result.put("message",
                            "✅ Xác nhận thành công!");

                    result.put("voucherName",
                            vName);

                    result.put("customerId",
                            usage.getCustomerId());

                    result.put("code",
                            usage.getCode());

                    return result;
                }
            }

            result.put("success", false);

            result.put("message",
                    "❌ Không thể xác nhận!");

            return result;

        } catch (Exception e) {

            result.put("success", false);

            result.put("message",
                    "❌ Lỗi: " + e.getMessage());

            return result;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GENERATE UNIQUE CODE
    // ════════════════════════════════════════════════════════════════════════

    private String generateUniqueCode() {

        String chars =
                "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        Random rng = new Random();

        String code;

        int attempts = 0;

        do {

            StringBuilder sb =
                    new StringBuilder(6);

            for (int i = 0; i < 6; i++) {

                sb.append(
                        chars.charAt(
                                rng.nextInt(chars.length())
                        )
                );
            }

            code = sb.toString();

            if (++attempts > 100) {
                break;
            }

        } while (
                dealCodeRepository
                        .findByCode(code)
                        .isPresent()
        );

        return code;
    }
}