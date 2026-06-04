package com.doan.demo.scheduler;

import com.doan.demo.model.WeeklyDeal;
import com.doan.demo.repository.WeeklyDealRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class WeeklyDealScheduler {

    @Autowired
    private WeeklyDealRepository weeklyDealRepository;

    @Scheduled(cron = "0 5 0 * * *")
    public void autoHideExpiredDeals() {
        LocalDate today = LocalDate.now();
        List<WeeklyDeal> activeDeals = weeklyDealRepository.findByActiveTrue();

        int hiddenCount = 0;
        for (WeeklyDeal deal : activeDeals) {
            if (deal.getExpiryDate() != null && deal.getExpiryDate().isBefore(today)) {
                deal.setActive(false);
                weeklyDealRepository.save(deal);
                hiddenCount++;
                System.out.println("[Scheduler] Ẩn deal hết hạn: [" + deal.getId() + "] "
                        + deal.getTitle() + " (hết hạn: " + deal.getExpiryDate() + ")");
            }
        }

        if (hiddenCount > 0) {
            System.out.println("[Scheduler] Đã ẩn " + hiddenCount + " deal hết hạn.");
        } else {
            System.out.println("[Scheduler] Không có deal nào hết hạn hôm nay (" + today + ").");
        }
    }
}