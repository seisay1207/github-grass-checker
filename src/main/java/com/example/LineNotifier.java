package com.example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * LINE Notify APIを使用して通知を送信するクラス
 * 
 * <p>GitHubのContribution状況をLINEに通知するためのクラスです。
 * LINE Notify APIを使用して、指定されたトークンにメッセージを送信します。</p>
 * 
 * <h3>使用方法</h3>
 * <p>LINE Notifyのトークンを取得し、環境変数<code>LINE_NOTIFY_TOKEN</code>に設定してください。</p>
 * 
 * <h3>LINE Notifyトークンの取得方法</h3>
 * <ol>
 *   <li><a href="https://notify-bot.line.me/">LINE Notify</a>にアクセス</li>
 *   <li>LINEアカウントでログイン</li>
 *   <li>「マイページ」→「トークンを発行する」</li>
 *   <li>トークン名を入力して発行</li>
 *   <li>発行されたトークンをコピー</li>
 * </ol>
 * 
 * @author GitHub Grass Checker Team
 * @since 1.0
 */
public class LineNotifier {
    private static final Logger logger = LoggerFactory.getLogger(LineNotifier.class);
    private static final String LINE_NOTIFY_URL = "https://notify-api.line.me/api/notify";
    
    private final OkHttpClient httpClient;
    private final String lineToken;
    
    /**
     * LineNotifierを初期化します
     * 
     * @param lineToken LINE Notify APIトークン
     */
    public LineNotifier(String lineToken) {
        this.lineToken = lineToken;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * メッセージをLINEに送信します
     * 
     * @param message 送信するメッセージ
     * @return 送信が成功した場合はtrue、失敗した場合はfalse
     */
    public boolean sendMessage(String message) {
        try {
            FormBody formBody = new FormBody.Builder()
                    .add("message", message)
                    .build();
            
            Request request = new Request.Builder()
                    .url(LINE_NOTIFY_URL)
                    .addHeader("Authorization", "Bearer " + lineToken)
                    .post(formBody)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("LINE通知を送信しました: {}", message);
                    }
                    return true;
                } else {
                    if (logger.isErrorEnabled()) {
                        logger.error("LINE通知の送信に失敗しました: HTTP {}", response.code());
                        if (response.body() != null) {
                            logger.error("エラー詳細: {}", response.body().string());
                        }
                    }
                    return false;
                }
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("LINE通知送信中にエラーが発生しました: {}", e.getMessage(), e);
            }
            return false;
        }
    }
    
    /**
     * GitHub Contribution状況の通知メッセージを送信します
     * 
     * @param username GitHubユーザー名
     * @param info Contribution情報
     * @return 送信が成功した場合はtrue、失敗した場合はfalse
     */
    public boolean sendContributionNotification(String username, GitHubContributionChecker.ContributionInfo info) {
        String message = buildContributionMessage(username, info);
        return sendMessage(message);
    }
    
    /**
     * Contribution状況に基づいて通知メッセージを構築します
     * 
     * @param username GitHubユーザー名
     * @param info Contribution情報
     * @return 通知メッセージ
     */
    private String buildContributionMessage(String username, GitHubContributionChecker.ContributionInfo info) {
        StringBuilder message = new StringBuilder();
        
        if (info.hasContribution()) {
            // Contributionがある場合
            message.append("✅ GitHub Contribution チェック結果\n");
            message.append("ユーザー: ").append(username).append("\n");
            message.append("今日はContributionがあります！草が生えています。\n");
            message.append("📊 今日のContribution数: ").append(info.getContributionCount()).append("件\n");
            
            if (info.getStreakDays() > 0) {
                message.append("🔥 継続日数: ").append(info.getStreakDays()).append("日");
            }
        } else {
            // Contributionがない場合
            message.append("❌ GitHub Contribution チェック結果\n");
            message.append("ユーザー: ").append(username).append("\n");
            message.append("今日はContributionがありません。草が生えていません。\n");
            
            if (info.getStreakDays() > 0) {
                message.append("💔 継続記録が途切れます。現在の継続日数: ").append(info.getStreakDays()).append("日");
            } else {
                message.append("💔 継続記録はありません");
            }
        }
        
        return message.toString();
    }
} 