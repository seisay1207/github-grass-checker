package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GitHub Grass Checker - メインアプリケーション
 * 
 * <p>GitHubのContribution（草）をチェックし、結果をログ出力するメインクラスです。</p>
 * 
 * <h3>使用方法</h3>
 * <p>以下の環境変数を設定して実行してください：</p>
 * <ul>
 *   <li><code>GITHUB_TOKEN</code> - GitHub Personal Access Token</li>
 *   <li><code>GITHUB_USERNAME</code> - チェック対象のGitHubユーザー名</li>
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
     * メインエントリーポイント
     * 
     * <p>環境変数からGitHub Tokenとユーザー名を取得し、
     * 今日のContribution状況をチェックして結果を出力します。</p>
     * 
     * @param args コマンドライン引数（使用しません）
     */
    public static void main( String[] args )
    {
        // GitHub Tokenは環境変数から取得することを推奨
        String githubToken = System.getenv("GITHUB_TOKEN");
        String username = System.getenv("GITHUB_USERNAME");
        
        // 環境変数の存在チェック
        if (githubToken == null || githubToken.isEmpty()) {
            if (logger.isErrorEnabled()) {
                logger.error("GITHUB_TOKEN環境変数が設定されていません");
            }
            System.exit(1);
        }
        
        if (username == null || username.isEmpty()) {
            if (logger.isErrorEnabled()) {
                logger.error("GITHUB_USERNAME環境変数が設定されていません");
            }
            System.exit(1);
        }
        
        // GitHubContributionCheckerを初期化
        GitHubContributionChecker checker = new GitHubContributionChecker(githubToken);
        
        if (logger.isInfoEnabled()) {
            logger.info("GitHubユーザー '{}' の今日のContributionをチェックしています...", username);
        }
        
        // Contribution情報を取得（継続日数含む）
        GitHubContributionChecker.ContributionInfo info = checker.getContributionInfo(username);
        
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
    }
}
