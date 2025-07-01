# GitHub Grass Checker

GitHub の草（Contribution）が生えていない場合に LINE に通知する Lambda を構築するプロジェクトです。

## 機能

- GitHub GraphQL API を使用して今日の Contribution をチェック
- 継続日数（ストリーク）の計算
- Contribution がない場合の LINE 通知機能（LINE Messaging API）
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

LINE 通知機能を使用する場合は、LINE Messaging API の設定が必要です：

```bash
export LINE_CHANNEL_ACCESS_TOKEN="your_line_channel_access_token"
export LINE_USER_ID="your_line_user_id"
```

### GitHub Personal Access Token の取得方法

1. GitHub にログイン
2. Settings > Developer settings > Personal access tokens > Tokens (classic)
3. "Generate new token"をクリック
4. 必要な権限を選択（最低限 `read:user` が必要）
5. Token を生成してコピー

### LINE Messaging API の設定方法

1. [LINE Developers](https://developers.line.biz/) にアクセス
2. LINE 公式アカウントを開設
3. Messaging API チャネルを作成
4. チャネルアクセストークンを取得
5. ユーザー ID を取得（通知を受け取る人の LINE ユーザー ID）

#### ユーザー ID の取得方法

- LINE 公式アカウントを友だち追加
- メッセージを送信
- Webhook でユーザー ID を取得するか、LINE Developers コンソールで確認

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
[main] INFO com.example.LineMessagingNotifier - LINE Messaging APIで通知を送信しました: ❌ GitHub Contribution チェック結果...
[main] INFO com.example.App - LINE通知の送信が完了しました
```

## 動作仕様

- **Contribution がある場合**: ログ出力のみ（LINE 通知は送信されません）
- **Contribution がない場合**: ログ出力 + LINE 通知（LINE_CHANNEL_ACCESS_TOKEN と LINE_USER_ID が設定されている場合のみ）
- **LINE 通知なし**: LINE_CHANNEL_ACCESS_TOKEN または LINE_USER_ID が設定されていない場合は、ログ出力のみ

## プロジェクト構造

```
src/main/java/com/example/
├── App.java                    # メインアプリケーション
├── GitHubContributionChecker.java  # GitHub GraphQL API呼び出しクラス
└── LineMessagingNotifier.java  # LINE Messaging API通知機能クラス
```

## 技術スタック

- Java 17
- Maven
- OkHttp (HTTP クライアント)
- Jackson (JSON 処理)
- SLF4J (ログ出力)
- LINE Messaging API

## テスト

### 全テストの実行

```bash
mvn test
```

### 特定のテストクラスの実行

```bash
mvn test -Dtest=GitHubContributionCheckerTest
mvn test -Dtest=LineMessagingNotifierTest
```

## 次のステップ

このコードを基に、AWS Lambda 関数としてデプロイし、CloudWatch Events での定期実行を設定する予定です。

## AWS Lambda デプロイ

### 1. JAR ファイルの作成

```bash
mvn clean package
```

### 2. AWS Systems Manager Parameter Store の設定

以下のパラメータを AWS Systems Manager Parameter Store に設定してください：

#### 必須パラメータ

- **パラメータ名**: `/github-grass-checker/github-token`

  - **タイプ**: SecureString
  - **値**: あなたの GitHub Personal Access Token

- **パラメータ名**: `/github-grass-checker/github-username`
  - **タイプ**: String
  - **値**: あなたの GitHub ユーザー名

#### オプションパラメータ

- **パラメータ名**: `/github-grass-checker/line-channel-access-token`

  - **タイプ**: SecureString
  - **値**: あなたの LINE チャネルアクセストークン

- **パラメータ名**: `/github-grass-checker/line-user-id`
  - **タイプ**: String
  - **値**: あなたの LINE ユーザー ID

### 3. Lambda 関数の作成

1. AWS Lambda コンソールで新しい関数を作成
2. **関数名**: `github-grass-checker`
3. **ランタイム**: Java 17
4. **ハンドラー**: `com.example.LambdaHandler::handleRequest`

### 4. IAM 権限の設定

Lambda 実行ロールに以下の権限を追加：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["ssm:GetParameter"],
      "Resource": "arn:aws:ssm:*:*:parameter/github-grass-checker/*"
    },
    {
      "Effect": "Allow",
      "Action": ["kms:Decrypt"],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "ssm.*.amazonaws.com"
        }
      }
    }
  ]
}
```

### 5. CloudWatch Events の設定

毎日特定の時間に実行するための CloudWatch Events ルールを作成：

```json
{
  "schedule": "cron(0 20 * * ? *)" // 毎日20:00（UTC）に実行
}
```

### 6. デプロイ

作成した JAR ファイルを Lambda 関数にアップロードしてデプロイ完了です。
