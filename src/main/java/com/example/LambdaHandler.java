package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

/**
 * AWS Lambda用のハンドラークラス
 * 
 * <p>CloudWatch Eventsから定期実行される際のエントリーポイントです。</p>
 * 
 * <h3>設定</h3>
 * <p>AWS Systems Manager Parameter Storeに以下のパラメータを設定してください：</p>
 * <ul>
 *   <li><code>/github-grass-checker/github-token</code> - GitHub Personal Access Token（SecureString）</li>
 *   <li><code>/github-grass-checker/github-username</code> - GitHubユーザー名</li>
 *   <li><code>/github-grass-checker/line-channel-access-token</code> - LINEチャネルアクセストークン（SecureString）</li>
 *   <li><code>/github-grass-checker/line-user-id</code> - LINEユーザーID</li>
 * </ul>
 * 
 * <h3>IAM権限</h3>
 * <p>Lambda実行ロールに以下の権限が必要です：</p>
 * <ul>
 *   <li><code>ssm:GetParameter</code> - Parameter Storeからの読み取り</li>
 *   <li><code>kms:Decrypt</code> - SecureStringパラメータの復号化（KMS使用時）</li>
 * </ul>
 */
public class LambdaHandler implements RequestHandler<ScheduledEvent, String> {
    private static final Logger logger = LoggerFactory.getLogger(LambdaHandler.class);
    
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Lambda関数が開始されました。Event: {}", event.getDetail());
            }
            
            // メインアプリケーションを実行
            App.main(new String[0]);
            
            if (logger.isInfoEnabled()) {
                logger.info("Lambda関数が正常に完了しました");
            }
            
            return "SUCCESS";
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Lambda関数の実行中にエラーが発生しました", e);
            }
            throw new RuntimeException("Lambda関数の実行に失敗しました", e);
        }
    }
} 