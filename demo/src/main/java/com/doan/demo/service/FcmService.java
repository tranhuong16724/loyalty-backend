package com.doan.demo.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;


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
        if (fcmToken == null || fcmToken.isBlank()) {// kiểm tra có token không
            log.warn("FCM token is empty, skip sending.");
            return;
        }
        try {
            //lấy quyền từ google
            String accessToken = getAccessToken();
            String body = buildDataMessage(fcmToken, dataPayload);
            postToFcm(accessToken, body);
        } catch (IOException e) {
            log.error("FCM sendDataMessage failed: {}", e.getMessage());
        }
    }

//hàm guiwrthoong báo
    public void sendNotification(String fcmToken, String title, String body,
                                 Map<String, String> dataPayload) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM token is empty, skip sending.");
            return;
        }
        try {
            String accessToken = getAccessToken();
            //tạo ội dung thông báo
            String requestBody = buildNotificationMessage(fcmToken, title, body, dataPayload);
            postToFcm(accessToken, requestBody);//gửi sang gogle
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
        androidNotif.addProperty("channel_id", "laura_channel");

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

        // Thử đọc từ biến môi trường trước (Railway)
        String jsonEnv = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (jsonEnv != null && !jsonEnv.isBlank()) {
            serviceAccount = new ByteArrayInputStream(jsonEnv.getBytes());
        } else {
            // Fallback: đọc từ file (chạy local)
            serviceAccount = getClass().getClassLoader()
                    .getResourceAsStream("firebase-service-account.json");
            if (serviceAccount == null) {
                serviceAccount = new FileInputStream("src/main/resources/firebase-service-account.json");
            }
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(serviceAccount)
                .createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}
