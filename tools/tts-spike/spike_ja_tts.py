#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""S0/S3 spike —— 验证「piper-plus 内置日语 TTS」这条路线能不能用。

背景（为什么不是 sherpa-onnx + Kokoro）
--------------------------------------------------
v2 规划原本选 sherpa-onnx + Kokoro-82M INT8，核实后有两个硬伤：
  1. sherpa-onnx 官方**没有**日语 TTS 模型，日语在 issue #3028 里被列为"缺失语言"
     （官方只有日语 ASR 模型）。
  2. sherpa-onnx 的 Kokoro 多语言模型里，非中文 CJK 一律走 espeak-ng；
     espeak-ng 没有日语形态素词典，汉字会被念成"字符描述"（subwave 的修复 commit 记录了这个坑）。
本脚本用来验证替代路线：piper-plus（OpenJTalk G2P + VITS，MIT）。

这个脚本做三件事
--------------------------------------------------
1. G2P 正确性：打印每句的音素序列，直接看汉字有没有被正确读出来
   （若被念成字符描述，音素串会异常长/异常）
2. 音质样本：把 wav 输出到你指定的目录，自己听
3. 性能：模型加载时间 + 逐句 RTF（推理耗时 / 音频时长）

用法
--------------------------------------------------
  python spike_ja_tts.py \
      --model "D:\\piper-plus-models\\tsukuyomi-chan-6lang-fp16.onnx" \
      --config "D:\\piper-plus-models\\config.json" \
      --db "D:\\Kanji-Dojo\\SQL翻译中\\kanji-dojo-data-base-v15-汉化中.sql" \
      --out "%USERPROFILE%\\Desktop\\tts-spike" \
      --limit 20

不传 --db 时用内置的默认句子集（离线可跑）。
"""

import argparse
import json
import os
import re
import sqlite3
import sys
import time
import wave

CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")

# 内置兜底句子集：覆盖 汉字假名混在 / 纯假名 / 数字 / 对话 / 汉字密集 五类
DEFAULT_SENTENCES = [
    "数学は私には難しい",
    "これはとてもおもしろいですね",
    "私は一日に100ユーロ稼ぎます",
    "「実を言うと、俺、高所恐怖症なんだ」「臆病者っ！」",
    "私の息子はいつもバスに乗ると酔う",
    "経済成長率の低下が深刻化している",
]


def load_sentences(db_path, limit):
    """优先从项目词典库里取真实例句（带中文译文，方便对照）。"""
    if not db_path or not os.path.exists(db_path):
        print("[i] 未提供 --db（或文件不存在），使用内置句子集")
        return [(s, "") for s in DEFAULT_SENTENCES[:limit]]

    uri = "file:" + db_path.replace("\\", "/") + "?mode=ro"
    con = sqlite3.connect(uri, uri=True)
    rows = con.execute(
        "SELECT sentence, translation FROM sentence ORDER BY score DESC LIMIT 2000"
    ).fetchall()
    con.close()

    picked = []
    for ja, zh in rows:
        if not ja or not CJK.search(ja):
            continue  # 只挑含汉字的句子 —— 这是 G2P 的命门
        if len(ja) < 6 or len(ja) > 40:
            continue
        picked.append((ja, zh or ""))
        if len(picked) >= limit:
            break
    print("[i] 从库里挑了 %d 句含汉字的真实例句" % len(picked))
    return picked


def main():
    ap = argparse.ArgumentParser(description="piper-plus 日语 TTS spike")
    ap.add_argument("--model", required=True, help="onnx 模型路径")
    ap.add_argument("--config", default=None, help="config.json 路径（可省）")
    ap.add_argument("--db", default=None, help="项目词典库路径（取真实例句）")
    ap.add_argument("--out", default="tts-spike-out", help="wav 输出目录")
    ap.add_argument("--limit", type=int, default=20, help="合成多少句")
    ap.add_argument("--length-scale", type=float, default=None,
                    help="语速（>1 更慢，piper-plus 默认偏快时可调 1.1~1.2）")
    args = ap.parse_args()

    try:
        from piper import PiperVoice
    except ImportError:
        print("[X] 没装 piper-plus。先执行： pip install -U piper-plus")
        return 2

    os.makedirs(args.out, exist_ok=True)

    print("=" * 78)
    print(" 1) 加载模型")
    print("=" * 78)
    t0 = time.perf_counter()
    voice = PiperVoice.load(args.model, config_path=args.config)
    load_ms = (time.perf_counter() - t0) * 1000
    print("    模型: %s" % os.path.basename(args.model))
    print("    加载耗时: %.0f ms" % load_ms)
    cfg = getattr(voice, "config", None)
    if cfg is not None:
        print("    采样率: %s  说话人数: %s" %
              (getattr(cfg, "sample_rate", "?"), getattr(cfg, "num_speakers", "?")))

    sentences = load_sentences(args.db, args.limit)

    print()
    print("=" * 78)
    print(" 2) 逐句：G2P 音素 + 合成 + RTF")
    print("=" * 78)

    results = []
    for i, (ja, zh) in enumerate(sentences, 1):
        # --- G2P：直接看汉字有没有被正确读出来 ---
        phonemes = ""
        try:
            ph = voice.phonemize(ja)
            phonemes = ph[0] if isinstance(ph, (tuple, list)) else str(ph)
        except Exception as exc:  # noqa: BLE001 - spike 脚本，尽力而为
            phonemes = "<phonemize failed: %s>" % exc

        out_path = os.path.join(args.out, "%02d.wav" % i)
        t1 = time.perf_counter()
        with wave.open(out_path, "wb") as wf:
            kwargs = {}
            if args.length_scale is not None:
                kwargs["length_scale"] = args.length_scale
            voice.synthesize(ja, wf, **kwargs)
        infer_ms = (time.perf_counter() - t1) * 1000

        with wave.open(out_path) as wf:
            dur = wf.getnframes() / float(wf.getframerate())
        rtf = (infer_ms / 1000.0) / dur if dur else 0.0

        results.append({
            "index": i, "text": ja, "zh": zh, "file": out_path,
            "chars": len(ja), "audio_sec": round(dur, 3),
            "infer_ms": round(infer_ms, 1), "rtf": round(rtf, 3),
            "phonemes": phonemes,
        })

        print("  [%02d] %s" % (i, ja))
        if zh:
            print("       中文: %s" % zh)
        print("       音素: %s" % phonemes)
        print("       音频 %.2fs  推理 %.0fms  RTF %.3f" % (dur, infer_ms, rtf))

    print()
    print("=" * 78)
    print(" 3) 汇总")
    print("=" * 78)
    n = len(results)
    total_audio = sum(r["audio_sec"] for r in results)
    total_infer = sum(r["infer_ms"] for r in results) / 1000.0
    avg_rtf = (total_infer / total_audio) if total_audio else 0.0
    print("    句子数        %d" % n)
    print("    模型加载      %.0f ms" % load_ms)
    print("    音频总长      %.2f s" % total_audio)
    print("    推理总耗时    %.2f s" % total_infer)
    print("    平均 RTF      %.3f   （越小越快，<1 表示比实时快）" % avg_rtf)
    print("    模型体积      %.1f MB" % (os.path.getsize(args.model) / 1024 / 1024))
    print("    wav 输出      %s" % os.path.abspath(args.out))

    report = os.path.join(args.out, "report.json")
    with open(report, "w", encoding="utf-8") as fh:
        json.dump({"model": args.model, "model_mb": os.path.getsize(args.model) / 1024 / 1024,
                   "load_ms": round(load_ms, 1), "avg_rtf": round(avg_rtf, 3),
                   "sentences": results}, fh, ensure_ascii=False, indent=2)
    print("    报告          %s" % report)
    print()
    print("  接下来：把 %s 里的 wav 挨个听一遍，重点听含汉字的句子对不对。" % args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
