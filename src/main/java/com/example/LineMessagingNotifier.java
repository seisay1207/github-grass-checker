package com.example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * LINE Messaging APIを使ってBotからユーザーやグループにメッセージを送信するクラス。
 * 
 * 必要な環境変数:
 *   LINE_CHANNEL_ACCESS_TOKEN: チャネルアクセストークン
 *   LINE_USER_ID: 送り先ユーザーID（グループIDも可）
 *
 * 参考: https://developers.line.biz/ja/reference/messaging-api/#send-push-message
 */
public class LineMessagingNotifier {
    private static final Logger logger = LoggerFactory.getLogger(LineMessagingNotifier.class);
    private static final String LINE_PUSH_URL = "https://api.line.me/v2/bot/message/push";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String channelAccessToken;
    private final String toId;

    /**
     * @param channelAccessToken LINEチャネルアクセストークン
     * @param toId 送り先ユーザーIDまたはグループID
     */
    public LineMessagingNotifier(String channelAccessToken, String toId) {
        this.channelAccessToken = channelAccessToken;
        this.toId = toId;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 任意のテキストメッセージを送信
     * @param message 送信するメッセージ
     * @return 送信成功でtrue
     */
    public boolean sendMessage(String message) {
        String json = String.format(
            "{\"to\":\"%s\",\"messages\":[{\"type\":\"text\",\"text\":\"%s\"}]}",
            toId, escapeJson(message)
        );
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(LINE_PUSH_URL)
                .addHeader("Authorization", "Bearer " + channelAccessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (logger.isInfoEnabled()) {
                    logger.info("LINE Messaging APIで通知を送信しました: {}", message);
                }
                return true;
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("LINE Messaging API通知の送信に失敗: HTTP {}", response.code());
                    if (response.body() != null) {
                        logger.error("エラー詳細: {}", response.body().string());
                    }
                }
                return false;
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("LINE Messaging API送信中にエラー: {}", e.getMessage(), e);
            }
            return false;
        }
    }

    /**
     * Contribution状況の通知メッセージを送信
     */
    public boolean sendContributionNotification(String username, GitHubContributionChecker.ContributionInfo info) {
        String message = buildContributionMessage(username, info);
        return sendMessage(message);
    }

    private String buildContributionMessage(String username, GitHubContributionChecker.ContributionInfo info) {
        StringBuilder message = new StringBuilder();
        
        // 現在の日時を取得
        java.time.LocalDate today = java.time.LocalDate.now();
        String todayStr = today.format(java.time.format.DateTimeFormatter.ofPattern("M月d日"));
        
        if (info.hasContribution()) {
            message.append("🌱 GitHub草チェック結果 ").append(todayStr).append("\n");
            message.append("━━━━━━━━━━━━━━━━━━━━\n");
            message.append("👤 ").append(username).append("\n");
            message.append("✅ 今日は草が生えています！\n");
            message.append("📊 Contribution数: ").append(info.getContributionCount()).append("件\n");
            if (info.getStreakDays() > 0) {
                message.append("🔥 継続日数: ").append(info.getStreakDays()).append("日連続！\n");
                if (info.getStreakDays() >= 7) {
                    message.append("🎉 素晴らしい継続力です！");
                } else if (info.getStreakDays() >= 3) {
                    message.append("💪 頑張っていますね！");
                }
            }
        } else {
            message.append("🌱 GitHub草チェック結果 ").append(todayStr).append("\n");
            message.append("━━━━━━━━━━━━━━━━━━━━\n");
            message.append("👤 ").append(username).append("\n");
            message.append("❌ 今日は草が生えていません\n");
            if (info.getStreakDays() > 0) {
                message.append("💔 継続記録が途切れます\n");
                message.append("📈 これまでの継続日数: ").append(info.getStreakDays()).append("日\n");
                message.append("💡 今すぐ草を生やしましょう！");
            } else {
                message.append("💡 今日から草を生やし始めましょう！\n");
                message.append("🚀 小さな一歩から始めることが大切です");
            }
        }
        
        return message.toString();
    }

    // JSONエスケープ（最低限）
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
} 