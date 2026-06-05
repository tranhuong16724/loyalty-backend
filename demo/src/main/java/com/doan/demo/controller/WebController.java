package com.doan.demo.controller;

import com.doan.demo.model.*;
import com.doan.demo.repository.*;
import com.doan.demo.service.FcmService;
import com.doan.demo.service.PointService;
import com.doan.demo.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class WebController {

    @Autowired private CustomerRepository         customerRepository;
    @Autowired private Voucher_UsageRepository    voucherUsageRepository;
    @Autowired private VoucherRepository          voucherRepository;
    @Autowired private FeedbackRepository         feedbackRepository;
    @Autowired private PointLogRepository         pointLogRepository;
    @Autowired private WeeklyDealRepository       weeklyDealRepository;
    @Autowired private MenuItemRepository         menuItemRepository;
    @Autowired private PointTransactionRepository pointTransactionRepository;
    @Autowired private DealCodeRepository         dealCodeRepository;
    @Autowired private FcmService                 fcmService;
    @Autowired private VoucherService             voucherService;  // ← dùng thay doVerify()
    @Autowired private PointService               pointService;

    private static final int PAGE_SIZE_CUSTOMERS = 20;
    private static final int PAGE_SIZE_HISTORY   = 15;

    // ── Trang chủ ─────────────────────────────────────────────────────────────
    @GetMapping("/")
    public String home(
            @RequestParam(required = false, defaultValue = "")  String search,
            @RequestParam(required = false, defaultValue = "0")  int page,
            @RequestParam(required = false, defaultValue = "0")  int historyPage,
            Model model) {

        // ── Danh sách khách hàng — phân trang ────────────────────────────────
        Pageable customerPageable = PageRequest.of(
                Math.max(page, 0), PAGE_SIZE_CUSTOMERS, Sort.by("id").descending());

        Page<Customer> customerPage = search.isEmpty()
                ? customerRepository.findAll(customerPageable)
                : customerRepository.searchByKeywordPaged(search, customerPageable);

        // ── Thống kê hạng — dùng COUNT trên DB, không load toàn bộ record ────
        long bronzeCount = 0, silverCount = 0, goldCount = 0, platinumCount = 0;
        for (Object[] row : customerRepository.countByTier()) {
            String tier  = (String) row[0];
            long   count = (Long)   row[1];
            switch (tier) {
                case "BRONZE"   -> bronzeCount   = count;
                case "SILVER"   -> silverCount   = count;
                case "GOLD"     -> goldCount      = count;
                case "PLATINUM" -> platinumCount  = count;
            }
        }

        // ── Thống kê voucher — dùng GROUP BY thay vì findAll() ───────────────
        Map<Long, Long> voucherUsageMap = new HashMap<>();
        for (Object[] row : voucherUsageRepository.countGroupByVoucherId()) {
            voucherUsageMap.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, String> voucherNameMap = new HashMap<>();
        voucherRepository.findAll().forEach(v -> voucherNameMap.put(v.getId(), v.getName()));

        // ── Lịch sử đổi voucher — phân trang ────────────────────────────────
        Pageable historyPageable = PageRequest.of(
                Math.max(historyPage, 0), PAGE_SIZE_HISTORY, Sort.by("id").descending());
        Page<VoucherUsage> historyPage2 = voucherUsageRepository.findAll(historyPageable);

        model.addAttribute("voucherUsage",       voucherUsageMap);
        model.addAttribute("voucherNameMap",      voucherNameMap);
        model.addAttribute("bronzeCount",         bronzeCount);
        model.addAttribute("silverCount",         silverCount);
        model.addAttribute("goldCount",           goldCount);
        model.addAttribute("platinumCount",       platinumCount);
        model.addAttribute("customerPage",        customerPage);           // ← Page object
        model.addAttribute("customers",           customerPage.getContent());
        model.addAttribute("currentPage",         customerPage.getNumber());
        model.addAttribute("totalPages",          customerPage.getTotalPages());
        model.addAttribute("search",              search);
        model.addAttribute("history",             historyPage2.getContent());
        model.addAttribute("historyCurrentPage",  historyPage2.getNumber());
        model.addAttribute("historyTotalPages",   historyPage2.getTotalPages());
        model.addAttribute("vouchers",            voucherRepository.findAll());
        model.addAttribute("feedbacks",           feedbackRepository.findAll(
                PageRequest.of(0, 20, Sort.by("id").descending())).getContent());
        model.addAttribute("deals",               weeklyDealRepository.findAll());
        model.addAttribute("menus",               menuItemRepository.findAll());
        model.addAttribute("totalCustomers",      customerRepository.count());
        model.addAttribute("totalVouchers",       voucherRepository.count());
        model.addAttribute("totalRedeemed",       voucherUsageRepository.count());
        return "index";
    }

    // ── Chi tiết khách hàng ───────────────────────────────────────────────────
    @GetMapping("/customer/{id}")
    public String customerDetail(@PathVariable Long id, Model model) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng ID=" + id));
        model.addAttribute("customer",  customer);
        model.addAttribute("usages",    voucherUsageRepository.findByCustomerId(id));
        model.addAttribute("feedbacks", feedbackRepository.findByCustomerId(id));
        model.addAttribute("pointLogs", pointLogRepository.findByCustomerIdOrderByCreatedAtDesc(id));
        model.addAttribute("vouchers",  voucherRepository.findAll());
        return "customer_detail";
    }

    // ── Xóa khách hàng ────────────────────────────────────────────────────────
    @GetMapping("/block-customer/{id}")
    public String blockCustomer(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        customer.setStatus("BLOCKED");
        customerRepository.save(customer);
        return "redirect:/"; }

    // ── Thêm voucher ──────────────────────────────────────────────────────────
    @PostMapping("/add-voucher")
    public String addVoucher(
            @RequestParam String name,
            @RequestParam int points,
            @RequestParam(required = false, defaultValue = "") String minTier) {

        Voucher v = new Voucher();
        v.setName(name);
        v.setPointsRequired(points);
        if (!minTier.isBlank()) v.setMinTier(minTier.toUpperCase());
        Voucher saved = voucherRepository.save(v);

        // Broadcast FCM tới tất cả customer có token
        Map<String, String> data = new HashMap<>();
        data.put("type",        "NEW_VOUCHER");
        data.put("voucherName", saved.getName());
        data.put("voucherId",   String.valueOf(saved.getId()));
        broadcastFcm(data);

        return "redirect:/";
    }

    // ── Xóa voucher ───────────────────────────────────────────────────────────
    @GetMapping("/delete-voucher/{id}")
    public String deleteVoucher(@PathVariable Long id) {
        voucherRepository.deleteById(id);
        return "redirect:/";
    }

    // ── Điều chỉnh điểm ──────────────────────────────────────────────────────
    @PostMapping("/adjust-points")
    public String adjustPoints(
            @RequestParam Long customerId,
            @RequestParam int delta,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam(defaultValue = "") String redirect) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        String oldTier  = customer.getTier();
        int newPoints   = Math.max(0, customer.getPoints() + delta);
        customer.setPoints(newPoints);
        customerRepository.save(customer);

        PointLog log = new PointLog();
        log.setCustomerId(customerId);
        log.setDelta(delta);
        log.setReason(reason.isEmpty() ? "Admin điều chỉnh" : reason);
        log.setCreatedAt(LocalDateTime.now());
        pointLogRepository.save(log);

        PointTransaction tx = new PointTransaction();
        tx.setCustomerId(customerId);
        tx.setPoints(delta);
        tx.setType(delta >= 0 ? "EARN" : "REDEEM");
        tx.setDescription(delta >= 0 ? "Admin tặng điểm: " + reason : "Admin trừ điểm: " + reason);
        pointTransactionRepository.save(tx);

        // Gửi FCM cập nhật điểm
        String token = customer.getFcmToken();
        Map<String, String> data = new HashMap<>();
        data.put("type",      "POINTS_UPDATE");
        data.put("newPoints", String.valueOf(newPoints));
        fcmService.sendDataMessage(token, data);

        // Nếu lên hạng → gửi thêm thông báo
        if (!oldTier.equals(customer.getTier())) {
            Map<String, String> tierData = new HashMap<>();
            tierData.put("type",    "PROMO_ALERT");
            tierData.put("message", "🏆 Chúc mừng! Bạn vừa lên hạng " + customer.getTierBadge() + "!");
            fcmService.sendDataMessage(token, tierData);
        }

        return "detail".equals(redirect)
                ? "redirect:/customer/" + customerId
                : "redirect:/";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XÁC NHẬN MÃ QR / VOUCHER
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/verify-voucher")
    public String verifyVoucher(@RequestParam String code, Model model) {
        Map<String, Object> r = voucherService.verifyCode(code, null); // null = Web admin, bỏ qua kiểm tra sở hữu
        boolean ok = Boolean.TRUE.equals(r.get("success"));
        model.addAttribute(ok ? "verifySuccess" : "verifyError", r.get("message"));
        return home("", 0, 0, model);
    }

    @PostMapping("/api/verify-json")
    @ResponseBody
    public Map<String, Object> verifyJson(@RequestParam String code) {
        return voucherService.verifyCode(code, null);
    }

    // ── Tích điểm từ QR cá nhân của khách hàng ───────────────────────────────
    @PostMapping("/api/scan-qr")
    @ResponseBody
    public Map<String, Object> scanQr(
            @RequestParam String code,
            @RequestParam double amount) {

        Map<String, Object> res = new LinkedHashMap<>();
        try {
            // Chuẩn hóa: bỏ khoảng trắng, uppercase
            // "M 042 250" → "M042250"
            String normalized = code.trim().toUpperCase().replaceAll("\\s+", "");

            if (!normalized.matches("M\\d{6}")) {
                res.put("success", false);
                res.put("message", "❌ Mã không hợp lệ! Định dạng đúng: M XXXXXX (6 chữ số)");
                return res;
            }

            long displayCode = Long.parseLong(normalized.substring(1));
            long now         = System.currentTimeMillis() / (5 * 60 * 1000L);
            final long MOD   = 1_000_000L;

            // Thử window hiện tại và window trước (phòng mã vừa đổi)
            for (long w : new long[]{now, now - 1}) {
                long wMod = w % MOD;
                for (long tryId = 1; tryId <= 10000; tryId++) {
                    long p1  = (tryId * 13337L) % MOD;
                    long p2  = wMod * (999983L % MOD) % MOD;
                    long raw = (p1 + p2) % MOD;
                    if (raw == displayCode) {
                        return processEarnPoints(tryId, amount, res);
                    }
                }
            }

            res.put("success", false);
            res.put("message", "❌ Mã không hợp lệ hoặc đã hết hạn! Yêu cầu khách mở lại app.");
            return res;

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "❌ Lỗi hệ thống: " + e.getMessage());
            return res;
        }
    }

    // Tách logic xử lý điểm — dùng chung cho cả 2 luồng
    private Map<String, Object> processEarnPoints(long customerId,
                                                  double amount,
                                                  Map<String, Object> res) {
        if (amount < 1000) {
            res.put("success", false);
            res.put("message", "❌ Số tiền tối thiểu 1.000đ!");
            return res;
        }
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            res.put("success", false);
            res.put("message", "❌ Không tìm thấy khách hàng!");
            return res;
        }
        if ("BLOCKED".equals(customer.getStatus())) {
            res.put("success", false);
            res.put("message", "🔒 Tài khoản khách hàng đang bị khóa!");
            return res;
        }

        String oldTier = customer.getTier();
        pointService.earnFromPurchase(customerId, amount);
        customer = customerRepository.findById(customerId).orElse(customer);

        res.put("success",      true);
        res.put("message",      "✅ Tích điểm thành công!");
        res.put("customerName", customer.getFullName());
        res.put("customerId",   customerId);
        res.put("newPoints",    customer.getPoints());
        res.put("tier",         customer.getTierBadge());
        res.put("tierChanged",  !oldTier.equals(customer.getTier()));
        return res;
    }

    // ── Thêm ưu đãi tuần ─────────────────────────────────────────────────────
    @PostMapping("/add-deal")
    public String addDeal(
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false, defaultValue = "") String originalPrice,
            @RequestParam(required = false, defaultValue = "") String discountPrice,
            @RequestParam(required = false, defaultValue = "") String expiryDate,
            @RequestParam(required = false, defaultValue = "MULTI_DAY") String dealType,
            @RequestParam(required = false) Integer allowedDayOfWeek) {

        WeeklyDeal deal = new WeeklyDeal();
        deal.setTitle(title);
        deal.setDescription(description.isEmpty()     ? null : description);
        deal.setOriginalPrice(originalPrice.isEmpty() ? null : originalPrice);
        deal.setDiscountPrice(discountPrice.isEmpty() ? null : discountPrice);
        if (!expiryDate.isEmpty()) {
            try { deal.setExpiryDate(LocalDate.parse(expiryDate)); } catch (Exception ignored) {}
        }
        deal.setDealType(dealType);
        deal.setAllowedDayOfWeek("FIXED_DAY".equals(dealType) ? allowedDayOfWeek : null);
        deal.setActive(true);
        weeklyDealRepository.save(deal);

        String msg = "🔥 Ưu đãi mới: " + title
                + (discountPrice.isEmpty() ? "" : " – " + discountPrice);
        Map<String, String> data = new HashMap<>();
        data.put("type",    "PROMO_ALERT");
        data.put("message", msg);
        broadcastFcm(data);

        return "redirect:/";
    }

    // ── Menu ──────────────────────────────────────────────────────────────────
    @PostMapping("/add-menu")
    public String addMenu(
            @RequestParam String name,
            @RequestParam(defaultValue = "") String price,
            @RequestParam(defaultValue = "") String emoji,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String badge,
            @RequestParam(defaultValue = "") String imageUrl) {

        MenuItem item = new MenuItem();
        item.setName(name);
        item.setPrice(price.isEmpty()       ? null       : price);
        item.setEmoji(emoji.isEmpty()       ? "🍽️"      : emoji);
        item.setCategory(category.isEmpty() ? "Món khác" : category);
        item.setBadge(badge.isEmpty()       ? null       : badge);
        item.setImageUrl(imageUrl.isEmpty() ? null       : imageUrl);  // ← THÊM DÒNG NÀY
        item.setActive(true);
        menuItemRepository.save(item);

        Map<String, String> data = new HashMap<>();
        data.put("type",    "PROMO_ALERT");
        data.put("message", "📋 Menu vừa được cập nhật!");
        broadcastFcm(data);

        return "redirect:/";
    }

    @GetMapping("/toggle-menu/{id}")
    public String toggleMenu(@PathVariable Long id) {
        menuItemRepository.findById(id).ifPresent(m -> {
            m.setActive(!m.isActive());
            menuItemRepository.save(m);
        });
        Map<String, String> data = new HashMap<>();
        data.put("type",    "PROMO_ALERT");
        data.put("message", "📋 Menu vừa được cập nhật!");
        broadcastFcm(data);
        return "redirect:/";
    }

    @GetMapping("/delete-menu/{id}")
    public String deleteMenu(@PathVariable Long id) {
        menuItemRepository.deleteById(id);
        Map<String, String> data = new HashMap<>();
        data.put("type",    "PROMO_ALERT");
        data.put("message", "📋 Menu vừa được cập nhật!");
        broadcastFcm(data);
        return "redirect:/";
    }

    // ── Deal ──────────────────────────────────────────────────────────────────
    @GetMapping("/delete-deal/{id}")
    public String deleteDeal(@PathVariable Long id) {
        weeklyDealRepository.deleteById(id);
        return "redirect:/";
    }

    @GetMapping("/toggle-deal/{id}")
    public String toggleDeal(@PathVariable Long id) {
        weeklyDealRepository.findById(id).ifPresent(deal -> {
            deal.setActive(!deal.isActive());
            weeklyDealRepository.save(deal);
        });
        return "redirect:/";
    }

    // ── Helper: broadcast FCM — chỉ SELECT fcm_token, không load toàn bộ Customer ──
    private void broadcastFcm(Map<String, String> data) {
        customerRepository.findAllFcmTokens().forEach(token ->
                fcmService.sendDataMessage(token, data));
    }
}