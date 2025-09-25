#!/usr/bin/env python3
"""
修复protobuf生成的Java文件中的兼容性问题
"""

import re
import os
import sys

def fix_java_file(file_path):
    """修复单个Java文件"""
    print(f"修复文件: {file_path}")

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    # 1. 移除IntListAdapter相关代码
    # 找到并替换IntListAdapter.IntConverter的定义
    converter_pattern = r'private static final com\.google\.protobuf\.Internal\.IntListAdapter\.IntConverter<[^>]+>\s+\w+_converter_\s*=\s*new com\.google\.protobuf\.Internal\.IntListAdapter\.IntConverter<[^>]+>\(\)\s*\{[^}]+\};'
    content = re.sub(converter_pattern, '', content, flags=re.DOTALL)

    # 2. 替换getActionsList方法的实现
    getactions_pattern = r'public java\.util\.List<([^>]+)> getActionsList\(\) \{\s*return new com\.google\.protobuf\.Internal\.IntListAdapter<[^>]+>\([^)]+\);\s*\}'

    def replace_getactions(match):
        enum_type = match.group(1)
        return f'''public java.util.List<{enum_type}> getActionsList() {{
      java.util.List<{enum_type}> result = new java.util.ArrayList<{enum_type}>(actions_.size());
      for (int i = 0; i < actions_.size(); i++) {{
        int value = actions_.getInt(i);
        {enum_type} enumValue = {enum_type}.forNumber(value);
        result.add(enumValue == null ? {enum_type}.UNRECOGNIZED : enumValue);
      }}
      return result;
    }}'''

    content = re.sub(getactions_pattern, replace_getactions, content, flags=re.DOTALL)

    # 3. 如果内容有变化，写回文件
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✅ 修复完成")
        return True
    else:
        print(f"  ℹ️  无需修复")
        return False

def main():
    """主函数"""
    java_dir = "/Volumes/extStorage/workspace/android/did-wallet-sdk-android/protobuf/src/main/java"

    if not os.path.exists(java_dir):
        print(f"错误: 目录不存在 {java_dir}")
        sys.exit(1)

    fixed_count = 0

    # 遍历所有Java文件
    for root, dirs, files in os.walk(java_dir):
        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                if fix_java_file(file_path):
                    fixed_count += 1

    print(f"\n修复完成! 共修复了 {fixed_count} 个文件")

if __name__ == '__main__':
    main()