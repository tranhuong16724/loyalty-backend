package com.doan.demo.service;

import com.doan.demo.model.Customer;
import com.doan.demo.model.PointTransaction;
import com.doan.demo.repository.CustomerRepository;
import com.doan.demo.repository.PointTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Toàn bộ nghiệp vụ liên quan đến điểm thưởng.
 *
 * Trước đây logic này bị copy-paste ở 2 chỗ trong CustomerController
 * (addPoints và verifyVoucher đều có đoạn applyEarnPoints riêng).
 * Giờ chỉ còn một nơi duy nhất.
 */
@Service
public class PointService {

    private final CustomerRepository          customerRepository;
    private final PointTransactionRepository  pointTransactionRepository;
    private final TierService                 tierService;
    private final FcmService                  fcmService;

    public PointService(CustomerRepository customerRepository,
                        PointTransactionRepository pointTransactionRepository,
                        TierService tierService,
                        FcmService fcmService) {
        this.customerRepository         = customerRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.tierService                = tierService;
        this.fcmService                 = fcmService;
    }

    /**
     * Cộng điểm từ giao dịch mua hàng (dựa trên số tiền).
     *
     * @param customerId  ID khách hàng
     * @param amount      Số tiền mua (VNĐ)
     * @return            Kết quả dạng chuỗi
     */
    @Transactional
    public String earnFromPurchase(Long customerId, double amount) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        int basePoints = (int) (amount / 1000);
        return applyEarnPoints(c, basePoints,
                String.format("Mua hàng %.0fđ → +%d điểm", amount, basePoints));
    }

    /**
     * Cộng điểm theo nghiệp vụ bất kỳ (dùng voucher, deal, bonus...).
     * Tính thêm % bonus theo hạng, thưởng lên hạng, ghi transaction, gửi FCM.
     *
     * @return chuỗi tóm tắt kết quả
     */
    @Transactional
    public String applyEarnPoints(Customer c, int basePoints, String description) {
        int bonusPct    = tierService.getBonusPercent(c.getTier());
        int bonus       = (basePoints * bonusPct) / 100;
        int earned      = basePoints + bonus;
        String oldTier  = c.getTier();

        int newPoints   = c.getPoints() + earned;
        String newTier  = tierService.calcTier(newPoints);

        // Thưởng lên hạng
        int tierBonus = 0;
        if (!oldTier.equals(newTier)) {
            tierBonus = tierService.getTierUpBonus(newTier);
        }
        newPoints += tierBonus;

        // Cập nhật customer — setPoints() sẽ tự cập nhật tier qua calcTier cũ;
        // ta set tier lại từ TierService để đảm bảo đọc từ DB.
        c.setPoints(newPoints);
        c.setTier(tierService.calcTier(newPoints));
        customerRepository.save(c);

        // Ghi transaction
        PointTransaction tx = new PointTransaction();
        tx.setCustomerId(c.getId());
        tx.setPoints(earned + tierBonus);
        tx.setType("EARN");
        tx.setDescription(description
                + (tierBonus > 0 ? " | Thưởng lên hạng +" + tierBonus : ""));
        pointTransactionRepository.save(tx);

        // FCM: cập nhật điểm
        fcmService.sendNotification(c.getFcmToken(),
                "⭐ Điểm thưởng cập nhật!",
                "Bạn vừa nhận " + earned + " điểm. Tổng: " + c.getPoints() + " điểm.",
                Map.of("type", "POINTS_UPDATE", "newPoints", String.valueOf(c.getPoints())));

        // FCM: lên hạng
        if (!oldTier.equals(c.getTier())) {
            fcmService.sendNotification(c.getFcmToken(),
                    "🏆 Chúc mừng lên hạng!",
                    "Bạn vừa lên hạng " + c.getTierBadge()
                            + (tierBonus > 0 ? " và nhận thêm " + tierBonus + " điểm!" : "!"),
                    Map.of("type", "PROMO_ALERT",
                            "message", "🏆 Lên hạng " + c.getTierBadge()));
        }

        return "Đã cộng " + (earned + tierBonus)
                + " điểm. Tổng: " + c.getPoints()
                + " | Hạng: " + c.getTierBadge();
    }
}