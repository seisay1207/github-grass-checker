# テスト用 GitHub リポジトリ作成手順

## 1. 新しいリポジトリを作成

1. GitHub にログイン
2. 右上の「+」ボタン → 「New repository」
3. リポジトリ名: `test-contributions`
4. Description: `Test repository for GitHub Grass Checker`
5. Public を選択
6. 「Add a README file」にチェック
7. 「Create repository」をクリック

## 2. テスト用 Contribution を作成

```bash
# リポジトリをクローン
git clone https://github.com/YOUR_TEST_USERNAME/test-contributions.git
cd test-contributions

# テストファイルを作成
echo "# Test Contribution" > test-file.md
git add test-file.md
git commit -m "Add test contribution for GitHub Grass Checker"
git push origin main
```

## 3. Personal Access Token を作成

1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 「Generate new token」→「Generate new token (classic)」
3. Note: `GitHub Grass Checker Test`
4. Expiration: 30 days
5. Scopes: `read:user` のみチェック
6. 「Generate token」をクリック
7. **Token をコピーして保存**

## 4. テスト実行

```bash
# 環境変数を設定
export GITHUB_TOKEN="your_test_token_here"
export GITHUB_USERNAME="your_test_username"

# アプリケーションを実行
mvn exec:java -Dexec.mainClass="com.example.App"
```

## 5. 期待される結果

- Contribution がある場合: ✅ 今日は Contribution があります！
- Contribution がない場合: ❌ 今日は Contribution がありません。
