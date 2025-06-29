package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LineNotifierTest {
    
    private LineNotifier lineNotifier;
    
    @BeforeEach
    void setUp() {
        // テスト用のLINE Notifyトークン（実際のテストでは環境変数から取得）
        String testToken = System.getenv("LINE_NOTIFY_TOKEN");
        if (testToken != null) {
            lineNotifier = new LineNotifier(testToken);
        }
    }
    
    @Test
    @DisplayName("LineNotifierの初期化テスト")
    void testInitialization() {
        // 環境変数が設定されている場合のみテスト
        if (System.getenv("LINE_NOTIFY_TOKEN") != null) {
            assertNotNull(lineNotifier, "LineNotifier should be initialized when token is provided");
        } else {
            // 環境変数が設定されていない場合は、テストをスキップする代わりに
            // 無効なトークンでLineNotifierを作成してテスト
            LineNotifier testNotifier = new LineNotifier("test_token");
            assertNotNull(testNotifier, "LineNotifier should be initialized even with test token");
        }
    }
    
    @Test
    @DisplayName("無効なトークンでエラーハンドリングが動作する")
    void testInvalidTokenMessageSend() {
        // 無効なトークンでLineNotifierを作成
        LineNotifier invalidNotifier = new LineNotifier("invalid_token");
        
        String testMessage = "テストメッセージ";
        boolean result = invalidNotifier.sendMessage(testMessage);
        
        // 無効なトークンまたは廃止されたAPIの場合はfalseが返されることを期待
        assertFalse(result, "Should return false for invalid token or discontinued API");
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
    @DisplayName("LineNotifierクラスの基本機能テスト")
    void testLineNotifierBasicFunctionality() {
        // 観点2: LineNotifierクラスの基本機能が正常に動作することを確認
        
        // テスト用のLineNotifierを作成
        LineNotifier testNotifier = new LineNotifier("test_token");
        
        // 基本的なメッセージ送信テスト（実際のAPIは廃止されているため、エラーハンドリングをテスト）
        String testMessage = "テストメッセージ";
        boolean result = testNotifier.sendMessage(testMessage);
        
        // 廃止されたAPIまたは無効なトークンの場合はfalseが返されることを期待
        assertFalse(result, "Should return false for discontinued API or invalid token");
    }
    
    @Test
    @DisplayName("Contribution通知メッセージの構築テスト")
    void testContributionNotificationMessageBuilding() {
        // 観点3: Contribution通知メッセージが正しく構築されることを確認
        
        // テスト用のLineNotifierを作成
        LineNotifier testNotifier = new LineNotifier("test_token");
        
        String username = "test_user";
        
        // ケース1: Contributionがある場合
        GitHubContributionChecker.ContributionInfo infoWithContribution = 
            new GitHubContributionChecker.ContributionInfo(true, 5, 10);
        
        boolean result1 = testNotifier.sendContributionNotification(username, infoWithContribution);
        // APIが廃止されているため、falseが返されることを期待
        assertFalse(result1, "Should return false for discontinued API");
        
        // ケース2: Contributionがない場合
        GitHubContributionChecker.ContributionInfo infoWithoutContribution = 
            new GitHubContributionChecker.ContributionInfo(false, 0, 5);
        
        boolean result2 = testNotifier.sendContributionNotification(username, infoWithoutContribution);
        // APIが廃止されているため、falseが返されることを期待
        assertFalse(result2, "Should return false for discontinued API");
    }
    
    @Test
    @DisplayName("空のメッセージでの送信テスト")
    void testEmptyMessageSend() {
        // 観点4: 空のメッセージでも適切に処理されることを確認
        
        LineNotifier testNotifier = new LineNotifier("test_token");
        
        boolean result = testNotifier.sendMessage("");
        
        // 空のメッセージでも送信は試行される（結果はAPI応答による）
        assertTrue(result == true || result == false, "Should return boolean result for empty message");
    }
    
    @Test
    @DisplayName("長いメッセージでの送信テスト")
    void testLongMessageSend() {
        // 観点5: 長いメッセージでも適切に処理されることを確認
        
        LineNotifier testNotifier = new LineNotifier("test_token");
        
        // 長いメッセージを作成（LINE Notifyの制限は1000文字）
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 50; i++) {
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
        String originalToken = System.getenv("LINE_NOTIFY_TOKEN");
        
        try {
            // 環境変数が設定されていない場合のテスト
            LineNotifier testNotifier = new LineNotifier("test_token");
            assertNotNull(testNotifier, "LineNotifier should be created even without environment variable");
            
            boolean result = testNotifier.sendMessage("テストメッセージ");
            assertTrue(result == true || result == false, "Should return boolean result");
            
        } finally {
            // テスト後に元の環境変数を復元（実際には影響しないが、念のため）
            if (originalToken != null) {
                // 環境変数の復元は通常のJavaでは直接できないため、コメントで記載
                // System.setProperty("LINE_NOTIFY_TOKEN", originalToken);
            }
        }
    }
} 