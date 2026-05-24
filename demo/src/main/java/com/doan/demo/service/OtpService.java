package com.doan.demo.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP in-memory đơn giản cho luồng quên mật khẩu.
 *
 * Trong production nên dùng Redis để share OTP giữa nhiều pod.
 * Hiện tại lưu trong memory — restart server sẽ mất OTP đang pending.
 *
 * Luồng:
 *   1. POST /api/customers/forgot-password/request  { phone }
 *      → sinh 6 số, gửi SMS/FCM, lưu vào map với TTL 5 phút
 *   2. POST /api/customers/forgot-password/verify   { phone, otp, newPassword }
 *      → kiểm tra OTP còn hạn → đổi mật khẩu → xóa OTP
 */
@Service
public class OtpService {

    private static final int    OTP_LENGTH  = 6;
    private static final long   TTL_SECONDS = 5 * 60; // 5 phút

    private record OtpEntry(String otp, Instant expiresAt) {}

    // Key = phone number
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /** Sinh OTP mới, lưu và trả về (caller tự gửi cho user qua SMS/FCM). */
    public String generateOtp(String phone) {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) sb.append(random.nextInt(10));
        String otp = sb.toString();
        store.put(phone, new OtpEntry(otp, Instant.now().plusSeconds(TTL_SECONDS)));
        return otp;
    }

    /** Kiểm tra OTP. Trả true và xóa entry nếu đúng. */
    public boolean verifyAndConsume(String phone, String inputOtp) {
        OtpEntry entry = store.get(phone);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(phone);
            return false;
        }
        if (!entry.otp().equals(inputOtp)) return false;
        store.remove(phone);
        return true;
    }

    /** Hủy OTP nếu cần (ví dụ: user request lại). */
    public void invalidate(String phone) {
        store.remove(phone);
    }
}