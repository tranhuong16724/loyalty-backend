package com.doan.demo.controller;

import com.doan.demo.model.DealCode;
import com.doan.demo.model.WeeklyDeal;
import com.doan.demo.repository.DealCodeRepository;
import com.doan.demo.repository.WeeklyDealRepository;
import com.doan.demo.websocket.LoyaltyWebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/deals")
@CrossOrigin(origins = "*")
public class WeeklyDealController {

    @Autowired private WeeklyDealRepository weeklyDealRepository;
    @Autowired private DealCodeRepository   dealCodeRepository;

    @GetMapping
    public List<WeeklyDeal> getActiveDeals() {
        return weeklyDealRepository.findByActiveTrue();
    }

    @GetMapping("/all")
    public List<WeeklyDeal> getAllDeals() {
        return weeklyDealRepository.findAll();
    }

    @PostMapping("/generate-code")
    public Map<String, Object> generateCode(
            @RequestParam Long dealId,
            @RequestParam Long customerId) {

        Map<String, Object> result = new LinkedHashMap<>();

        WeeklyDeal deal = weeklyDealRepository.findById(dealId).orElse(null);
        if (deal == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy ưu đãi!");
            return result;
        }
        if ("FIXED_DAY".equals(deal.getDealType()) && deal.getAllowedDayOfWeek() != null) {
            int todayDow = java.time.LocalDate.now().getDayOfWeek().getValue();
            int todayDisplay = (todayDow % 7) + 1;
            if (todayDisplay != deal.getAllowedDayOfWeek()) {
                String[] dayNames = {"", "Chủ nhật", "Thứ 2", "Thứ 3",
                        "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
                String allowedDay = dayNames[deal.getAllowedDayOfWeek()];
                result.put("success", false);
                result.put("message", "⏰ Ưu đãi này chỉ áp dụng vào " + allowedDay + " hàng tuần!");
                return result;
            }
        }
        // Xóa mã hết hạn trong DB
        dealCodeRepository.deleteExpired(LocalDateTime.now());

        // Tạo mã 6 ký tự không trùng
        String code = generateUniqueCode();

        DealCode dc = new DealCode();
        dc.setDealId(dealId);
        dc.setCustomerId(customerId);
        dc.setCode(code);
        dc.setUsed(false);
        dc.setMaxUses(1);
        dc.setTimesUsed(0);
        dc.setCreatedAt(LocalDateTime.now());
        dc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        dealCodeRepository.save(dc);

        result.put("success",    true);
        result.put("code",       code);
        result.put("dealId",     dealId);
        result.put("dealTitle",  deal.getTitle());
        result.put("maxUses",    dc.getMaxUses());
        result.put("expiresAt",  dc.getExpiresAt().toString());
        return result;
    }

    @PostMapping("/verify-code")
    public Map<String, Object> verifyCode(@RequestParam String code) {
        Map<String, Object> result = new LinkedHashMap<>();

        DealCode dc = dealCodeRepository.findByCode(code.trim().toUpperCase()).orElse(null);

        if (dc == null) {
            result.put("success", false);
            result.put("message", "❌ Mã không tồn tại: " + code);
            return result;
        }

        if (dc.getExpiresAt().isBefore(LocalDateTime.now())) {
            result.put("success", false);
            result.put("message", "⏰ Mã đã hết hạn! Khách cần tạo mã mới.");
            return result;
        }

        if (dc.getTimesUsed() >= dc.getMaxUses()) {
            result.put("success", false);
            result.put("message", "⚠️ Mã đã dùng đủ " + dc.getMaxUses() + " lần!");
            return result;
        }

        dc.setTimesUsed(dc.getTimesUsed() + 1);

        if (dc.getTimesUsed() >= dc.getMaxUses()) {
            dc.setUsed(true);
        }
        dealCodeRepository.save(dc);

        String dealTitle = weeklyDealRepository.findById(dc.getDealId())
                .map(WeeklyDeal::getTitle).orElse("Ưu đãi");

        LoyaltyWebSocketServer.pushDealUsed(
                dc.getCustomerId(),
                dc.getCode(),
                dealTitle
        );

        int remaining = dc.getMaxUses() - dc.getTimesUsed();
        result.put("success",   true);
        result.put("message",   "✅ Xác nhận thành công! Ưu đãi: " + dealTitle
                + (remaining > 0 ? " (còn " + remaining + " lượt)" : ""));
        result.put("dealTitle",  dealTitle);
        result.put("customerId", dc.getCustomerId());
        result.put("timesUsed",  dc.getTimesUsed());
        result.put("maxUses",    dc.getMaxUses());
        result.put("remaining",  remaining);
        return result;
    }
    private String generateUniqueCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rng   = new Random();
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++)
                sb.append(chars.charAt(rng.nextInt(chars.length())));
            code = sb.toString();
            if (++attempts > 100) break;
        } while (dealCodeRepository.findByCode(code).isPresent());
        return code;
    }
}