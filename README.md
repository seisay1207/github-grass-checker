# GitHub Grass Checker

GitHub の草（Contribution）が生えていない場合に LINE に通知する Lambda を構築するプロジェクトです。

## 機能

- GitHub GraphQL API を使用して今日の Contribution をチェック
- Java 17 + Maven で実装
- ローカルでのテスト実行が可能

## セットアップ

### 1. 依存関係のインストール

```bash
mvn clean install
```

### 2. 環境変数の設定

GitHub Personal Access Token を取得し、環境変数に設定してください：

```bash
export GITHUB_TOKEN="your_github_personal_access_token"
export GITHUB_USERNAME="your_github_username"
```

### GitHub Personal Access Token の取得方法

1. GitHub にログイン
2. Settings > Developer settings > Personal access tokens > Tokens (classic)
3. "Generate new token"をクリック
4. 必要な権限を選択（最低限 `read:user` が必要）
5. Token を生成してコピー

## 使用方法

### ローカルでの実行

```bash
mvn exec:java -Dexec.mainClass="com.example.App"
```

または

```bash
mvn compile
java -cp target/classes com.example.App
```

### 実行例

```
[main] INFO com.example.App - GitHubユーザー 'your_username' の今日のContributionをチェックしています...
[main] INFO com.example.GitHubContributionChecker - 今日のContribution数: 3
[main] INFO com.example.App - ✅ 今日はContributionがあります！草が生えています。
```

## プロジェクト構造

```
src/main/java/com/example/
├── App.java                    # メインアプリケーション
└── GitHubContributionChecker.java  # GitHub GraphQL API呼び出しクラス
```

## 技術スタック

- Java 17
- Maven
- OkHttp (HTTP クライアント)
- Jackson (JSON 処理)
- SLF4J (ログ出力)

## 次のステップ

このコードを基に、AWS Lambda 関数としてデプロイし、LINE 通知機能を追加する予定です。
