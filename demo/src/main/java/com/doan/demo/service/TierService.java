package com.doan.demo.service;

import com.doan.demo.model.TierConfig;
import com.doan.demo.repository.TierConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Single source of truth cho toàn bộ tier logic.
 *
 * Trước khi có class này, ngưỡng tier bị hard-code ở 3 chỗ:
 *   1. Customer.calcTier()  — backend model
 *   2. SessionManager       — Android app
 *   3. bảng tier_config     — DB
 *
 * Giờ backend chỉ đọc từ DB (tier_config). Nếu admin muốn thay
 * ngưỡng thì chỉ cần UPDATE tier_config — không cần deploy lại.
 *
 * Lưu ý: Android app vẫn cần cập nhật SessionManager riêng,
 * hoặc fetch /api/tiers khi khởi động để lấy config mới nhất.
 */
@Service
public class TierService {

    private final TierConfigRepository tierConfigRepository;

    private static final List<String> TIER_ORDER = Arrays.asList(
            "BRONZE", "SILVER", "GOLD", "PLATINUM");

    public TierService(TierConfigRepository tierConfigRepository) {
        this.tierConfigRepository = tierConfigRepository;
    }

    /**
     * Tính hạng dựa trên điểm, ưu tiên đọc từ DB.
     * Fallback về logic cứng nếu DB chưa có dữ liệu tier_config.
     */
    public String calcTier(int points) {
        List<TierConfig> eligible = tierConfigRepository.findEligibleTiers(points);
        if (!eligible.isEmpty()) {
            return eligible.get(0).getTier(); // đã ORDER BY minPoints DESC → lấy phần tử đầu
        }
        // Fallback an toàn khi bảng tier_config rỗng
        return fallbackCalcTier(points);
    }

    /**
     * Bonus điểm khi lên hạng, đọc từ tier_config.bonusPoints.
     */
    public int getTierUpBonus(String newTier) {
        return tierConfigRepository.findByTier(newTier)
                .map(TierConfig::getBonusPoints)
                .orElse(0);
    }

    /**
     * Tỷ lệ bonus % theo hạng hiện tại, đọc từ… logic nghiệp vụ hiện tại.
     * TODO: có thể chuyển thành cột bonus_percent trong tier_config sau này.
     */
    public int getBonusPercent(String tier) {
        switch (tier) {
            case "SILVER":   return 5;
            case "GOLD":     return 10;
            case "PLATINUM": return 20;
            default:         return 0;
        }
    }

    /**
     * Kiểm tra tier của customer có đủ điều kiện dùng voucher không.
     */
    public boolean isTierSufficient(String customerTier, String requiredTier) {
        int customerIdx = TIER_ORDER.indexOf(customerTier);
        int requiredIdx = TIER_ORDER.indexOf(requiredTier);
        if (customerIdx < 0 || requiredIdx < 0) return false;
        return customerIdx >= requiredIdx;
    }

    // ── Fallback khi DB trống ─────────────────────────────────────────────────
    private static String fallbackCalcTier(int points) {
        if (points >= 3000) return "PLATINUM";
        if (points >= 1500) return "GOLD";
        if (points >= 500)  return "SILVER";
        return "BRONZE";
    }
}