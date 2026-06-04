package com.doan.demo.repository;
import com.doan.demo.model.TierConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface TierConfigRepository extends JpaRepository<TierConfig, Long> {
    Optional<TierConfig> findByTier(String tier);

    List<TierConfig> findAllByOrderByMinPointsAsc();

    @Query("SELECT t FROM TierConfig t WHERE t.minPoints <= :points ORDER BY t.minPoints DESC")
    List<TierConfig> findEligibleTiers(int points);
}