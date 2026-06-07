package com.doan.demo.service;

import com.doan.demo.model.TierConfig;
import com.doan.demo.repository.TierConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service
public class TierService {

    private final TierConfigRepository tierConfigRepository;

    private static final List<String> TIER_ORDER = Arrays.asList(
            "BRONZE", "SILVER", "GOLD", "PLATINUM");

    public TierService(TierConfigRepository tierConfigRepository) {
        this.tierConfigRepository = tierConfigRepository;
    }

    public String calcTier(int points) {
        List<TierConfig> eligible = tierConfigRepository.findEligibleTiers(points);
        if (!eligible.isEmpty()) {
            return eligible.get(0).getTier(); // đã ORDER BY minPoints DESC → lấy phần tử đầu
        }
        // Fallback an toàn khi bảng tier_config rỗng
        return fallbackCalcTier(points);
    }


    public int getTierUpBonus(String newTier) {
        return tierConfigRepository.findByTier(newTier)
                .map(TierConfig::getBonusPoints)
                .orElse(0);
    }

    public int getBonusPercent(String tier) {
        switch (tier) {
            case "SILVER":   return 5;
            case "GOLD":     return 10;
            case "PLATINUM": return 20;
            default:         return 0;
        }
    }


    public boolean isTierSufficient(String customerTier, String requiredTier) {
        if (requiredTier == null || requiredTier.isEmpty()) return true;
        int customerIdx = TIER_ORDER.indexOf(customerTier != null ? customerTier.toUpperCase() : "BRONZE");
        int requiredIdx = TIER_ORDER.indexOf(requiredTier.toUpperCase());
        if (requiredIdx < 0) return true;
        if (customerIdx < 0) customerIdx = 0;
        return customerIdx >= requiredIdx;


//        int customerIdx = TIER_ORDER.indexOf(customerTier);
//        int requiredIdx = TIER_ORDER.indexOf(requiredTier);
//        if (customerIdx < 0 || requiredIdx < 0) return false;
//        return customerIdx >= requiredIdx;
    }

    private static String fallbackCalcTier(int points) {
        if (points >= 3000) return "PLATINUM";
        if (points >= 1500) return "GOLD";
        if (points >= 500)  return "SILVER";
        return "BRONZE";
    }
}