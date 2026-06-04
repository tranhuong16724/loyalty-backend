package com.doan.demo.service;

import com.doan.demo.model.*;
import com.doan.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class VoucherService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final VoucherRepository          voucherRepository;
    private final Voucher_UsageRepository    voucherUsageRepository;
    private final DealCodeRepository         dealCodeRepository;
    private final WeeklyDealRepository       weeklyDealRepository;
    private final CustomerRepository         customerRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointService               pointService;
    private final TierService                tierService;
    private final FcmService                 fcmService;

    public VoucherService(VoucherRepository voucherRepository,
                          Voucher_UsageRepository voucherUsageRepository,
                          DealCodeRepository dealCodeRepository,
                          WeeklyDealRepository weeklyDealRepository,
                          CustomerRepository customerRepository,
                          PointTransactionRepository pointTransactionRepository,
                          PointService pointService,
                          TierService tierService,
                          FcmService fcmService) {
        this.voucherRepository          = voucherRepository;
        this.voucherUsageRepository     = voucherUsageRepository;
        this.dealCodeRepository         = dealCodeRepository;
        this.weeklyDealRepository       = weeklyDealRepository;
        this.customerRepository         = customerRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.pointService               = pointService;
        this.tierService                = tierService;
        this.fcmService                 = fcmService;
    }

    // ── Đổi voucher bằng điểm ────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> redeemVoucher(Long customerId, Long voucherId) {
        Map<String, Object> res = new LinkedHashMap<>();

        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        Voucher v = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        if (v.getMinTier() != null && !tierService.isTierSufficient(c.getTier(), v.getMinTier())) {
            res.put("success", false);
            res.put("message", "Voucher này yêu cầu hạng " + v.getMinTier() + " trở lên!");
            return res;
        }
        if (c.getPoints() < v.getPointsRequired()) {
            res.put("success", false);
            res.put("message", "Không đủ điểm! Cần " + v.getPointsRequired()
                    + " điểm, hiện có " + c.getPoints() + " điểm.");
            return res;
        }

        c.setPoints(c.getPoints() - v.getPointsRequired());
        customerRepository.save(c);

        PointTransaction tx = new PointTransaction();
        tx.setCustomerId(customerId);
        tx.setPoints(-v.getPointsRequired());
        tx.setType("REDEEM");
        tx.setDescription("Đổi voucher: " + v.getName());
        pointTransactionRepository.save(tx);

        VoucherUsage usage = new VoucherUsage();
        usage.setCustomerId(customerId);
        usage.setVoucherId(voucherId);
        usage.setCode(generateUniqueCode());
        usage.setUsed(false);
        usage.setUsed_date(LocalDateTime.now());
        voucherUsageRepository.save(usage);

        fcmService.sendNotification(c.getFcmToken(),
                "🎁 Đổi voucher thành công!",
                "Bạn còn " + c.getPoints() + " điểm tích lũy.",
                Map.of("type", "POINTS_UPDATE", "newPoints", String.valueOf(c.getPoints())));

        res.put("success", true);
        res.put("message", "Đổi voucher thành công!");
        res.put("code",    usage.getCode());
        return res;
    }



    @Transactional
    public Map<String, Object> verifyCode(String rawCode, Long callerCustomerId) {
        Map<String, Object> res = new LinkedHashMap<>();
        String code = normalizeCode(rawCode);

        DealCode dc = dealCodeRepository.findByCode(code).orElse(null);
        if (dc != null) return verifyDealCode(dc, code, callerCustomerId, res);

        VoucherUsage usage = voucherUsageRepository.findByCode(code).orElse(null);
        if (usage != null) return verifyVoucherUsage(usage, code, callerCustomerId, res);

        res.put("success", false);
        res.put("message", "❌ Không tìm thấy mã: " + code);
        return res;
    }


    private Map<String, Object> verifyDealCode(DealCode dc, String code,
                                               Long caller, Map<String, Object> res) {
        if (caller != null && !dc.getCustomerId().equals(caller)) {
            res.put("success", false);
            res.put("message", "⚠️ Mã không thuộc về tài khoản này!");
            return res;
        }
        if (dc.isUsed() || dc.getTimesUsed() >= dc.getMaxUses()) {
            res.put("success", false);
            res.put("message", "⚠️ Mã này đã được sử dụng rồi!");
            return res;
        }
        if (dc.getExpiresAt() != null && dc.getExpiresAt().isBefore(LocalDateTime.now())) {
            res.put("success", false);
            res.put("message", "⏰ Mã đã hết hạn! Yêu cầu khách mở lại mã mới trên App.");
            return res;
        }

        if (dc.getDealId() < 0) return verifyVoucherViaDealCode(dc, code, caller, res);

        dc.setTimesUsed(dc.getTimesUsed() + 1);
        if (dc.getTimesUsed() >= dc.getMaxUses()) dc.setUsed(true);
        dealCodeRepository.save(dc);

        WeeklyDeal deal = weeklyDealRepository.findById(dc.getDealId()).orElse(null);
        String dealTitle = deal != null ? deal.getTitle() : "Ưu đãi tuần";

        int earnPoints = 20; // fallback mặc định
        if (deal != null && deal.getDiscountPrice() != null) {
            try {
                String raw = deal.getDiscountPrice()
                        .replace(".", "")
                        .replace(",", "")
                        .replace("đ", "")
                        .replace("d", "")
                        .replaceAll("[^0-9]", "")
                        .trim();
                if (!raw.isEmpty()) {
                    earnPoints = (int)(Long.parseLong(raw) / 1000);
                }
            } catch (Exception ignored) {}
        }

        final int finalPoints = earnPoints;
        customerRepository.findById(dc.getCustomerId()).ifPresent(c -> {
            pointService.applyEarnPoints(c, finalPoints,
                    "Sử dụng ưu đãi tuần: " + dealTitle);
            fcmService.sendDataMessage(c.getFcmToken(),
                    Map.of("type", "DEAL_USED", "dealTitle", dealTitle, "code", code));
        });

        res.put("success", true); res.put("message", "✅ Xác nhận ưu đãi thành công: " + dealTitle);
        res.put("dealTitle", dealTitle); res.put("customerId", dc.getCustomerId());
        res.put("type", "deal"); res.put("code", code);
        return res;
    }

    private Map<String, Object> verifyVoucherViaDealCode(DealCode dc, String code,
                                                         Long caller, Map<String, Object> res) {
        Long usageId = -dc.getDealId();
        VoucherUsage usage = voucherUsageRepository.findById(usageId).orElse(null);
        if (usage == null)    { res.put("success", false); res.put("message", "❌ Không tìm thấy voucher!"); return res; }
        if (usage.isUsed())   { res.put("success", false); res.put("message", "⚠️ Voucher đã được dùng rồi!"); return res; }

        String vName = voucherRepository.findById(usage.getVoucherId()).map(Voucher::getName).orElse("Voucher");
        usage.setUsed(true); usage.setUsed_date(LocalDateTime.now()); voucherUsageRepository.save(usage);
        dc.setTimesUsed(dc.getTimesUsed() + 1); dc.setUsed(true); dealCodeRepository.save(dc);

        customerRepository.findById(usage.getCustomerId()).ifPresent(c ->
                fcmService.sendDataMessage(c.getFcmToken(),
                        Map.of("type", "TYPE_VOUCHER_USED", "voucherName", vName, "code", code)));

        res.put("success", true); res.put("message", "✅ Xác nhận thành công! Voucher: " + vName);
        res.put("voucherName", vName); res.put("customerId", usage.getCustomerId());
        res.put("type", "voucher"); res.put("code", code);
        return res;
    }

    private Map<String, Object> verifyVoucherUsage(VoucherUsage usage, String code,
                                                   Long caller, Map<String, Object> res) {
        if (caller != null && !usage.getCustomerId().equals(caller)) {
            res.put("success", false); res.put("message", "⚠️ Mã không thuộc về tài khoản này!"); return res;
        }
        if (usage.isUsed()) { res.put("success", false); res.put("message", "⚠️ Voucher này đã được sử dụng rồi!"); return res; }

        String vName = voucherRepository.findById(usage.getVoucherId()).map(Voucher::getName).orElse("Voucher");
        usage.setUsed(true); usage.setUsed_date(LocalDateTime.now()); voucherUsageRepository.save(usage);

        customerRepository.findById(usage.getCustomerId()).ifPresent(c -> {
            pointService.applyEarnPoints(c, 10, "Sử dụng voucher: " + vName);
            fcmService.sendDataMessage(c.getFcmToken(),
                    Map.of("type", "TYPE_VOUCHER_USED", "voucherName", vName, "code", code));
        });

        res.put("success", true); res.put("message", "✅ Xác nhận thành công! Voucher: " + vName);
        res.put("voucherName", vName); res.put("customerId", usage.getCustomerId());
        res.put("type", "voucher"); res.put("code", code);
        return res;
    }

    private static String normalizeCode(String raw) {
        String code = raw.trim().toUpperCase();
        if (code.startsWith("KBVOUCHER|")) code = code.replace("KBVOUCHER|", "").trim();
        if (code.startsWith("KBDEAL|"))    code = code.replace("KBDEAL|", "").trim();
        return code;
    }

    public String generateUniqueCode() {
        Random rng = new Random();
        String code; int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
            code = sb.toString();
            if (++attempts > 100) break;
        } while (voucherUsageRepository.findByCode(code).isPresent()
                || dealCodeRepository.findByCode(code).isPresent());
        return code;
    }
}