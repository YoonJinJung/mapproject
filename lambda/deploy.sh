#!/bin/bash
# ============================================================
# Space Threat Monitoring Dashboard - Lambda 배포 스크립트
# 실행 전 AWS CLI가 설치 및 구성되어 있어야 합니다.
# 사용법: chmod +x deploy.sh && ./deploy.sh
# ============================================================

set -e  # 오류 발생 시 즉시 종료

# ──────────────────────────────────────────────────────────
# [1] 설정값 (본인 환경에 맞게 수정)
# ──────────────────────────────────────────────────────────
FUNCTION_NAME="space-threat-bff"
REGION="ap-northeast-2"          # 서울 리전 (원하는 리전으로 변경 가능)
RUNTIME="nodejs18.x"
HANDLER="index.handler"
ZIP_FILE="lambda_function.zip"

# IAM Role ARN: AWS 콘솔에서 Lambda 기본 실행 역할 ARN을 복사하여 붙여넣기
# 예: arn:aws:iam::123456789012:role/service-role/AWSLambdaBasicExecutionRole
ROLE_ARN="arn:aws:iam::YOUR_ACCOUNT_ID:role/YOUR_LAMBDA_ROLE_NAME"

# ──────────────────────────────────────────────────────────
# [2] 배포 패키지 생성 (외부 패키지 없음 → index.js만 압축)
# ──────────────────────────────────────────────────────────
echo ">>> [1/3] Creating deployment package..."
cd "$(dirname "$0")"
zip -r "$ZIP_FILE" index.js
echo "    Done: $ZIP_FILE created."

# ──────────────────────────────────────────────────────────
# [3] Lambda 함수 생성 또는 코드 업데이트
# ──────────────────────────────────────────────────────────
echo ">>> [2/3] Deploying to AWS Lambda (region: $REGION)..."

FUNCTION_EXISTS=$(aws lambda get-function --function-name "$FUNCTION_NAME" --region "$REGION" 2>&1 || true)

if echo "$FUNCTION_EXISTS" | grep -q "ResourceNotFoundException"; then
  # 함수가 없으면 새로 생성
  echo "    Function not found. Creating new function..."
  aws lambda create-function \
    --function-name "$FUNCTION_NAME" \
    --runtime "$RUNTIME" \
    --handler "$HANDLER" \
    --role "$ROLE_ARN" \
    --zip-file "fileb://$ZIP_FILE" \
    --timeout 15 \
    --memory-size 128 \
    --region "$REGION"
else
  # 함수가 있으면 코드만 업데이트
  echo "    Function exists. Updating code..."
  aws lambda update-function-code \
    --function-name "$FUNCTION_NAME" \
    --zip-file "fileb://$ZIP_FILE" \
    --region "$REGION"
fi

# ──────────────────────────────────────────────────────────
# [4] 환경변수 설정 (NASA_API_KEY 주입)
# ──────────────────────────────────────────────────────────
echo ">>> [3/3] Setting environment variables..."
echo "    NOTE: Replace 'YOUR_NASA_API_KEY' with your actual key from https://api.nasa.gov"
aws lambda update-function-configuration \
  --function-name "$FUNCTION_NAME" \
  --environment "Variables={NASA_API_KEY=YOUR_NASA_API_KEY}" \
  --region "$REGION"

echo ""
echo "=== Deployment complete! ==="
echo "Function Name : $FUNCTION_NAME"
echo "Region        : $REGION"
echo ""
echo "Next step: Create an API Gateway (HTTP API) and connect it to this Lambda."
echo "Then set the API Gateway URL in your Android app's NetworkConfig.kt"

# 임시 zip 파일 정리
rm -f "$ZIP_FILE"
