#!/bin/bash

# GitHub草チェッカー AWS Lambda デプロイスクリプト

set -e

# 設定
FUNCTION_NAME="github-grass-checker"
REGION="ap-northeast-1"
RUNTIME="java17"
HANDLER="com.example.LambdaHandler::handleRequest"
TIMEOUT=30
MEMORY_SIZE=512

echo "🚀 GitHub草チェッカーをAWS Lambdaにデプロイします..."

# 1. JARファイルの作成
echo "📦 JARファイルを作成中..."
mvn clean package

# 2. JARファイルの確認
JAR_FILE="target/github-grass-checker-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JARファイルが見つかりません: $JAR_FILE"
    exit 1
fi

echo "✅ JARファイルが作成されました: $JAR_FILE"

# 3. AWS CLIの確認
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLIがインストールされていません"
    echo "https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html からインストールしてください"
    exit 1
fi

# 4. AWS認証情報の確認
AWS_PROFILE=${AWS_PROFILE:-"github-grass-checker-dev"}

if ! aws sts get-caller-identity --profile "$AWS_PROFILE" &> /dev/null; then
    echo "❌ AWS認証情報が設定されていません"
    echo "aws configure --profile github-grass-checker-dev を実行して認証情報を設定してください"
    exit 1
fi

echo "✅ AWS認証情報が確認されました (プロファイル: $AWS_PROFILE)"

# 5. Lambda関数の存在確認
if aws lambda get-function --function-name "$FUNCTION_NAME" --region "$REGION" --profile "$AWS_PROFILE" &> /dev/null; then
    echo "📝 既存のLambda関数を更新します..."
    aws lambda update-function-code \
        --function-name "$FUNCTION_NAME" \
        --zip-file "fileb://$JAR_FILE" \
        --region "$REGION" \
        --profile "$AWS_PROFILE"
    
    aws lambda update-function-configuration \
        --function-name "$FUNCTION_NAME" \
        --timeout "$TIMEOUT" \
        --memory-size "$MEMORY_SIZE" \
        --region "$REGION" \
        --profile "$AWS_PROFILE"
    
    echo "✅ Lambda関数が更新されました"
else
    echo "🆕 新しいLambda関数を作成します..."
    
    # IAMロールの作成（既存の場合はスキップ）
    ROLE_NAME="github-grass-checker-role"
    if ! aws iam get-role --role-name "$ROLE_NAME" --profile "$AWS_PROFILE" &> /dev/null; then
        echo "🔐 IAMロールを作成中..."
        
        # 信頼ポリシーの作成
        cat > trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
        
        # ロールの作成
        aws iam create-role \
            --role-name "$ROLE_NAME" \
            --assume-role-policy-document file://trust-policy.json \
            --profile "$AWS_PROFILE"
        
        # 基本実行ポリシーのアタッチ
        aws iam attach-role-policy \
            --role-name "$ROLE_NAME" \
            --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole \
            --profile "$AWS_PROFILE"
        
        # Systems Manager Parameter Storeアクセスポリシーのアタッチ
        aws iam attach-role-policy \
            --role-name "$ROLE_NAME" \
            --policy-arn arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess \
            --profile "$AWS_PROFILE"
        
        # ロールが使用可能になるまで待機
        echo "⏳ IAMロールが使用可能になるまで待機中..."
        aws iam wait role-exists --role-name "$ROLE_NAME" --profile "$AWS_PROFILE"
        sleep 10
        
        rm trust-policy.json
    fi
    
    # ロールARNの取得
    ROLE_ARN=$(aws iam get-role --role-name "$ROLE_NAME" --query 'Role.Arn' --output text --profile "$AWS_PROFILE")
    
    # Lambda関数の作成
    aws lambda create-function \
        --function-name "$FUNCTION_NAME" \
        --runtime "$RUNTIME" \
        --role "$ROLE_ARN" \
        --handler "$HANDLER" \
        --zip-file "fileb://$JAR_FILE" \
        --timeout "$TIMEOUT" \
        --memory-size "$MEMORY_SIZE" \
        --region "$REGION" \
        --profile "$AWS_PROFILE"
    
    echo "✅ Lambda関数が作成されました"
fi

# 6. 環境変数の設定
echo "🔧 環境変数を設定中..."
aws lambda update-function-configuration \
    --function-name "$FUNCTION_NAME" \
    --environment "Variables={ENVIRONMENT=production}" \
    --region "$REGION" \
    --profile "$AWS_PROFILE"

echo "✅ 環境変数が設定されました"

# 7. 関数の詳細表示
echo ""
echo "📋 デプロイ完了！"
echo "関数名: $FUNCTION_NAME"
echo "リージョン: $REGION"
echo "ランタイム: $RUNTIME"
echo "ハンドラー: $HANDLER"
echo "タイムアウト: ${TIMEOUT}秒"
echo "メモリ: ${MEMORY_SIZE}MB"

echo ""
echo "🔗 次のステップ:"
echo "1. AWS Systems Manager Parameter Storeに設定値を保存"
echo "2. CloudWatch Eventsで定期実行を設定"
echo "3. テスト実行で動作確認"

echo ""
echo "📝 設定例:"
echo "aws ssm put-parameter --name '/github-grass-checker/github-token' --value 'your-github-token' --type SecureString --profile $AWS_PROFILE"
echo "aws ssm put-parameter --name '/github-grass-checker/line-channel-token' --value 'your-line-token' --type SecureString --profile $AWS_PROFILE"
echo "aws ssm put-parameter --name '/github-grass-checker/line-user-id' --value 'your-line-user-id' --type SecureString --profile $AWS_PROFILE"
echo "aws ssm put-parameter --name '/github-grass-checker/github-username' --value 'your-github-username' --type String --profile $AWS_PROFILE" 