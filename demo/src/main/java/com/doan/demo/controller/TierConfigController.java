package com.doan.demo.controller;

import com.doan.demo.model.TierConfig;
import com.doan.demo.repository.TierConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trả về ngưỡng tier hiện tại từ DB cho Android App.
 *
 * Android gọi GET /api/tiers/config sau khi đăng nhập để đồng bộ
 * SessionManager.thresholdSilver/Gold/Platinum với giá trị thực trong DB.
 * Nhờ đó admin chỉ cần UPDATE bảng tier_config — app tự cập nhật ở lần
 * đăng nhập tiếp theo, không cần release bản app mới.
 *
 * Response: { "silver": 500, "gold": 1500, "platinum": 3000 }
 *
 * Endpoint yêu cầu JWT (được bảo vệ bởi SecurityConfig /api/** chain).
 */
@RestController
@RequestMapping("/api/tiers")
public class TierConfigController {

    private final TierConfigRepository tierConfigRepository;

    public TierConfigController(TierConfigRepository tierConfigRepository) {
        this.tierConfigRepository = tierConfigRepository;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Integer>> getTierConfig() {
        List<TierConfig> configs = tierConfigRepository.findAllByOrderByMinPointsAsc();

        // Giá trị mặc định — dùng khi DB chưa có dữ liệu tier_config
        int silver   = 500;
        int gold     = 1500;
        int platinum = 3000;

        for (TierConfig cfg : configs) {
            switch (cfg.getTier()) {
                case "SILVER":   silver   = cfg.getMinPoints(); break;
                case "GOLD":     gold     = cfg.getMinPoints(); break;
                case "PLATINUM": platinum = cfg.getMinPoints(); break;
            }
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("silver",   silver);
        result.put("gold",     gold);
        result.put("platinum", platinum);

        return ResponseEntity.ok(result);
    }
}