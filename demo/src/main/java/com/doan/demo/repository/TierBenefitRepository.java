package com.doan.demo.repository;
import com.doan.demo.model.TierBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TierBenefitRepository extends JpaRepository<TierBenefit, Long> {
    /** Lấy ưu đãi active của 1 hạng */
    List<TierBenefit> findByTierAndActiveTrueOrderByIdAsc(String tier);

    /** Lấy ưu đãi của nhiều hạng — dùng cho Vàng (cần cả BAC + VANG) */
    List<TierBenefit> findByTierInAndActiveTrueOrderByTierAscIdAsc(List<String> tiers);
}