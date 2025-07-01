package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

/**
 * AWS Systems Manager Parameter Storeから設定を読み込むクラス
 * 
 * <p>Lambda環境で安全にトークンやIDを管理するために使用します。</p>
 * 
 * <h3>設定パラメータ</h3>
 * <ul>
 *   <li><code>/github-grass-checker/github-token</code> - GitHub Personal Access Token</li>
 *   <li><code>/github-grass-checker/github-username</code> - GitHubユーザー名</li>
 *   <li><code>/github-grass-checker/line-channel-access-token</code> - LINEチャネルアクセストークン</li>
 *   <li><code>/github-grass-checker/line-user-id</code> - LINEユーザーID</li>
 * </ul>
 */
public class AwsConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(AwsConfigManager.class);
    
    private final SsmClient ssmClient;
    private final String parameterPrefix;
    
    public AwsConfigManager() {
        this.ssmClient = SsmClient.builder().build();
        this.parameterPrefix = "/github-grass-checker/";
    }
    
    public AwsConfigManager(SsmClient ssmClient, String parameterPrefix) {
        this.ssmClient = ssmClient;
        this.parameterPrefix = parameterPrefix;
    }
    
    /**
     * Parameter Storeからパラメータを取得
     * @param parameterName パラメータ名（プレフィックスなし）
     * @return パラメータ値、取得できない場合はnull
     */
    public String getParameter(String parameterName) {
        try {
            String fullParameterName = parameterPrefix + parameterName;
            GetParameterRequest request = GetParameterRequest.builder()
                .name(fullParameterName)
                .withDecryption(true) // SecureStringパラメータを復号化
                .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (ParameterNotFoundException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Parameter Storeにパラメータが見つかりません: {}", parameterName);
            }
            return null;
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Parameter Storeからのパラメータ取得に失敗: {}", parameterName, e);
            }
            return null;
        }
    }
    
    /**
     * GitHub Tokenを取得
     */
    public String getGitHubToken() {
        return getParameter("github-token");
    }
    
    /**
     * GitHubユーザー名を取得
     */
    public String getGitHubUsername() {
        return getParameter("github-username");
    }
    
    /**
     * LINEチャネルアクセストークンを取得
     */
    public String getLineChannelAccessToken() {
        return getParameter("line-channel-access-token");
    }
    
    /**
     * LINEユーザーIDを取得
     */
    public String getLineUserId() {
        return getParameter("line-user-id");
    }
    
    /**
     * すべての設定が利用可能かチェック
     */
    public boolean isConfigComplete() {
        String githubToken = getGitHubToken();
        String githubUsername = getGitHubUsername();
        
        if (githubToken == null || githubToken.isEmpty()) {
            logger.error("GitHub Tokenが設定されていません");
            return false;
        }
        
        if (githubUsername == null || githubUsername.isEmpty()) {
            logger.error("GitHub Usernameが設定されていません");
            return false;
        }
        
        return true;
    }
    
    /**
     * LINE通知設定が利用可能かチェック
     */
    public boolean isLineNotificationEnabled() {
        String channelToken = getLineChannelAccessToken();
        String userId = getLineUserId();
        
        return channelToken != null && !channelToken.isEmpty() && 
               userId != null && !userId.isEmpty();
    }
} 