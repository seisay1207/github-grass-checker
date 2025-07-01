package com.example;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void testAppRuns() {
        // アプリケーションのmainが例外なく実行できることを確認
        try {
            App.main(new String[]{});
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false, "App.mainで例外: " + e.getMessage());
        }
    }
}
