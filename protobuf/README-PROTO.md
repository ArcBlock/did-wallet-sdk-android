# Protobuf 文件更新指南

由于protobuf插件与Android库插件存在兼容性问题，protobuf模块现在使用手动生成的方式处理.proto文件。

**✨ 快速更新方法**：在项目根目录运行 `./update-proto.sh`

## 当你修改了.proto文件时

### 方案1：使用brew安装protoc（推荐）

1. **安装protoc编译器**：
   ```bash
   # macOS
   brew install protobuf

   # Ubuntu/Debian
   sudo apt-get install protobuf-compiler

   # CentOS/RHEL
   sudo yum install protobuf-compiler
   ```

2. **运行生成脚本**：
   ```bash
   # 在项目根目录
   ./generate-proto.sh
   ```

3. **或者手动生成**：
   ```bash
   cd protobuf

   # 为每个proto文件生成Java代码
   protoc --java_out=lite:src/main/java --proto_path=src/main/proto src/main/proto/*.proto
   ```

### 方案2：使用在线protoc编译器

1. 访问 https://protobuf-compiler.herokuapp.com/
2. 上传你的.proto文件
3. 选择"Java"作为输出语言，选择"lite"运行时
4. 下载生成的Java文件
5. 将文件放置在 `src/main/java` 对应的包目录中

### 方案3：使用Docker

```bash
# 使用Docker运行protoc
docker run --rm -v $(pwd):/workspace phovea/protoc:3.21.12 \
  --java_out=lite:/workspace/protobuf/src/main/java \
  --proto_path=/workspace/protobuf/src/main/proto \
  /workspace/protobuf/src/main/proto/*.proto
```

## 当前Proto文件位置

- **源文件**: `src/main/proto/`
- **生成的Java文件**: `src/main/java/`

## 生成规则

- 使用 `--java_out=lite` 生成轻量级的protobuf类
- 生成的类会自动放在对应的包结构中
- 确保生成的类文件被包含在git版本控制中

## 验证生成结果

生成完成后，运行以下命令验证：

```bash
# 构建protobuf模块
./gradlew :protobuf:assembleRelease

# 检查AAR文件是否包含新的类
unzip -l protobuf/build/outputs/aar/protobuf-release.aar
```

## 故障排除

### 如果protoc命令未找到

确保protoc已正确安装并在PATH中：
```bash
which protoc
protoc --version
```

### 如果生成的Java文件有编译错误

1. 检查proto文件语法是否正确
2. 确保导入的其他proto文件路径正确
3. 检查包名是否与目录结构匹配

### 如果AAR文件中缺少新类

1. 确保生成的Java文件在 `src/main/java` 中
2. 重新clean和build：
   ```bash
   ./gradlew :protobuf:clean :protobuf:assembleRelease
   ```

## 自动化建议

建议在CI/CD流程中添加检查，确保proto文件修改后对应的Java文件也被更新：

```bash
# 检查proto文件是否比Java文件新
find src/main/proto -name "*.proto" -newer src/main/java -exec echo "Proto文件需要重新生成" \; -quit
```