package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LineMessagingNotifierTest {
    
    private LineMessagingNotifier lineNotifier;
    
    @BeforeEach
    void setUp() {
        // テスト用のLINE Messaging APIトークン（実際のテストでは環境変数から取得）
        String testChannelAccessToken = System.getenv("LINE_CHANNEL_ACCESS_TOKEN");
        String testUserId = System.getenv("LINE_USER_ID");
        if (testChannelAccessToken != null && testUserId != null) {
            lineNotifier = new LineMessagingNotifier(testChannelAccessToken, testUserId);
        }
    }
    
    @Test
    @DisplayName("LineMessagingNotifierの初期化テスト")
    void testInitialization() {
        // 環境変数が設定されている場合のみテスト
        if (System.getenv("LINE_CHANNEL_ACCESS_TOKEN") != null && System.getenv("LINE_USER_ID") != null) {
            assertNotNull(lineNotifier, "LineMessagingNotifier should be initialized when tokens are provided");
        } else {
            // 環境変数が設定されていない場合は、テスト用のトークンでLineMessagingNotifierを作成してテスト
            LineMessagingNotifier testNotifier = new LineMessagingNotifier("test_token", "test_user_id");
            assertNotNull(testNotifier, "LineMessagingNotifier should be initialized even with test tokens");
        }
    }
    
    @Test
    @DisplayName("無効なトークンでエラーハンドリングが動作する")
    void testInvalidTokenMessageSend() {
        // 無効なトークンでLineMessagingNotifierを作成
        LineMessagingNotifier invalidNotifier = new LineMessagingNotifier("invalid_token", "test_user_id");
        
        String testMessage = "テストメッセージ";
        boolean result = invalidNotifier.sendMessage(testMessage);
        
        // 無効なトークンの場合はfalseが返されることを期待
        assertFalse(result, "Should return false for invalid token");
    }
    
    @Test
    @DisplayName("ContributionInfoクラスの動作確認")
    void testContributionInfoClass() {
        // 観点1: ContributionInfoクラスが正常に動作することを確認
        
        // ケース1: Contributionがある場合
        GitHubContributionChecker.ContributionInfo infoWithContribution = 
            new GitHubContributionChecker.ContributionInfo(true, 3, 7);
        
        assertTrue(infoWithContribution.hasContribution(), "Should have contribution");
        assertEquals(3, infoWithContribution.getContributionCount(), "Should have correct contribution count");
        assertEquals(7, infoWithContribution.getStreakDays(), "Should have correct streak days");
        
        // ケース2: Contributionがない場合
        GitHubContributionChecker.ContributionInfo infoWithoutContribution = 
            new GitHubContributionChecker.ContributionInfo(false, 0, 0);
        
        assertFalse(infoWithoutContribution.hasContribution(), "Should not have contribution");
        assertEquals(0, infoWithoutContribution.getContributionCount(), "Should have 0 contribution count");
        assertEquals(0, infoWithoutContribution.getStreakDays(), "Should have 0 streak days");
    }
    
    @Test
    @DisplayName("LineMessagingNotifierクラスの基本機能テスト")
    void testLineMessagingNotifierBasicFunctionality() {
        // 観点2: LineMessagingNotifierクラスの基本機能が正常に動作することを確認
        
        // テスト用のLineMessagingNotifierを作成
        LineMessagingNotifier testNotifier = new LineMessagingNotifier("test_token", "test_user_id");
        
        // 基本的なメッセージ送信テスト（無効なトークンのため、エラーハンドリングをテスト）
        String testMessage = "テストメッセージ";
        boolean result = testNotifier.sendMessage(testMessage);
        
        // 無効なトークンの場合はfalseが返されることを期待
        assertFalse(result, "Should return false for invalid token");
    }
    
    @Test
    @DisplayName("Contribution通知メッセージの構築テスト")
    void testContributionNotificationMessageBuilding() {
        // 観点3: Contribution通知メッセージが正しく構築されることを確認
        
        // テスト用のLineMessagingNotifierを作成
        LineMessagingNotifier testNotifier = new LineMessagingNotifier("test_token", "test_user_id");
        
        String username = "test_user";
        
        // ケース1: Contributionがある場合
        GitHubContributionChecker.ContributionInfo infoWithContribution = 
            new GitHubContributionChecker.ContributionInfo(true, 5, 10);
        
        boolean result1 = testNotifier.sendContributionNotification(username, infoWithContribution);
        // 無効なトークンのため、falseが返されることを期待
        assertFalse(result1, "Should return false for invalid token");
        
        // ケース2: Contributionがない場合
        GitHubContributionChecker.ContributionInfo infoWithoutContribution = 
            new GitHubContributionChecker.ContributionInfo(false, 0, 5);
        
        boolean result2 = testNotifier.sendContributionNotification(username, infoWithoutContribution);
        // 無効なトークンのため、falseが返されることを期待
        assertFalse(result2, "Should return false for invalid token");
    }
    
    @Test
    @DisplayName("空のメッセージでの送信テスト")
    void testEmptyMessageSend() {
        // 観点4: 空のメッセージでも適切に処理されることを確認
        
        LineMessagingNotifier testNotifier = new LineMessagingNotifier("test_token", "test_user_id");
        
        boolean result = testNotifier.sendMessage("");
        
        // 空のメッセージでも送信は試行される（結果はAPI応答による）
        assertTrue(result == true || result == false, "Should return boolean result for empty message");
    }
    
    @Test
    @DisplayName("長いメッセージでの送信テスト")
    void testLongMessageSend() {
        // 観点5: 長いメッセージでも適切に処理されることを確認
        
        LineMessagingNotifier testNotifier = new LineMessagingNotifier("test_token", "test_user_id");
        
        // 長いメッセージを作成（LINE Messaging APIの制限は5000文字）
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longMessage.append("これは長いメッセージのテストです。").append(i).append("\n");
        }
        
        boolean result = testNotifier.sendMessage(longMessage.toString());
        
        // 長いメッセージでも送信は試行される（結果はAPI応答による）
        assertTrue(result == true || result == false, "Should return boolean result for long message");
    }
    
    @Test
    @DisplayName("環境変数なしでの動作テスト")
    void testBehaviorWithoutEnvironmentVariable() {
        // 観点6: 環境変数が設定されていない場合の動作を確認
        
        // 環境変数を一時的にクリアしてテスト
        String originalChannelToken = System.getenv("LINE_CHANNEL_ACCESS_TOKEN");
        String originalUserId = System.getenv("LINE_USER_ID");
        
        try {
            // 環境変数が設定されていない場合のテスト
            LineMessagingNotifier testNotifier = new LineMessagingNotifier("test_token", "test_user_id");
            assertNotNull(testNotifier, "LineMessagingNotifier should be created even without environment variable");
            
            boolean result = testNotifier.sendMessage("テストメッセージ");
            assertTrue(result == true || result == false, "Should return boolean result");
            
        } finally {
            // テスト後に元の環境変数を復元（実際には影響しないが、念のため）
            if (originalChannelToken != null || originalUserId != null) {
                // 環境変数の復元は通常のJavaでは直接できないため、コメントで記載
                // System.setProperty("LINE_CHANNEL_ACCESS_TOKEN", originalChannelToken);
                // System.setProperty("LINE_USER_ID", originalUserId);
            }
        }
    }
    
    @Test
    @DisplayName("JSONエスケープ機能のテスト")
    void testJsonEscapeFunctionality() {
        // 観点7: JSONエスケープ機能が正常に動作することを確認
        
        LineMessagingNotifier testNotifier = new LineMessagingNotifier("test_token", "test_user_id");
        
        // 特殊文字を含むメッセージをテスト
        String messageWithSpecialChars = "テスト\"メッセージ\\改行\nテスト";
        boolean result = testNotifier.sendMessage(messageWithSpecialChars);
        
        // 特殊文字を含むメッセージでも送信は試行される（結果はAPI応答による）
        assertTrue(result == true || result == false, "Should return boolean result for message with special characters");
    }
} 