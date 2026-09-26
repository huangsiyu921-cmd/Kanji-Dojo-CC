#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""S0/S3 spike —— 验证「piper-plus 内置日语 TTS」这条路线能不能用。

背景（为什么不是 sherpa-onnx + Kokoro）
--------------------------------------------------
v2 规划原本选 sherpa-onnx + Kokoro-82M INT8，核实后有两个硬伤：
  1. sherpa-onnx 官方**没有**日语 TTS 模型，日语在 issue #3028 里被列为"缺失语言"
     （官方只有日语 ASR 模型）。
  2. sherpa-onnx 的 Kokoro 多语言模型里，非中文 CJK 一律走 espeak-ng；
     espeak-ng 没有日语形态素词典，汉字会被念成"字符描述"。
本脚本用来验证替代路线：piper-plus（OpenJTalk G2P + VITS，MIT）。

这个脚本做三件事
--------------------------------------------------
1. G2P 正确性：打印每句的音素序列，直接看汉字有没有被正确读出来
2. 音质样本：把 wav 输出到你指定的目录，自己听
3. 性能与质量体检：模型加载时间、逐句 RTF、**结尾截断检测**
   （piper-plus 的 sentence_silence 默认 0.0，句末不留静音 → 最后一块波形被硬切，
    实测 26 个样本里有 5 个中招，最严重的末 50ms RMS 达到整体的 73%）

用法
--------------------------------------------------
  python spike_ja_tts.py \
      --model   "D:\\piper-plus-models\\tsukuyomi-chan-6lang-fp16.onnx" \
      --config  "D:\\piper-plus-models\\tsukuyomi-chan-6lang-fp16.json" \
      --db      "D:\\Kanji-Dojo\\SQL翻译中\\kanji-dojo-data-base-v15-汉化中.sql" \
      --out     "%USERPROFILE%\\Desktop\\tts-spike" \
      --limit   20

不传 --db 时用内置的默认句子集（离线可跑）。
"""

import argparse
import glob
import json
import os
import re
import sqlite3
import sys
import time
import wave

CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")

# ---------------------------------------------------------------------------
# 试听定档参数（2026-09-23，女声 tsukuyomi-chan）
#   语速 1.35（模型 config 默认 1.0 偏快）
#   句末静音 0.20s（修 piper-plus 默认 0.0 导致的结尾截断）
#   降噪 noise_scale 0.55 / noise_w 0.70（模型默认 0.667 / 0.8，"气息重"）
# ---------------------------------------------------------------------------
DEFAULT_LENGTH_SCALE = 1.35
DEFAULT_SENTENCE_SILENCE = 0.20
DEFAULT_NOISE_SCALE = 0.55
DEFAULT_NOISE_W = 0.70

# 结尾截断判定：末尾 50ms 的 RMS 超过整体 RMS 的这个比例，就认为"还在发声就被切了"
TAIL_TRUNCATION_RATIO = 0.25

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


def wav_stats(path):
    """返回 (时长秒, 整体RMS, 末尾50ms RMS)。RMS 用 numpy；没装就返回 None。"""
    with wave.open(path) as wf:
        sr, n = wf.getframerate(), wf.getnframes()
        wf.rewind()
        raw = wf.readframes(n)
    dur = n / float(sr)
    try:
        import numpy as np
    except ImportError:
        return dur, None, None
    data = np.frombuffer(raw, dtype=np.int16).astype(np.float32)
    if not len(data):
        return dur, 0.0, 0.0

    def rms(a):
        return float(np.sqrt((a ** 2).mean())) if len(a) else 0.0

    tail = data[-int(sr * 0.05):]
    return dur, rms(data), rms(tail)


def main():
    ap = argparse.ArgumentParser(description="piper-plus 日语 TTS spike")
    ap.add_argument("--model", required=True, help="onnx 模型路径")
    ap.add_argument("--config", default=None, help="config.json 路径（每个模型要各自一份，别共用）")
    ap.add_argument("--db", default=None, help="项目词典库路径（取真实例句）")
    ap.add_argument("--out", default="tts-spike-out", help="wav 输出目录")
    ap.add_argument("--limit", type=int, default=20, help="合成多少句")
    ap.add_argument("--length-scale", type=float, default=DEFAULT_LENGTH_SCALE,
                    help="语速（>1 更慢；试听定档 %.2f）" % DEFAULT_LENGTH_SCALE)
    ap.add_argument("--sentence-silence", type=float, default=DEFAULT_SENTENCE_SILENCE,
                    help="句末静音秒数（0 会结尾被切；定档 %.2f）" % DEFAULT_SENTENCE_SILENCE)
    ap.add_argument("--noise-scale", type=float, default=DEFAULT_NOISE_SCALE,
                    help="生成噪声（模型默认 0.667；定档 %.2f）" % DEFAULT_NOISE_SCALE)
    ap.add_argument("--noise-w", type=float, default=DEFAULT_NOISE_W,
                    help="音素宽度噪声（模型默认 0.8；定档 %.2f）" % DEFAULT_NOISE_W)
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
    print("    参数: length_scale=%.2f sentence_silence=%.2f noise_scale=%.2f noise_w=%.2f"
          % (args.length_scale, args.sentence_silence, args.noise_scale, args.noise_w))

    sentences = load_sentences(args.db, args.limit)

    print()
    print("=" * 78)
    print(" 2) 逐句：G2P 音素 + 合成 + RTF + 截断检测")
    print("=" * 78)

    results = []
    truncated = []
    for i, (ja, zh) in enumerate(sentences, 1):
        # --- G2P：直接看汉字有没有被正确读出来 ---
        phonemes = ""
        try:
            ph = voice.phonemize(ja)
            # phonemize 返回 list[list[str]]（按句切分），多句时全打印
            if isinstance(ph, (tuple, list)) and ph and isinstance(ph[0], (tuple, list)):
                phonemes = " ‖ ".join("".join(seg) for seg in ph)
            else:
                phonemes = str(ph)
        except Exception as exc:  # noqa: BLE001 - spike 脚本，尽力而为
            phonemes = "<phonemize failed: %s>" % exc

        out_path = os.path.join(args.out, "%02d.wav" % i)
        t1 = time.perf_counter()
        with wave.open(out_path, "wb") as wf:
            voice.synthesize(
                ja, wf,
                length_scale=args.length_scale,
                noise_scale=args.noise_scale,
                noise_w=args.noise_w,
                sentence_silence=args.sentence_silence,
            )
        infer_ms = (time.perf_counter() - t1) * 1000

        dur, overall, tail = wav_stats(out_path)
        rtf = (infer_ms / 1000.0) / dur if dur else 0.0
        is_trunc = bool(overall and tail is not None and tail > TAIL_TRUNCATION_RATIO * overall)
        if is_trunc:
            truncated.append(i)

        results.append({
            "index": i, "text": ja, "zh": zh, "file": out_path,
            "chars": len(ja), "audio_sec": round(dur, 3),
            "infer_ms": round(infer_ms, 1), "rtf": round(rtf, 3),
            "tail_rms": round(tail, 1) if tail is not None else None,
            "overall_rms": round(overall, 1) if overall is not None else None,
            "truncated": is_trunc,
            "phonemes": phonemes,
        })

        print("  [%02d] %s" % (i, ja))
        if zh:
            print("       中文: %s" % zh)
        print("       音频 %.2fs  推理 %.0fms  RTF %.3f%s"
              % (dur, infer_ms, rtf, "   ★结尾疑似截断" if is_trunc else ""))

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
    print("    结尾截断      %s" % (("★ %s" % truncated) if truncated else "无 ✅"))
    print("    wav 输出      %s" % os.path.abspath(args.out))

    report = os.path.join(args.out, "report.json")
    with open(report, "w", encoding="utf-8") as fh:
        json.dump({
            "model": args.model,
            "model_mb": round(os.path.getsize(args.model) / 1024 / 1024, 1),
            "params": {
                "length_scale": args.length_scale,
                "sentence_silence": args.sentence_silence,
                "noise_scale": args.noise_scale,
                "noise_w": args.noise_w,
            },
            "load_ms": round(load_ms, 1),
            "avg_rtf": round(avg_rtf, 3),
            "truncated_indices": truncated,
            "sentences": results,
        }, fh, ensure_ascii=False, indent=2)
    print("    报告          %s" % report)
    print()
    print("  接下来：把 %s 里的 wav 挨个听一遍，重点听含汉字的句子对不对。" % args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
