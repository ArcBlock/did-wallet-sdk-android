#!/bin/bash

# 生成protobuf文件的脚本
set -e

echo "正在生成protobuf文件..."

# 进入protobuf目录
cd protobuf

# 设置路径
PROTO_SRC_DIR="src/main/proto"
OUTPUT_DIR="src/main/java"

# 检查protoc是否安装
if ! command -v protoc &> /dev/null; then
    echo "错误: protoc 未安装。请先安装Protocol Buffers编译器"
    echo "macOS: brew install protobuf"
    echo "Ubuntu: sudo apt-get install protobuf-compiler"
    exit 1
fi

# 确保输出目录存在
mkdir -p "$OUTPUT_DIR"

# 清理之前生成的文件（可选）
# find "$OUTPUT_DIR" -name "*.java" -type f | grep -E "(Proto|Grpc)" | xargs rm -f

# 生成Java文件
for proto_file in $(find "$PROTO_SRC_DIR" -name "*.proto"); do
    echo "处理: $proto_file"
    protoc --java_out=lite:"$OUTPUT_DIR" --proto_path="$PROTO_SRC_DIR" "$proto_file"
done

echo "protobuf文件生成完成！"
echo "生成的文件位于: $OUTPUT_DIR"

# 返回根目录
cd ..