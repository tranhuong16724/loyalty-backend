package com.doan.demo.scheduler;

import com.doan.demo.model.WeeklyDeal;
import com.doan.demo.repository.WeeklyDealRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
public class DealExpiryScheduler {

    @Autowired
    private WeeklyDealRepository weeklyDealRepository;

    @Scheduled(fixedRate = 3_600_000)
    public void deactivateExpiredDeals() {
        LocalDate today = LocalDate.now();
        List<WeeklyDeal> activeDeals = weeklyDealRepository.findByActiveTrue();
        int count = 0;
        for (WeeklyDeal deal : activeDeals) {
            if (deal.getExpiryDate() != null && deal.getExpiryDate().isBefore(today)) {
                deal.setActive(false);
                weeklyDealRepository.save(deal);
                count++;
            }
        }
        if (count > 0)
            System.out.println("[DealExpiry] Tự động ẩn " + count + " ưu đãi hết hạn.");
    }
}