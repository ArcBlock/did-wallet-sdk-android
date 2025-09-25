#!/bin/bash

# Proto文件更新脚本 - 简化版
# 使用Docker来避免本地安装protoc的复杂性

set -e

echo "🔄 更新protobuf文件..."

# 检查Docker是否可用
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装。请选择以下方案之一："
    echo ""
    echo "方案1: 安装Docker"
    echo "  macOS: brew install docker"
    echo "  或从 https://docker.com 下载Docker Desktop"
    echo ""
    echo "方案2: 安装protoc"
    echo "  macOS: brew install protobuf"
    echo "  Ubuntu: sudo apt-get install protobuf-compiler"
    echo ""
    echo "方案3: 手动操作"
    echo "  请参考 protobuf/README-PROTO.md"
    exit 1
fi

# 检查Docker是否运行
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker未运行。请启动Docker后重试。"
    exit 1
fi

echo "✅ 使用Docker生成protobuf文件..."

# 切换到项目根目录
cd "$(dirname "$0")"

# 使用Docker运行protoc
docker run --rm \
    -v "$(pwd)/protobuf/src/main/proto":/input \
    -v "$(pwd)/protobuf/src/main/java":/output \
    namely/protoc-all:1.51_1 \
    -f /input/*.proto \
    -l java \
    -o /output \
    --java_opt=lite

echo "✅ protobuf文件生成完成！"
echo ""
echo "📁 生成的文件位于: protobuf/src/main/java/"
echo ""
echo "🔨 现在可以运行构建："
echo "  ./gradlew :protobuf:assembleRelease"