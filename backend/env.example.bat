@echo off
REM 复制本文件为 env.bat，并按需填写你的密钥与参数

REM 必填：OpenAI 兼容服务的 API Key（勿提交仓库）
set "OPENAI_API_KEY=请在此处填入你的密钥"

REM 可选：OpenAI 兼容服务 Base URL（默认 https://api.aabao.vip/v1）
set "OPENAI_BASE_URL=https://api.aabao.vip/v1"

REM 可选：图片模型与尺寸（默认 dall-e-3 / 512x512）
set "OPENAI_IMAGE_MODEL=dall-e-3"
set "OPENAI_IMAGE_SIZE=512x512"

echo Local env loaded: OPENAI_BASE_URL=%OPENAI_BASE_URL%, MODEL=%OPENAI_IMAGE_MODEL%, SIZE=%OPENAI_IMAGE_SIZE%




