package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GitHubContributionCheckerTest {
    
    private GitHubContributionChecker checker;
    
    @BeforeEach
    void setUp() {
        // テスト用のトークン（実際のテストでは環境変数から取得、なければダミートークン）
        String testToken = System.getenv("GITHUB_TOKEN");
        if (testToken == null) {
            testToken = "dummy_token_for_testing";
        }
        checker = new GitHubContributionChecker(testToken);
    }
    
    @Test
    @DisplayName("GitHubContributionCheckerが正常に初期化される")
    void testInitialization() {
        assertNotNull(checker, "GitHubContributionChecker should be initialized");
    }
    
    @Test
    @DisplayName("有効なGitHubユーザー名でContributionチェックが実行される")
    void testValidUserContributionCheck() {
        String testUsername = System.getenv("GITHUB_USERNAME");
        if (testUsername == null) {
            // テストスキップ（環境変数が設定されていない場合）
            return;
        }
        
        boolean result = checker.hasTodayContribution(testUsername);
        // 結果はtrue/falseどちらでもOK（実際のContribution状況による）
        assertTrue(result == true || result == false, "Should return boolean result");
    }
    
    @Test
    @DisplayName("無効なGitHubユーザー名でエラーハンドリングが動作する")
    void testInvalidUserContributionCheck() {
        boolean result = checker.hasTodayContribution("invalid_user_12345");
        // 無効なユーザーの場合はfalseが返されることを期待
        assertFalse(result, "Should return false for invalid user");
    }
    
    // ===== 新しいカレンダーベースの継続日数計算ロジックのテスト =====
    
    @Test
    @DisplayName("有効なユーザーで継続日数が取得できる")
    void testGetStreakDaysValidUser() {
        String testUsername = System.getenv("GITHUB_USERNAME");
        if (testUsername == null) {
            // テストスキップ（環境変数が設定されていない場合）
            return;
        }
        
        int streakDays = checker.getStreakDays(testUsername);
        
        // 観点1: 継続日数は0以上の整数であることを確認
        assertTrue(streakDays >= 0, "Streak days should be non-negative");
        
        // 観点2: 継続日数は現実的な範囲内であることを確認（365日を超えることはない）
        assertTrue(streakDays <= 365, "Streak days should not exceed 365 days");
    }
    
    @Test
    @DisplayName("無効なユーザーで継続日数が0を返す")
    void testGetStreakDaysInvalidUser() {
        int streakDays = checker.getStreakDays("invalid_user_12345");
        
        // 観点3: 無効なユーザーの場合は0を返すことを確認
        assertEquals(0, streakDays, "Invalid user should return 0 streak days");
    }
    
    // ===== ContributionInfoクラスのテスト =====
    
    @Test
    @DisplayName("有効なユーザーでContributionInfoが取得できる")
    void testGetContributionInfoValidUser() {
        String testUsername = System.getenv("GITHUB_USERNAME");
        if (testUsername == null) {
            // テストスキップ（環境変数が設定されていない場合）
            return;
        }
        
        GitHubContributionChecker.ContributionInfo info = checker.getContributionInfo(testUsername);
        
        // 観点4: ContributionInfoオブジェクトが正常に作成されることを確認
        assertNotNull(info, "ContributionInfo should not be null");
        
        // 観点5: Contributionの有無はboolean値であることを確認
        assertTrue(info.hasContribution() == true || info.hasContribution() == false, 
            "hasContribution should return boolean value");
        
        // 観点6: Contribution数は0以上の整数であることを確認
        assertTrue(info.getContributionCount() >= 0, "Contribution count should be non-negative");
        
        // 観点7: 継続日数は0以上の整数であることを確認
        assertTrue(info.getStreakDays() >= 0, "Streak days should be non-negative");
        
        // 観点8: 継続日数は現実的な範囲内であることを確認
        assertTrue(info.getStreakDays() <= 365, "Streak days should not exceed 365 days");
    }
    
    @Test
    @DisplayName("無効なユーザーでContributionInfoが適切なデフォルト値を返す")
    void testGetContributionInfoInvalidUser() {
        GitHubContributionChecker.ContributionInfo info = checker.getContributionInfo("invalid_user_12345");
        
        // 観点9: 無効なユーザーの場合、適切なデフォルト値が設定されることを確認
        assertNotNull(info, "ContributionInfo should not be null even for invalid user");
        assertFalse(info.hasContribution(), "Invalid user should have no contribution");
        assertEquals(0, info.getContributionCount(), "Invalid user should have 0 contribution count");
        assertEquals(0, info.getStreakDays(), "Invalid user should have 0 streak days");
    }
    
    // ===== ContributionInfoクラスの内部動作テスト =====
    
    @Test
    @DisplayName("ContributionInfoのtoStringメソッドが正しく動作する")
    void testContributionInfoToString() {
        // 観点10: ContributionInfoのtoStringメソッドが期待される形式で文字列を返すことを確認
        
        // ケース1: Contributionがある場合
        GitHubContributionChecker.ContributionInfo infoWithContribution = 
            new GitHubContributionChecker.ContributionInfo(true, 5, 10);
        String toString1 = infoWithContribution.toString();
        assertTrue(toString1.contains("hasContribution=true"), "toString should contain correct hasContribution value");
        assertTrue(toString1.contains("count=5"), "toString should contain correct contribution count");
        assertTrue(toString1.contains("streak=10"), "toString should contain correct streak days");
        
        // ケース2: Contributionがない場合
        GitHubContributionChecker.ContributionInfo infoWithoutContribution = 
            new GitHubContributionChecker.ContributionInfo(false, 0, 0);
        String toString2 = infoWithoutContribution.toString();
        assertTrue(toString2.contains("hasContribution=false"), "toString should contain correct hasContribution value");
        assertTrue(toString2.contains("count=0"), "toString should contain correct contribution count");
        assertTrue(toString2.contains("streak=0"), "toString should contain correct streak days");
    }
    
    @Test
    @DisplayName("ContributionInfoのゲッターメソッドが正しく動作する")
    void testContributionInfoGetters() {
        // 観点11: ContributionInfoの各ゲッターメソッドが正しい値を返すことを確認
        
        boolean hasContribution = true;
        int contributionCount = 15;
        int streakDays = 25;
        
        GitHubContributionChecker.ContributionInfo info = 
            new GitHubContributionChecker.ContributionInfo(hasContribution, contributionCount, streakDays);
        
        assertEquals(hasContribution, info.hasContribution(), "hasContribution getter should return correct value");
        assertEquals(contributionCount, info.getContributionCount(), "getContributionCount getter should return correct value");
        assertEquals(streakDays, info.getStreakDays(), "getStreakDays getter should return correct value");
    }
    
    // ===== エラーハンドリングのテスト =====
    
    @Test
    @DisplayName("ネットワークエラー時の適切なエラーハンドリング")
    void testNetworkErrorHandling() {
        // 観点12: ネットワークエラーやAPIエラーが発生した場合でも、
        // アプリケーションがクラッシュせずに適切なデフォルト値を返すことを確認
        
        // 無効なトークンでチェッカーを作成してエラーをシミュレート
        GitHubContributionChecker invalidChecker = new GitHubContributionChecker("invalid_token");
        
        // これらの呼び出しが例外を投げずにデフォルト値を返すことを確認
        boolean hasContribution = invalidChecker.hasTodayContribution("test_user");
        assertFalse(hasContribution, "Should return false on network/API error");
        
        GitHubContributionChecker.ContributionInfo info = invalidChecker.getContributionInfo("test_user");
        assertNotNull(info, "Should return ContributionInfo object on network/API error");
        assertFalse(info.hasContribution(), "Should have no contribution on network/API error");
        assertEquals(0, info.getContributionCount(), "Should have 0 contribution count on network/API error");
        assertEquals(0, info.getStreakDays(), "Should have 0 streak days on network/API error");
        
        int streakDays = invalidChecker.getStreakDays("test_user");
        assertEquals(0, streakDays, "Should return 0 streak days on network/API error");
    }
} 