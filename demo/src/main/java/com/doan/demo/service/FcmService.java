package com.doan.demo.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * FcmService — gửi Push Notification qua Firebase Cloud Messaging (FCM v1 API).
 *
 * ── So sánh với WebSocket (cũ) ───────────────────────────────────────────────
 * WebSocket: Server push tới app đang MỞ qua TCP connection.
 * FCM:       Server → Firebase → Google → thiết bị Android, kể cả khi app TẮT.
 *
 * ── Cấu hình bắt buộc ────────────────────────────────────────────────────────
 * 1. Tạo Firebase project tại https://console.firebase.google.com
 * 2. Vào Project Settings → Service Accounts → Generate new private key
 * 3. Đặt file JSON vào src/main/resources/firebase-service-account.json
 * 4. Thêm FCM_PROJECT_ID vào application.properties:
 *      fcm.project-id=YOUR_FIREBASE_PROJECT_ID
 *
 * ── Dependency (pom.xml) ─────────────────────────────────────────────────────
 *   <dependency>
 *     <groupId>com.google.auth</groupId>
 *     <artifactId>google-auth-library-oauth2-http</artifactId>
 *     <version>1.23.0</version>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.squareup.okhttp3</groupId>
 *     <artifactId>okhttp</artifactId>
 *     <version>4.12.0</version>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.google.code.gson</groupId>
 *     <artifactId>gson</artifactId>
 *   </dependency>
 * ────────────────────────────────────────────────────────────────────────────
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    // Đọc từ application.properties: fcm.project-id=xxx
    @org.springframework.beans.factory.annotation.Value("${fcm.project-id}")
    private String projectId;

    private static final String FCM_URL_TEMPLATE =
            "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private static final String SERVICE_ACCOUNT_PATH =
            "src/main/resources/firebase-service-account.json";

    private final OkHttpClient httpClient = new OkHttpClient();

    // ── Gửi tới 1 thiết bị (by FCM token) ────────────────────────────────────

    /**
     * Gửi Data Message tới 1 thiết bị cụ thể.
     * @param fcmToken  Token của thiết bị Android (lưu trong DB)
     * @param dataPayload  Map key-value String (type, message, newPoints, v.v.)
     */
    public void sendDataMessage(String fcmToken, Map<String, String> dataPayload) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM token is empty, skip sending.");
            return;
        }
        try {
            String accessToken = getAccessToken();
            String body = buildDataMessage(fcmToken, dataPayload);
            postToFcm(accessToken, body);
        } catch (IOException e) {
            log.error("FCM sendDataMessage failed: {}", e.getMessage());
        }
    }

    /**
     * Gửi Notification + Data Message tới 1 thiết bị.
     * Dùng khi muốn Android tự hiển thị notification kể cả khi app bị kill.
     */
    public void sendNotification(String fcmToken, String title, String body,
                                 Map<String, String> dataPayload) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM token is empty, skip sending.");
            return;
        }
        try {
            String accessToken = getAccessToken();
            String requestBody = buildNotificationMessage(fcmToken, title, body, dataPayload);
            postToFcm(accessToken, requestBody);
        } catch (IOException e) {
            log.error("FCM sendNotification failed: {}", e.getMessage());
        }
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private String buildDataMessage(String token, Map<String, String> data) {
        JsonObject dataObj = new JsonObject();
        data.forEach(dataObj::addProperty);

        JsonObject message = new JsonObject();
        message.addProperty("token", token);
        message.add("data", dataObj);

        JsonObject root = new JsonObject();
        root.add("message", message);
        return root.toString();
    }

    private String buildNotificationMessage(String token, String title, String body,
                                            Map<String, String> data) {
        JsonObject notifObj = new JsonObject();
        notifObj.addProperty("title", title);
        notifObj.addProperty("body", body);

        JsonObject dataObj = new JsonObject();
        if (data != null) data.forEach(dataObj::addProperty);

        JsonObject androidConfig = new JsonObject();

        androidConfig.addProperty("priority", "HIGH");

        JsonObject androidNotif = new JsonObject();
        androidNotif.addProperty("channel_id", "katiburger_channel");

        androidConfig.add("notification", androidNotif);

        JsonObject message = new JsonObject();
        message.addProperty("token", token);
        message.add("notification", notifObj);
        message.add("data", dataObj);
        message.add("android", androidConfig);

        JsonObject root = new JsonObject();
        root.add("message", message);
        return root.toString();
    }

    // ── HTTP call ─────────────────────────────────────────────────────────────

    private void postToFcm(String accessToken, String jsonBody) throws IOException {
        String url = String.format(FCM_URL_TEMPLATE, projectId);
        RequestBody body = RequestBody.create(jsonBody,
                MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("FCM sent successfully ✅");
            } else {
                log.error("FCM error: {} — {}", response.code(),
                        response.body() != null ? response.body().string() : "");
            }
        }
    }

    // ── OAuth2 Access Token từ Service Account ────────────────────────────────

    private String getAccessToken() throws IOException {
        InputStream serviceAccount;
        // Thử classpath trước (jar), fallback sang file system
        serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("firebase-service-account.json");
        if (serviceAccount == null) {
            serviceAccount = new FileInputStream(SERVICE_ACCOUNT_PATH);
        }
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(serviceAccount)
                .createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}
