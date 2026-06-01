package com.doan.demo.controller;

import com.doan.demo.model.Feedback;
import com.doan.demo.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*") // cho phép app Android gọi
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @PostMapping
    public String create(@RequestBody Feedback feedback) {
        if (feedback.getNoi_dung() == null || feedback.getNoi_dung().trim().isEmpty()) {
            return "Nội dung không được để trống!";
        }
        feedbackRepository.save(feedback);
        return "Gửi góp ý thành công!";
    }

    @GetMapping
    public List<Feedback> getAll() {
        return feedbackRepository.findAll();
    }
    @GetMapping("/my")
    public List<Feedback> getMyFeedbacks(@RequestParam Long customerId) {
        return feedbackRepository.findByCustomerId(customerId);
    }

}