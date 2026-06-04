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