package com.doan.demo.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.PathParam;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Server Endpoint cho Spring Boot

 * ── Nguyên lý hoạt động (Mạng máy tính & Truyền thông dữ liệu) ───────────
 * HTTP (stateless): Client gửi request → Server trả response → đóng kết nối
 * WebSocket (stateful): Client gửi HTTP Upgrade → Server đồng ý (101) →
 *   TCP connection duy trì → Server có thể PUSH dữ liệu bất kỳ lúc nào
 */
@ServerEndpoint("/ws/loyalty/{customerId}")
@Component
public class LoyaltyWebSocketServer {

    private static final Map<Long, Session> sessions = new ConcurrentHashMap<>();
    @OnOpen
    public void onOpen(Session session, @PathParam("customerId") Long customerId) {
        sessions.put(customerId, session);
        System.out.println("✅ WebSocket connected: customerId=" + customerId);
        sendToCustomer(customerId, "{\"type\":\"CONNECTED\",\"message\":\"Kết nối realtime thành công!\"}");
    }

    @OnMessage
    public void onMessage(String message, @PathParam("customerId") Long customerId) {
        System.out.println("📨 Message from " + customerId + ": " + message);
    }

    @OnClose
    public void onClose(@PathParam("customerId") Long customerId) {
        sessions.remove(customerId);
        System.out.println("❌ WebSocket closed: customerId=" + customerId);
    }

    @OnError
    public void onError(Throwable error, @PathParam("customerId") Long customerId) {
        System.err.println("⚠️ WebSocket error for " + customerId + ": " + error.getMessage());
        sessions.remove(customerId);
    }

    public static void pushPointsUpdate(Long customerId, int newPoints) {
        String msg = "{\"type\":\"POINTS_UPDATE\",\"newPoints\":" + newPoints + "}";
        sendToCustomer(customerId, msg);
    }

    public static void broadcastNewVoucher(String voucherName, Long voucherId) {
        String msg = "{\"type\":\"NEW_VOUCHER\",\"message\":\"Voucher mới: " + voucherName
                + "\",\"voucherId\":" + voucherId + "}";
        sessions.forEach((id, session) -> sendMsg(session, msg));
    }

    public static void broadcastPromoAlert(String message) {
        String msg = "{\"type\":\"PROMO_ALERT\",\"message\":\"" + message + "\"}";
        sessions.forEach((id, session) -> sendMsg(session, msg));
    }

    public static void pushVoucherUsed(Long customerId,  String code, String voucherName) {
        String msg = "{\"type\":\"TYPE_VOUCHER_USED\","
                + "\"code\":\"" + code + "\","
                + "\"voucherName\":\"" + voucherName + "\"}";
        sendToCustomer(customerId, msg);
    }
    public static void pushDealUsed(Long customerId, String code, String dealTitle) {

        String msg = "{" + "\"type\":\"DEAL_USED\","
                        + "\"code\":\"" + code + "\","
                        + "\"dealTitle\":\"" + dealTitle + "\"" + "}";

        sendToCustomer(customerId, msg);
    }
    private static void sendToCustomer(Long customerId, String message) {
        Session session = sessions.get(customerId);
        if (session != null && session.isOpen()) sendMsg(session, message);
    }

    private static void sendMsg(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            System.err.println("Failed to send WS message: " + e.getMessage());
        }
    }
    public static void pushNotification(
            Long customerId,
            String message) {

        String msg = "{"
                        + "\"type\":\"PROMO_ALERT\","
                        + "\"message\":\"" + message + "\""
                        + "}";

        sendToCustomer(customerId, msg);
    }
    public static int getActiveConnections() {
        return sessions.size();
    }
}