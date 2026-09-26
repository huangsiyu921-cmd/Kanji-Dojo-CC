import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.hiroshiba.voicevoxcore.AudioQuery;
import jp.hiroshiba.voicevoxcore.CharacterMeta;
import jp.hiroshiba.voicevoxcore.StyleMeta;
import jp.hiroshiba.voicevoxcore.blocking.Onnxruntime;
import jp.hiroshiba.voicevoxcore.blocking.OpenJtalk;
import jp.hiroshiba.voicevoxcore.blocking.Synthesizer;
import jp.hiroshiba.voicevoxcore.blocking.VoiceModelFile;

/**
 * M1 第一步：VOICEVOX CORE の公式 Java API を最小構成で叩けるか確認する spike。
 *
 * <p>Python 版 {@code spike_voicevox.py} と同一の 13 ケース・同一パラメータで合成し、 得られた wav を Python 版と
 * 突き合わせることで「Java 側の API の使い方が正しいか」を検証する。
 *
 * <pre>
 * java -cp "voicevoxcore-0.17.0.jar;gson.jar;jakarta*.jar" VvSpike \
 *     --core  D:\voicevox-spike\core \
 *     --model D:\voicevox-spike\models\4.vvm \
 *     --style-id 11 \
 *     --out   D:\Kanji-Dojo\tools\tts-spike\java\out\wav
 * </pre>
 */
public final class VvSpike {

  /** Python spike と同一のケース（単音・短詞は piper-plus が失敗した領域）。 */
  private static final String[][] CASES = {
    {"single-kana-a", "あ"},
    {"single-kana-n", "ん"},
    {"single-kana-ki", "き"},
    {"single-kanji-watashi", "私"},
    {"single-kanji-ki", "木"},
    {"single-kanji-mizu", "水"},
    {"short-word", "学校"},
    {"short-word-2", "ありがとう"},
    {"short-word-3", "こんにちは"},
    {"sentence-basic", "数学は私には難しい"},
    {"sentence-long", "経済成長率の低下が深刻化している"},
    {"sentence-number", "私は一日に100ユーロ稼ぎます"},
    {"sentence-dialog", "「実を言うと、俺、高所恐怖症なんだ」「臆病者っ！」"},
  };

  private static PrintStream out;

  public static void main(String[] args) throws Exception {
    out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

    String coreDir = "D:\\voicevox-spike\\core";
    String modelPath = "D:\\voicevox-spike\\models\\4.vvm";
    String outDir = "D:\\Kanji-Dojo\\tools\\tts-spike\\java\\out\\wav";
    int styleId = 11;
    double speedScale = 1.0;
    double pitchScale = 0.0;
    double intonationScale = 1.0;
    double prePhonemeLength = 0.10;
    double postPhonemeLength = 0.50;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--core" -> coreDir = args[++i];
        case "--model" -> modelPath = args[++i];
        case "--out" -> outDir = args[++i];
        case "--style-id" -> styleId = Integer.parseInt(args[++i]);
        case "--speed-scale" -> speedScale = Double.parseDouble(args[++i]);
        case "--pitch-scale" -> pitchScale = Double.parseDouble(args[++i]);
        case "--intonation-scale" -> intonationScale = Double.parseDouble(args[++i]);
        case "--pre-phoneme-length" -> prePhonemeLength = Double.parseDouble(args[++i]);
        case "--post-phoneme-length" -> postPhonemeLength = Double.parseDouble(args[++i]);
        default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
      }
    }

    Path onnxPath = findFile(Paths.get(coreDir), "voicevox_onnxruntime.dll");
    Path dictDir = findDir(Paths.get(coreDir), "open_jtalk_dic_utf_8");
    if (onnxPath == null || dictDir == null) {
      out.println("[X] missing onnxruntime or open_jtalk dictionary under " + coreDir);
      System.exit(2);
    }

    out.println("=".repeat(78));
    out.println(" 0) environment");
    out.println("=".repeat(78));
    out.println("    onnxruntime : " + onnxPath);
    out.println("    open_jtalk  : " + dictDir);
    out.println("    model       : " + modelPath);
    out.println("    style_id    : " + styleId);
    out.println("    post/pre    : " + postPhonemeLength + " / " + prePhonemeLength);

    long t0 = System.nanoTime();
    Onnxruntime onnxruntime = Onnxruntime.loadOnce().filename(onnxPath.toString()).perform();
    OpenJtalk openJtalk = new OpenJtalk(dictDir.toString());
    Synthesizer synth = Synthesizer.builder(onnxruntime, openJtalk).build();

    Path outPath = Paths.get(outDir);
    Files.createDirectories(outPath);

    int ok = 0;
    List<String> wavNames = new ArrayList<>();
    try (VoiceModelFile model = new VoiceModelFile(modelPath)) {
      synth.loadVoiceModel(model).perform();
      long loadMs = (System.nanoTime() - t0) / 1_000_000;
      out.println("    load_ms     : " + loadMs);

      out.println("    speaker     : " + describeModel(model));

      out.println();
      out.println("=".repeat(78));
      out.println(" 1) synthesis (" + CASES.length + " cases)");
      out.println("=".repeat(78));

      double totalAudio = 0.0;
      double totalInferMs = 0.0;
      for (int i = 0; i < CASES.length; i++) {
        String name = CASES[i][0];
        String text = CASES[i][1];
        String fileName = String.format(Locale.ROOT, "%02d_%s.wav", i + 1, name);
        Path file = outPath.resolve(fileName);
        String status;
        try {
          long t1 = System.nanoTime();
          AudioQuery query = synth.createAudioQuery(text, styleId);
          query.speedScale = speedScale;
          query.pitchScale = pitchScale;
          query.intonationScale = intonationScale;
          query.prePhonemeLength = prePhonemeLength;
          query.postPhonemeLength = postPhonemeLength;
          byte[] wav = synth.synthesis(query, styleId).perform();
          long inferMs = (System.nanoTime() - t1) / 1_000_000;
          Files.write(file, wav);

          double dur = wavSeconds(wav);
          totalAudio += dur;
          totalInferMs += inferMs;
          ok++;
          wavNames.add(fileName);
          status =
              String.format(
                  Locale.ROOT,
                  "audio %.2fs  infer %4dms  rtf %.3f  %d bytes",
                  dur,
                  inferMs,
                  dur > 0 ? (inferMs / 1000.0) / dur : 0.0,
                  wav.length);
        } catch (Exception e) {
          status = "[X] " + e;
        }
        out.printf(Locale.ROOT, "  [%02d] %-22s %-18s %s%n", i + 1, name, text, status);
      }

      out.println();
      out.println("=".repeat(78));
      out.println(" 2) summary");
      out.println("=".repeat(78));
      out.printf(Locale.ROOT, "    success      %d / %d%n", ok, CASES.length);
      out.printf(Locale.ROOT, "    audio total  %.2f s%n", totalAudio);
      out.printf(Locale.ROOT, "    infer total  %.2f s%n", totalInferMs / 1000.0);
      out.printf(
          Locale.ROOT,
          "    average RTF  %.3f%n",
          totalAudio > 0 ? (totalInferMs / 1000.0) / totalAudio : 0.0);
      out.println("    wav output   " + outPath.toAbsolutePath());
    }

    out.println();
    out.println("    file list for diff:");
    for (String n : wavNames) {
      out.println("      " + n);
    }
    System.exit(ok == CASES.length ? 0 : 1);
  }

  private static String describeModel(VoiceModelFile model) {
    StringBuilder sb = new StringBuilder();
    sb.append(model.id);
    for (CharacterMeta meta : model.metas) {
      sb.append(" | ").append(meta.name).append(" [");
      for (int i = 0; i < meta.styles.length; i++) {
        StyleMeta style = meta.styles[i];
        if (i > 0) sb.append(", ");
        sb.append(style.id).append(':').append(style.name);
      }
      sb.append(']');
    }
    return sb.toString();
  }

  private static Path findFile(Path root, String name) throws Exception {
    try (var stream = Files.walk(root)) {
      return stream.filter(p -> p.getFileName().toString().equals(name)).findFirst().orElse(null);
    }
  }

  private static Path findDir(Path root, String prefix) throws Exception {
    try (var stream = Files.walk(root)) {
      return stream
          .filter(p -> Files.isDirectory(p) && p.getFileName().toString().startsWith(prefix))
          .findFirst()
          .orElse(null);
    }
  }

  /** RIFF ヘッダから再生時間（秒）を求める。 */
  private static double wavSeconds(byte[] wav) {
    int pos = 12;
    while (pos + 8 <= wav.length) {
      String id = new String(wav, pos, 4, StandardCharsets.US_ASCII);
      int size = le32(wav, pos + 4);
      if (id.equals("fmt ") && pos + 8 + 16 <= wav.length) {
        int byteRate = le32(wav, pos + 8 + 8);
        int dataSize = dataSize(wav, pos + 8 + size);
        if (byteRate > 0 && dataSize >= 0) {
          return dataSize / (double) byteRate;
        }
      }
      pos += 8 + size + (size & 1);
    }
    return 0.0;
  }

  private static int dataSize(byte[] wav, int pos) {
    while (pos + 8 <= wav.length) {
      String id = new String(wav, pos, 4, StandardCharsets.US_ASCII);
      int size = le32(wav, pos + 4);
      if (id.equals("data")) return size;
      pos += 8 + size + (size & 1);
    }
    return -1;
  }

  private static int le32(byte[] b, int off) {
    return (b[off] & 0xFF)
        | ((b[off + 1] & 0xFF) << 8)
        | ((b[off + 2] & 0xFF) << 16)
        | ((b[off + 3] & 0xFF) << 24);
  }
}
