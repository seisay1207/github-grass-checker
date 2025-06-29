#!/bin/bash

# テスト用GitHubアカウント設定
echo "=== GitHub Grass Checker テスト設定 ==="
echo ""

# テスト用アカウント情報の入力
read -p "テスト用GitHubユーザー名を入力してください: " TEST_GITHUB_USERNAME
read -p "テスト用GitHub Personal Access Tokenを入力してください: " TEST_GITHUB_TOKEN

# 環境変数を設定
export GITHUB_USERNAME="$TEST_GITHUB_USERNAME"
export GITHUB_TOKEN="$TEST_GITHUB_TOKEN"

echo ""
echo "設定完了！"
echo "GitHubユーザー名: $GITHUB_USERNAME"
echo ""

# 設定の確認
echo "設定をテストしますか？ (y/n)"
read -p "選択してください: " CONFIRM

if [ "$CONFIRM" = "y" ] || [ "$CONFIRM" = "Y" ]; then
    echo ""
    echo "アプリケーションを実行中..."
    mvn exec:java -Dexec.mainClass="com.example.App"
else
    echo ""
    echo "設定が完了しました。"
    echo "後で以下のコマンドでテストできます："
    echo "mvn exec:java -Dexec.mainClass=\"com.example.App\""
fi 