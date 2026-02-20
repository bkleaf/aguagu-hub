@echo off
setlocal enabledelayedexpansion

REM =============================================
REM aguagu-hub QNAP NAS 배포 스크립트
REM =============================================

REM --- 설정 (환경에 맞게 수정) ---
set IMAGE_NAME=aguagu-hub
set IMAGE_TAG=latest
set TAR_FILE=%IMAGE_NAME%.tar
set NAS_USER=admin
set NAS_HOST=NAS_IP_주소
set NAS_DEPLOY_DIR=/share/Container/aguagu-hub

REM --- 1단계: Docker 이미지 빌드 ---
echo [1/4] Docker 이미지 빌드 중...
docker build -t %IMAGE_NAME%:%IMAGE_TAG% .
if errorlevel 1 (
    echo [오류] Docker 이미지 빌드 실패
    exit /b 1
)
echo [완료] 이미지 빌드 성공

REM --- 2단계: 이미지를 tar 파일로 저장 ---
echo [2/4] 이미지를 %TAR_FILE%로 저장 중...
docker save -o %TAR_FILE% %IMAGE_NAME%:%IMAGE_TAG%
if errorlevel 1 (
    echo [오류] 이미지 저장 실패
    exit /b 1
)
echo [완료] 이미지 저장 성공

REM --- 3단계: NAS로 파일 전송 ---
echo [3/4] NAS로 파일 전송 중...
scp %TAR_FILE% %NAS_USER%@%NAS_HOST%:%NAS_DEPLOY_DIR%/
scp docker-compose.yml %NAS_USER%@%NAS_HOST%:%NAS_DEPLOY_DIR%/
if errorlevel 1 (
    echo [오류] 파일 전송 실패
    exit /b 1
)
echo [완료] 파일 전송 성공

REM --- 4단계: NAS에서 이미지 로드 및 컨테이너 실행 ---
echo [4/4] NAS에서 컨테이너 배포 중...
ssh %NAS_USER%@%NAS_HOST% "cd %NAS_DEPLOY_DIR% && docker load -i %TAR_FILE% && docker compose down && docker compose up -d && rm -f %TAR_FILE%"
if errorlevel 1 (
    echo [오류] NAS 배포 실패
    exit /b 1
)

REM --- 로컬 tar 파일 정리 ---
del /f %TAR_FILE%

echo.
echo =============================================
echo  배포 완료!
echo  NAS 주소: http://%NAS_HOST%:8080
echo  Swagger:  http://%NAS_HOST%:8080/swagger-ui/index.html
echo =============================================

endlocal
