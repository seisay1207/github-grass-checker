# GitHub Grass Checker

GitHub の草（Contribution）が生えていない場合に LINE に通知する Lambda を構築するプロジェクトです。

## 機能

- GitHub GraphQL API を使用して今日の Contribution をチェック
- 継続日数（ストリーク）の計算
- Contribution がない場合の LINE 通知機能
- Java 17 + Maven で実装
- ローカルでのテスト実行が可能

## セットアップ

### 1. 依存関係のインストール

```bash
mvn clean install
```

### 2. 環境変数の設定

#### 必須の環境変数

GitHub Personal Access Token を取得し、環境変数に設定してください：

```bash
export GITHUB_TOKEN="your_github_personal_access_token"
export GITHUB_USERNAME="your_github_username"
```

#### オプションの環境変数

LINE 通知機能を使用する場合は、LINE Notify API トークンを設定してください：

```bash
export LINE_NOTIFY_TOKEN="your_line_notify_token"
```

### GitHub Personal Access Token の取得方法

1. GitHub にログイン
2. Settings > Developer settings > Personal access tokens > Tokens (classic)
3. "Generate new token"をクリック
4. 必要な権限を選択（最低限 `read:user` が必要）
5. Token を生成してコピー

### LINE Notify API トークンの取得方法

1. [LINE Notify](https://notify-bot.line.me/) にアクセス
2. LINE アカウントでログイン
3. 「マイページ」→「トークンを発行する」
4. トークン名を入力して発行
5. 発行されたトークンをコピー

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

#### Contribution がある場合

```
[main] INFO com.example.App - GitHubユーザー 'your_username' の今日のContributionをチェックしています...
[main] INFO com.example.GitHubContributionChecker - 今日のContribution数: 3 (実際のアクティビティ: true)
[main] INFO com.example.App - ✅ 今日はContributionがあります！草が生えています。
[main] INFO com.example.App - 📊 今日のContribution数: 3件
[main] INFO com.example.App - 🔥 継続日数: 5日
```

#### Contribution がない場合（LINE 通知あり）

```
[main] INFO com.example.App - GitHubユーザー 'your_username' の今日のContributionをチェックしています...
[main] INFO com.example.GitHubContributionChecker - 今日のContribution数: 0 (実際のアクティビティ: false)
[main] WARN com.example.App - ❌ 今日はContributionがありません。草が生えていません。
[main] WARN com.example.App - 💔 継続記録が途切れます。現在の継続日数: 5日
[main] INFO com.example.App - LINE通知を送信しています...
[main] INFO com.example.LineNotifier - LINE通知を送信しました: ❌ GitHub Contribution チェック結果...
[main] INFO com.example.App - LINE通知の送信が完了しました
```

## 動作仕様

- **Contribution がある場合**: ログ出力のみ（LINE 通知は送信されません）
- **Contribution がない場合**: ログ出力 + LINE 通知（LINE_NOTIFY_TOKEN が設定されている場合のみ）
- **LINE 通知なし**: LINE_NOTIFY_TOKEN が設定されていない場合は、ログ出力のみ

## プロジェクト構造

```
src/main/java/com/example/
├── App.java                    # メインアプリケーション
├── GitHubContributionChecker.java  # GitHub GraphQL API呼び出しクラス
└── LineNotifier.java           # LINE通知機能クラス
```

## 技術スタック

- Java 17
- Maven
- OkHttp (HTTP クライアント)
- Jackson (JSON 処理)
- SLF4J (ログ出力)
- LINE Notify API

## テスト

### 全テストの実行

```bash
mvn test
```

### 特定のテストクラスの実行

```bash
mvn test -Dtest=GitHubContributionCheckerTest
mvn test -Dtest=LineNotifierTest
```

## 次のステップ

このコードを基に、AWS Lambda 関数としてデプロイし、CloudWatch Events での定期実行を設定する予定です。
