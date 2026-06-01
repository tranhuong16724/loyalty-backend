package com.doan.demo.controller;

import com.doan.demo.model.*;
import com.doan.demo.repository.*;
import com.doan.demo.security.JwtUtil;
import com.doan.demo.service.FcmService;
import com.doan.demo.service.OtpService;
import com.doan.demo.service.PointService;
import com.doan.demo.service.TierService;
import com.doan.demo.service.VoucherService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository         customerRepository;
    private final VoucherRepository          voucherRepository;
    private final Voucher_UsageRepository    voucherUsageRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PasswordEncoder            passwordEncoder;
    private final JwtUtil                    jwtUtil;
    private final PointService               pointService;
    private final VoucherService             voucherService;
    private final TierService                tierService;
    private final OtpService                 otpService;
    private final FcmService                 fcmService;

    public CustomerController(CustomerRepository customerRepository,
                              VoucherRepository voucherRepository,
                              Voucher_UsageRepository voucherUsageRepository,
                              PointTransactionRepository pointTransactionRepository,
                              PasswordEncoder passwordEncoder,
                              JwtUtil jwtUtil,
                              PointService pointService,
                              VoucherService voucherService,
                              TierService tierService,
                              OtpService otpService,
                              FcmService fcmService) {
        this.customerRepository         = customerRepository;
        this.voucherRepository          = voucherRepository;
        this.voucherUsageRepository     = voucherUsageRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.passwordEncoder            = passwordEncoder;
        this.jwtUtil                    = jwtUtil;
        this.pointService               = pointService;
        this.voucherService             = voucherService;
        this.tierService                = tierService;
        this.otpService                 = otpService;
        this.fcmService                 = fcmService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Customer c) {
        if (customerRepository.findByPhoneNumber(c.getPhoneNumber()).isPresent())
            return ResponseEntity.badRequest().body("Số điện thoại này đã được đăng ký rồi!");
        if (c.getPassword() == null || c.getPassword().length() < 6)
            return ResponseEntity.badRequest().body("Mật khẩu phải từ 6 ký tự!");
        if (c.getEmail() != null && c.getEmail().trim().isEmpty()) {
            c.setEmail(null);
        }

        c.setPassword(passwordEncoder.encode(c.getPassword()));
        c.setPoints(0);
        c.setTier("BRONZE");
        customerRepository.save(c);
        return ResponseEntity.ok("Đăng ký thành công!");
    }

//    @PostMapping("/login")
//    public ResponseEntity<String> login(@RequestBody Customer loginInfo) {
//        String result = customerRepository
//                .findByPhoneNumber(loginInfo.getPhoneNumber())
//                .map(c -> {
//                    if (c.getPassword() == null)    return "Tài khoản chưa thiết lập mật khẩu!";
//                    if ("BLOCKED".equals(c.getStatus())) return "Tài khoản đã bị khóa!";
//                    if (passwordEncoder.matches(loginInfo.getPassword(), c.getPassword()))
//                        return "Đăng nhập thành công! Chào mừng " + c.getFullName();
//                    return "Sai mật khẩu rồi!";
//                })
//                .orElse("Số điện thoại này chưa đăng ký thành viên!");
//        return ResponseEntity.ok(result);
//    }

    @PostMapping("/loginV2")
    public ResponseEntity<Map<String, Object>> loginV2(@RequestBody Customer loginInfo) {
        Map<String, Object> res = new LinkedHashMap<>();
        Optional<Customer> opt = customerRepository.findByPhoneNumber(loginInfo.getPhoneNumber());

        if (opt.isEmpty()) {
            res.put("success", false);
            res.put("message", "Số điện thoại này chưa đăng ký thành viên!");
            return ResponseEntity.status(401).body(res);
        }
        Customer c = opt.get();
        if ("BLOCKED".equals(c.getStatus())) {
            res.put("success", false);
            res.put("message", "Tài khoản đã bị khóa!");
            return ResponseEntity.status(403).body(res); }
        if (c.getPassword() == null || !passwordEncoder.matches(loginInfo.getPassword(), c.getPassword())) {
            res.put("success", false);
            res.put("message", c.getPassword() == null ? "Tài khoản chưa thiết lập mật khẩu!" : "Sai mật khẩu rồi!");
            return ResponseEntity.status(401).body(res);
        }

        String correctTier = tierService.calcTier(c.getPoints());
        if (!correctTier.equals(c.getTier())) {
            c.setTier(correctTier);
            customerRepository.save(c);
        }

        String token = jwtUtil.generateToken(c.getId());
        res.put("success", true);
        res.put("message", "Đăng nhập thành công! Chào mừng " + c.getFullName());
        res.put("token",   token);
        res.put("id",      c.getId());
        res.put("name",    c.getFullName());
        res.put("phone",   c.getPhoneNumber());
        res.put("email",   c.getEmail());
        res.put("points",  c.getPoints());
        res.put("tier",    c.getTier());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<String> requestOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank())
            return ResponseEntity.badRequest().body("Thiếu số điện thoại!");

        Optional<Customer> opt = customerRepository.findByPhoneNumber(phone);
        if (opt.isEmpty())
            return ResponseEntity.badRequest().body("Không tìm thấy tài khoản với SĐT này!");

        String otp = otpService.generateOtp(phone);
        //System.out.println("OTP = " + otp);

        Customer c = opt.get();
        fcmService.sendNotification(c.getFcmToken(),
                "🔐 Mã xác thực đặt lại mật khẩu",
                "Mã OTP của bạn: " + otp + " (hiệu lực 5 phút)",
                Map.of("type", "OTP", "otp", otp));

        return ResponseEntity.ok("Đã gửi OTP thành công! Vui lòng kiểm tra tin nhắn của bạn.");
    }


    @PostMapping("/forgot-password/verify")
    public ResponseEntity<String> verifyOtpAndReset(@RequestBody Map<String, String> body) {
        String phone       = body.get("phone");
        String otp         = body.get("otp");
        String newPassword = body.get("newPassword");

        if (phone == null || otp == null || newPassword == null)
            return ResponseEntity.badRequest().body("Thiếu thông tin: phone, otp, newPassword!");
        if (newPassword.length() < 6)
            return ResponseEntity.badRequest().body("Mật khẩu mới phải từ 6 ký tự!");

        if (!otpService.verifyAndConsume(phone, otp))
            return ResponseEntity.status(400).body("OTP không đúng hoặc đã hết hạn!");

        return customerRepository.findByPhoneNumber(phone)
                .map(c -> {
                    c.setPassword(passwordEncoder.encode(newPassword));
                    customerRepository.save(c);
                    return ResponseEntity.ok("Đổi mật khẩu thành công!");
                })
                .orElse(ResponseEntity.badRequest().body("Không tìm thấy tài khoản!"));
    }



    @GetMapping("/search")
    public ResponseEntity<?> searchByPhone(@RequestParam String phone) {
        Customer c = customerRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng!"));
        String correct = tierService.calcTier(c.getPoints());
        if (!correct.equals(c.getTier())) { c.setTier(correct); customerRepository.save(c); }
        return ResponseEntity.ok(c);
    }

    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<String> updateFcmToken(@PathVariable Long id,
                                                 @RequestParam String token,
                                                 Authentication auth) {
        Long callerId = (Long) auth.getPrincipal();
        if (!callerId.equals(id))
            return ResponseEntity.status(403).body("Không có quyền thực hiện thao tác này!");

        return customerRepository.findById(id).map(c -> {
            c.setFcmToken(token.isBlank() ? null : token);
            customerRepository.save(c);
            return ResponseEntity.ok("FCM token updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/add-points")
    public ResponseEntity<String> addPoints(@RequestBody Map<String, Object> body,
                                            Authentication auth) {
        Long id     = Long.valueOf(body.get("id").toString());
        double amount = Double.parseDouble(body.get("amount").toString());

        Long callerId = (Long) auth.getPrincipal();
        if (!callerId.equals(id))
            return ResponseEntity.status(403).body("Không có quyền thực hiện thao tác này!");

        String result = pointService.earnFromPurchase(id, amount);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/redeem")
    public ResponseEntity<String> redeem(@RequestParam Long customerId,
                                         @RequestParam Long voucherId,
                                         Authentication auth) {
        Long callerId = (Long) auth.getPrincipal();
        if (!callerId.equals(customerId))
            return ResponseEntity.status(403).body("Không có quyền thực hiện thao tác này!");

        Map<String, Object> result = voucherService.redeemVoucher(customerId, voucherId);
        boolean success = Boolean.TRUE.equals(result.get("success"));
        String message  = (String) result.get("message");
        return success ? ResponseEntity.ok(message) : ResponseEntity.badRequest().body(message);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication auth) {
        Long callerId = (Long) auth.getPrincipal();
        List<VoucherUsage> usages = voucherUsageRepository.findByCustomerId(callerId);
        Map<Long, Voucher> vMap = new HashMap<>();
        voucherRepository.findAll().forEach(v -> vMap.put(v.getId(), v));

        List<Map<String, Object>> result = new ArrayList<>();
        for (VoucherUsage u : usages) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",          u.getId());
            row.put("customerId",  u.getCustomerId());
            row.put("voucherId",   u.getVoucherId());
            row.put("used_date",   u.getUsed_date());
            row.put("used",        u.isUsed());
            row.put("code",        u.getCode() != null ? u.getCode() : "");
            Voucher v = vMap.get(u.getVoucherId());
            row.put("voucherName", v != null ? v.getName() : "");
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }


    @GetMapping("/points-history")
    public ResponseEntity<?> getPointsHistory(@RequestParam Long customerId, Authentication auth) {
        Long callerId = (Long) auth.getPrincipal();
        if (!callerId.equals(customerId))
            return ResponseEntity.status(403).body("Không có quyền xem lịch sử này!");
        return ResponseEntity.ok(pointTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }

    @PostMapping("/use-voucher")
    public ResponseEntity<String> useVoucher(@RequestParam String code, Authentication auth) {
        Long callerId = (Long) auth.getPrincipal();
        Map<String, Object> result = voucherService.verifyCode(code, callerId);
        boolean success = Boolean.TRUE.equals(result.get("success"));
        String message  = (String) result.get("message");
        return success ? ResponseEntity.ok(message) : ResponseEntity.badRequest().body(message);
    }

    @PostMapping("/verify-voucher")
    public ResponseEntity<Map<String, Object>> verifyVoucher(@RequestParam String code,
                                                             Authentication auth) {
        Long callerId = (Long) auth.getPrincipal();
        try {
            Map<String, Object> result = voucherService.verifyCode(code, callerId);
            boolean success = Boolean.TRUE.equals(result.get("success"));
            return success ? ResponseEntity.ok(result) : ResponseEntity.status(403).body(result);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "❌ Lỗi xác nhận: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
    @GetMapping("/voucher-status")
    public ResponseEntity<Boolean> getVoucherStatus(
            @RequestParam Long usageId,
            Authentication auth) {

        Long callerId = (Long) auth.getPrincipal();

        Optional<VoucherUsage> opt =
                voucherUsageRepository.findById(usageId);

        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        VoucherUsage usage = opt.get();

        if (!usage.getCustomerId().equals(callerId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(usage.isUsed());
    }
}