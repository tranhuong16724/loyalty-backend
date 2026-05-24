package com.doan.demo.controller;

import com.doan.demo.model.Feedback;
import com.doan.demo.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * API nhận góp ý từ Mobile App
 * POST http://localhost:8080/api/feedback
 * GET  http://localhost:8080/api/feedback  (Web Admin)
 */
@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*") // cho phép app Android gọi
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    /** Mobile App gửi góp ý lên */
    @PostMapping
    public String create(@RequestBody Feedback feedback) {
        if (feedback.getNoi_dung() == null || feedback.getNoi_dung().trim().isEmpty()) {
            return "Nội dung không được để trống!";
        }
        feedbackRepository.save(feedback);
        return "Gửi góp ý thành công!";
    }

    /** Web Admin xem tất cả góp ý */
    @GetMapping
    public List<Feedback> getAll() {
        return feedbackRepository.findAll();
    }
}