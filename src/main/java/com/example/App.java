package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GitHub Grass Checker - メインアプリケーション
 * 
 * <p>GitHubのContribution（草）をチェックし、結果をログ出力し、必要に応じてLINE通知を送信するメインクラスです。</p>
 * 
 * <h3>使用方法</h3>
 * <p>以下の環境変数を設定して実行してください：</p>
 * <ul>
 *   <li><code>GITHUB_TOKEN</code> - GitHub Personal Access Token（必須）</li>
 *   <li><code>GITHUB_USERNAME</code> - チェック対象のGitHubユーザー名（必須）</li>
 *   <li><code>LINE_CHANNEL_ACCESS_TOKEN</code> - LINE Messaging APIチャネルアクセストークン（オプション）</li>
 *   <li><code>LINE_USER_ID</code> - 送り先ユーザーID（オプション）</li>
 * </ul>
 * 
 * <p>または、プロジェクトルートに<code>.env</code>ファイルを作成して設定することもできます。</p>
 * 
 * <p>AWS Lambda環境では、AWS Systems Manager Parameter Storeから設定を自動的に読み込みます。</p>
 * 
 * <h3>動作</h3>
 * <ul>
 *   <li>Contributionがある場合：ログ出力のみ</li>
 *   <li>Contributionがない場合：ログ出力 + LINE通知（LINE Messaging APIトークンが設定されている場合）</li>
 * </ul>
 * 
 * <h3>出力例</h3>
 * <pre>
 * ✅ 今日はContributionがあります！草が生えています。
 * 📊 今日のContribution数: 1件
 * 🔥 継続日数: 1日
 * </pre>
 * 
 * @author GitHub Grass Checker Team
 * @since 1.0
 */
public class App 
{
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    /**
     * AWS Lambda環境かどうかを判定
     */
    private static boolean isAwsLambda() {
        return System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null;
    }
    
    /**
     * .envファイルから環境変数を読み込む
     */
    private static void loadEnvFile() {
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try {
                Map<String, String> envVars = new HashMap<>();
                Files.lines(envPath)
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            envVars.put(key, value);
                        }
                    });
                
                // 環境変数が設定されていない場合のみ.envファイルの値を設定
                envVars.forEach((key, value) -> {
                    if (System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                });
                
                if (logger.isInfoEnabled()) {
                    logger.info(".envファイルを読み込みました");
                }
            } catch (IOException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(".envファイルの読み込みに失敗しました: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 環境変数またはシステムプロパティから値を取得
     */
    private static String getEnvOrProperty(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(key);
        }
        return value;
    }
    
    /**
     * 設定値を取得（AWS環境またはローカル環境）
     */
    private static ConfigValues getConfigValues() {
        ConfigValues config = new ConfigValues();
        
        if (isAwsLambda()) {
            // AWS Lambda環境ではParameter Storeから取得
            try {
                AwsConfigManager awsConfig = new AwsConfigManager();
                config.githubToken = awsConfig.getGitHubToken();
                config.githubUsername = awsConfig.getGitHubUsername();
                config.lineChannelAccessToken = awsConfig.getLineChannelAccessToken();
                config.lineUserId = awsConfig.getLineUserId();
                
                if (logger.isInfoEnabled()) {
                    logger.info("AWS Parameter Storeから設定を読み込みました");
                }
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("AWS Parameter Storeからの設定読み込みに失敗しました", e);
                }
            }
        } else {
            // ローカル環境では.envファイルまたは環境変数から取得
            loadEnvFile();
            config.githubToken = getEnvOrProperty("GITHUB_TOKEN");
            config.githubUsername = getEnvOrProperty("GITHUB_USERNAME");
            config.lineChannelAccessToken = getEnvOrProperty("LINE_CHANNEL_ACCESS_TOKEN");
            config.lineUserId = getEnvOrProperty("LINE_USER_ID");
            
            if (logger.isInfoEnabled()) {
                logger.info("ローカル環境から設定を読み込みました");
            }
        }
        
        return config;
    }
    
    /**
     * 設定値を保持する内部クラス
     */
    private static final class ConfigValues {
        String githubToken;
        String githubUsername;
        String lineChannelAccessToken;
        String lineUserId;
    }
    
    /**
     * メインエントリーポイント
     * 
     * <p>環境変数からGitHub Tokenとユーザー名を取得し、
     * 今日のContribution状況をチェックして結果を出力し、
     * 必要に応じてLINE通知を送信します。</p>
     * 
     * @param args コマンドライン引数（使用しません）
     */
    public static void main( String[] args )
    {
        // 設定値を取得
        ConfigValues config = getConfigValues();
        
        // 環境変数の存在チェック
        if (config.githubToken == null || config.githubToken.isEmpty()) {
            if (logger.isErrorEnabled()) {
                logger.error("GITHUB_TOKENが設定されていません");
            }
            System.exit(1);
        }
        
        if (config.githubUsername == null || config.githubUsername.isEmpty()) {
            if (logger.isErrorEnabled()) {
                logger.error("GITHUB_USERNAMEが設定されていません");
            }
            System.exit(1);
        }
        
        // GitHubContributionCheckerを初期化
        GitHubContributionChecker checker = new GitHubContributionChecker(config.githubToken);
        
        // LINE Messaging Notifierを初期化（トークンとユーザーIDが設定されている場合のみ）
        LineMessagingNotifier lineNotifier = null;
        if (config.lineChannelAccessToken != null && !config.lineChannelAccessToken.isEmpty() && 
            config.lineUserId != null && !config.lineUserId.isEmpty()) {
            lineNotifier = new LineMessagingNotifier(config.lineChannelAccessToken, config.lineUserId);
            if (logger.isInfoEnabled()) {
                logger.info("LINE Messaging API通知機能が有効です");
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("LINE_CHANNEL_ACCESS_TOKENまたはLINE_USER_IDが設定されていないため、LINE通知機能は無効です");
            }
        }
        
        if (logger.isInfoEnabled()) {
            logger.info("GitHubユーザー '{}' の今日のContributionをチェックしています...", config.githubUsername);
        }
        
        // Contribution情報を取得（継続日数含む）
        GitHubContributionChecker.ContributionInfo info;
        boolean tokenError = false;
        
        try {
            info = checker.getContributionInfo(config.githubUsername);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof GitHubContributionChecker.GitHubTokenException) {
                // GitHubトークンエラーの場合
                tokenError = true;
                info = new GitHubContributionChecker.ContributionInfo(false, 0, 0);
                
                if (logger.isErrorEnabled()) {
                    logger.error("GitHubトークンが無効です: {}", e.getCause().getMessage());
                }
            } else {
                // その他のエラーの場合
                info = new GitHubContributionChecker.ContributionInfo(false, 0, 0);
                
                if (logger.isErrorEnabled()) {
                    logger.error("Contribution情報の取得に失敗しました: {}", e.getMessage());
                }
            }
        }
        
        // 結果を出力
        if (info.hasContribution()) {
            if (logger.isInfoEnabled()) {
                logger.info("✅ 今日はContributionがあります！草が生えています。");
                logger.info("📊 今日のContribution数: {}件", info.getContributionCount());
                if (info.getStreakDays() > 0) {
                    logger.info("🔥 継続日数: {}日", info.getStreakDays());
                }
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("❌ 今日はContributionがありません。草が生えていません。");
                if (info.getStreakDays() > 0) {
                    logger.warn("💔 継続記録が途切れます。現在の継続日数: {}日", info.getStreakDays());
                }
            }
        }
        
        // 常にLINE通知を送信（Contributionの有無に関係なく）
        if (lineNotifier != null) {
            if (logger.isInfoEnabled()) {
                logger.info("LINE通知を送信しています...");
            }
            
            boolean notificationSent;
            
            if (tokenError) {
                // GitHubトークンエラーの場合は専用の通知を送信
                notificationSent = lineNotifier.sendTokenErrorNotification(config.githubUsername);
            } else {
                // 通常のContribution通知を送信
                notificationSent = lineNotifier.sendContributionNotification(config.githubUsername, info);
            }
            
            if (notificationSent) {
                if (logger.isInfoEnabled()) {
                    logger.info("LINE通知の送信が完了しました");
                }
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("LINE通知の送信に失敗しました");
                }
            }
        }
    }
}
