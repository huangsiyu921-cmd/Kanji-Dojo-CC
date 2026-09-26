#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""方案 B spike —— VOICEVOX CORE 日语 TTS 评估（单音 / 短词 / 长句 / 语速）。

为什么要评估它
--------------------------------------------------
piper-plus 调参治不了「连音严重 + 单字效果差」——那是 VITS 句子级模型的固有特性
（训练语料里没有单音素样本，1~2 个音素的输入合成极不稳定）。
VOICEVOX CORE 是日语母语级 TTS，且官方提供全平台预编译产物：
  - Android      → voicevox_core-android-*.zip / java_packages.zip（含 AAR）
  - Windows/Linux/macOS → C API + **Java API**（JVM 可直接用，不用自研 JNI）
  - iOS          → voicevox_core-xcframework-*.zip（已过 App Store 审核）

前置准备（一次性）
--------------------------------------------------
  # 1) 装 Python 包（GitHub Releases 里的 wheel，不在 PyPI）
  pip install voicevox_core-0.17.0-cp310-abi3-win_amd64.whl

  # 2) 下载 onnxruntime + OpenJTalk 词典（需同意许可，交互输入 y）
  download-windows-x64.exe --only onnxruntime dict -o D:\\voicevox-spike\\core

  # 3) 下载音色模型（男声：4.vvm = 玄野武宏 style 11，9.vvm = 白上虎太郎 style 12）
  #    https://github.com/VOICEVOX/voicevox_vvm/releases

用法
--------------------------------------------------
  python spike_voicevox.py \
      --core  "D:\\voicevox-spike\\core" \
      --model "D:\\voicevox-spike\\models\\4.vvm" \
      --style-id 11 \
      --out   "%USERPROFILE%\\Desktop\\tts-spike\\voicevox"

许可提醒
--------------------------------------------------
  CORE 本体 MIT；**音色需署名**（例：`VOICEVOX:玄野武宏`），商用/再分发允许。
  避开需要企业事前确认的角色（青山龍星 / もち子さん / 後鬼 / No.7）。
"""

import argparse
import glob
import json
import os
import sys
import time
import wave

# 专项测试集：前几行专门针对「单音 / 短词」——piper-plus 的痛点
CASES = [
    ("single-kana-a", "あ"),
    ("single-kana-n", "ん"),
    ("single-kana-ki", "き"),
    ("single-kanji-watashi", "私"),
    ("single-kanji-ki", "木"),
    ("single-kanji-mizu", "水"),
    ("short-word", "学校"),
    ("short-word-2", "ありがとう"),
    ("short-word-3", "こんにちは"),
    ("sentence-basic", "数学は私には難しい"),
    ("sentence-long", "経済成長率の低下が深刻化している"),
    ("sentence-number", "私は一日に100ユーロ稼ぎます"),
    ("sentence-dialog", "「実を言うと、俺、高所恐怖症なんだ」「臆病者っ！」"),
]


def wav_stats(path):
    """(时长秒, 整体RMS, 末尾50ms RMS)"""
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
    rms = lambda a: float(np.sqrt((a ** 2).mean())) if len(a) else 0.0  # noqa: E731
    return dur, rms(data), rms(data[-int(sr * 0.05):])


def find_onnxruntime(core_dir):
    # 注意：VOICEVOX 自带的是 voicevox_onnxruntime.dll，不是标准 onnxruntime.dll
    patterns = (
        "voicevox_onnxruntime.dll", "*onnxruntime*.dll",
        "libvoicevox_onnxruntime.so", "libonnxruntime.so",
        "libvoicevox_onnxruntime.dylib", "libonnxruntime.dylib",
    )
    for pat in patterns:
        hits = sorted(glob.glob(os.path.join(core_dir, "**", pat), recursive=True))
        if hits:
            return hits[0]
    return None


def find_dict(core_dir):
    for name in ("open_jtalk_dic_utf_8-1.11", "open_jtalk_dic_utf_8-1.10",
                 "open_jtalk_dic_utf_8-1.09"):
        p = os.path.join(core_dir, name)
        if os.path.isdir(p):
            return p
    hits = [d for d in glob.glob(os.path.join(core_dir, "**", "open_jtalk_dic*"), recursive=True)
            if os.path.isdir(d)]
    return hits[0] if hits else None


def main():
    ap = argparse.ArgumentParser(description="VOICEVOX CORE 日语 TTS spike")
    ap.add_argument("--core", required=True, help="download-windows-*.exe 的输出目录")
    ap.add_argument("--model", required=True, help=".vvm 音色模型路径")
    ap.add_argument("--style-id", type=int, default=11,
                    help="スタイルID（玄野武宏 ノーマル=11，白上虎太郎 ふつう=12）")
    ap.add_argument("--out", default="tts-spike-voicevox", help="wav 输出目录")
    ap.add_argument("--speed-scale", type=float, default=1.0,
                    help="语速（VOICEVOX 默认 1.0；<1 更慢？注意：>1 更快）")
    ap.add_argument("--pitch-scale", type=float, default=0.0)
    ap.add_argument("--intonation-scale", type=float, default=1.0,
                    help="抑扬顿挫（<1 更平，>1 更夸张）")
    args = ap.parse_args()

    try:
        from voicevox_core.blocking import Onnxruntime, OpenJtalk, Synthesizer, VoiceModelFile
    except ImportError:
        print("[X] 没装 VOICEVOX CORE 的 Python 包（不在 PyPI，要从 GitHub Releases 下 wheel）")
        return 2

    os.makedirs(args.out, exist_ok=True)

    onnx_path = find_onnxruntime(args.core)
    dict_dir = find_dict(args.core)
    print("=" * 78)
    print(" 0) 环境检查")
    print("=" * 78)
    print("    onnxruntime : %s" % onnx_path)
    print("    open_jtalk  : %s" % dict_dir)
    print("    模型        : %s (%.1f MB)" % (os.path.basename(args.model),
                                              os.path.getsize(args.model) / 1024 / 1024))
    print("    style_id    : %d" % args.style_id)
    if not onnx_path or not dict_dir:
        print("[X] 缺 onnxruntime 或词典 —— 先跑 download-windows-x64.exe --only onnxruntime dict")
        return 2

    t0 = time.perf_counter()
    onnxruntime = Onnxruntime.load_once(filename=onnx_path)
    open_jtalk = OpenJtalk(dict_dir)
    synth = Synthesizer(onnxruntime, open_jtalk)
    if hasattr(VoiceModelFile, "open"):
        model = VoiceModelFile.open(args.model)
    else:
        model = VoiceModelFile(args.model)
    synth.load_voice_model(model)
    load_ms = (time.perf_counter() - t0) * 1000
    print("    加载耗时    : %.0f ms" % load_ms)

    print()
    print("=" * 78)
    print(" 1) 逐条合成（单音/短词在前，重点看 piper 翻车的地方）")
    print("=" * 78)

    results = []
    truncated = []
    for i, (name, text) in enumerate(CASES, 1):
        out_path = os.path.join(args.out, "%02d_%s.wav" % (i, name))
        t1 = time.perf_counter()
        try:
            query = synth.create_audio_query(text, args.style_id)
            query.speed_scale = args.speed_scale
            query.pitch_scale = args.pitch_scale
            query.intonation_scale = args.intonation_scale
            wav_bytes = synth.synthesis(query, args.style_id)
            with open(out_path, "wb") as fh:
                fh.write(wav_bytes)
        except Exception as exc:  # noqa: BLE001
            print("  [%02d] %-22s %s   ✗ 失败: %s" % (i, name, text, exc))
            results.append({"index": i, "name": name, "text": text, "error": str(exc)})
            continue
        infer_ms = (time.perf_counter() - t1) * 1000

        dur, overall, tail = wav_stats(out_path)
        rtf = (infer_ms / 1000.0) / dur if dur else 0.0
        is_trunc = bool(overall and tail is not None and tail > 0.25 * overall)
        if is_trunc:
            truncated.append(i)

        results.append({"index": i, "name": name, "text": text, "file": out_path,
                        "audio_sec": round(dur, 3), "infer_ms": round(infer_ms, 1),
                        "rtf": round(rtf, 3), "truncated": is_trunc})
        print("  [%02d] %-22s %-30s 音频 %.2fs  推理 %5.0fms  RTF %.3f%s"
              % (i, name, text, dur, infer_ms, rtf, "  ★截断" if is_trunc else ""))

    print()
    print("=" * 78)
    print(" 2) 汇总")
    print("=" * 78)
    ok = [r for r in results if "error" not in r]
    total_audio = sum(r["audio_sec"] for r in ok)
    total_infer = sum(r["infer_ms"] for r in ok) / 1000.0
    print("    成功/总数     %d / %d" % (len(ok), len(results)))
    print("    模型加载      %.0f ms" % load_ms)
    print("    音频总长      %.2f s" % total_audio)
    print("    推理总耗时    %.2f s" % total_infer)
    print("    平均 RTF      %.3f" % ((total_infer / total_audio) if total_audio else 0.0))
    print("    结尾截断      %s" % (("★ %s" % truncated) if truncated else "无 ✅"))
    print("    语速 scale    %.2f  抑扬 %.2f" % (args.speed_scale, args.intonation_scale))
    print("    wav 输出      %s" % os.path.abspath(args.out))

    report = os.path.join(args.out, "report.json")
    with open(report, "w", encoding="utf-8") as fh:
        json.dump({"model": args.model, "style_id": args.style_id,
                   "speed_scale": args.speed_scale,
                   "intonation_scale": args.intonation_scale,
                   "load_ms": round(load_ms, 1),
                   "truncated_indices": truncated, "results": results},
                  fh, ensure_ascii=False, indent=2)
    print("    报告          %s" % report)
    print()
    print("  重点听：01~06（单音/单字）—— 这是 piper-plus 翻车的地方；再看 13（对话）连音。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
