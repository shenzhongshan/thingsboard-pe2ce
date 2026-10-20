#!/usr/bin/env python3
"""
校验 docs/ 下中英两版文档的结构对齐，防止翻译版静默过期。

本项目约定中文版为权威版（见各中文文件开头的声明），英文版是对照上游 CE 的参照。
两份并存就一定会漂移，而过期的文档比没有文档更危险——本仓库的
20-dao/sql-schema.md 就曾把 basic/lts 描述成两种升级类型，误导了很久。

这个脚本不理解内容，只比结构。结构能对上，不保证译文准确；结构对不上，一定是
有一边改了另一边没跟。配合 upgrade_doc_conformance.py（那个校验具体事实断言）。

用法:
  python docs/tools/doc_translation_parity.py [docs 目录]

退出码 0 = 对齐，1 = 有缺口。
"""
import io
import os
import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DOCS = os.path.dirname(HERE)

ZH_SUFFIX = '.zh-CN.md'
# 中文版必须带的权威性声明关键词，缺了说明是从英文直接复制改名的
AUTHORITY_MARKER = '权威'

H2_RE = re.compile(r'^##\s+(.*)$', re.M)
H3_RE = re.compile(r'^###\s+', re.M)
NUMBERED_H2_RE = re.compile(r'^##\s+(\d+)\.', re.M)
TABLE_SEP_RE = re.compile(r'^\|[\s:|-]+\|\s*$', re.M)
FENCE_RE = re.compile(r'^```', re.M)
LINK_RE = re.compile(r'\]\((?!https?:|mailto:)([^)#]+)')


def read(path):
    return io.open(path, encoding='utf-8', errors='replace').read()


def english_docs(docs_dir):
    out = []
    for root, _, files in os.walk(docs_dir):
        if os.path.basename(root) == 'tools' and root != docs_dir:
            pass  # tools/README.md 也要译，不跳过
        for fn in sorted(files):
            if fn.endswith('.md') and not fn.endswith(ZH_SUFFIX):
                out.append(os.path.join(root, fn))
    return sorted(out)


def zh_path(en_path):
    return en_path[:-len('.md')] + ZH_SUFFIX


def structure(text):
    return {
        'h2': len(H2_RE.findall(text)),
        'h3': len(H3_RE.findall(text)),
        'numbered': NUMBERED_H2_RE.findall(text),
        'tables': len(TABLE_SEP_RE.findall(text)),
        'fences': len(FENCE_RE.findall(text)),
    }


def main(docs_dir):
    docs_dir = os.path.abspath(docs_dir)
    if not os.path.isdir(docs_dir):
        print('FAIL  目录不存在: {}'.format(docs_dir))
        return 1

    en_files = english_docs(docs_dir)
    missing, mismatched, link_broken, no_marker = [], [], [], []
    checked = 0

    for en in en_files:
        rel = os.path.relpath(en, docs_dir)
        zh = zh_path(en)
        if not os.path.exists(zh):
            missing.append(rel)
            continue
        checked += 1

        en_s = structure(read(en))
        zh_text = read(zh)
        zh_s = structure(zh_text)

        diffs = []
        for key, label in (('h2', '二级标题数'), ('h3', '三级标题数'),
                           ('tables', '表格数'), ('fences', '代码块围栏数')):
            if en_s[key] != zh_s[key]:
                diffs.append('{}: 英文 {} / 中文 {}'.format(label, en_s[key], zh_s[key]))
        if en_s['numbered'] != zh_s['numbered']:
            diffs.append('编号节序列: 英文 {} / 中文 {}'.format(en_s['numbered'], zh_s['numbered']))
        if diffs:
            mismatched.append((rel, diffs))

        # 中文版里的相对链接必须都能解析；链到 .md 的建议链到中文版，但不强制
        for target in LINK_RE.findall(zh_text):
            resolved = os.path.normpath(os.path.join(os.path.dirname(zh), target))
            if not os.path.exists(resolved):
                link_broken.append((rel, target))

        if AUTHORITY_MARKER not in zh_text[:1200]:
            no_marker.append(rel)

    print('英文文档 {} 篇，已有中文版 {} 篇'.format(len(en_files), checked))
    print('')

    if missing:
        print('缺中文版 ({} 篇):'.format(len(missing)))
        for rel in missing:
            print('    ' + rel)
        print('')
    if mismatched:
        print('结构不一致 ({} 篇):'.format(len(mismatched)))
        for rel, diffs in mismatched:
            print('    ' + rel)
            for d in diffs:
                print('        ' + d)
        print('')
    if link_broken:
        print('中文版失效链接 ({} 处):'.format(len(link_broken)))
        for rel, target in link_broken:
            print('    {} -> {}'.format(rel, target))
        print('')
    if no_marker:
        print('中文版开头缺少权威性声明 ({} 篇):'.format(len(no_marker)))
        for rel in no_marker:
            print('    ' + rel)
        print('')

    total = len(missing) + len(mismatched) + len(link_broken) + len(no_marker)
    if total:
        print('FAIL  {} 项待处理'.format(total))
        return 1
    print('OK    {} 篇全部对齐'.format(checked))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else DEFAULT_DOCS))
