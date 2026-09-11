# Kanji Dojo 改造交接文档

> 给下一个窗口/agent。项目根：`D:\Kanji-Dojo`（git 仓库，分支 `develop`）。

## 1. 项目与技术栈
- **Kotlin Multiplatform + Compose Multiplatform**：Kotlin 2.1.20、AGP 8.5.2、Gradle 8.7、Compose 1.8.2
- 模块：`app`(Android) · `desktopApp`(JVM 桌面) · `iosApp` · `core`(共享，主战场) · `mediaGenerator`
- `origin` = `syt0r/Kanji-Dojo`（**无推送权限**，push 需先配自己的 fork）

## 2. 环境与构建（务必先看）
- **JDK**：系统 `JAVA_HOME` 指向 JDK 25，会让 Gradle 8.7 内置 Kotlin DSL 崩溃（`IllegalArgumentException: 25.0.3`）。已在 `gradle.properties` 固定：
  ```
  org.gradle.java.home=C:/Program Files/Microsoft/jdk-17.0.20.101-hotspot
  org.gradle.java.installations.paths=C:/Program Files/Microsoft/jdk-17.0.20.101-hotspot
  ```
- **仓库镜像**：国内直连 google()/mavenCentral() 不通，已在 `settings.gradle.kts`、`build.gradle.kts`、`buildSrc/build.gradle.kts` 加阿里云镜像；`gradle-wrapper.properties` 用腾讯云镜像
- **命令**
  - Android：`.\gradlew.bat :app:assembleFdroidDebug`（用 `fdroid` flavor；`googlePlay` 缺 `google-services.json` 会失败）
  - 桌面：`.\gradlew.bat :desktopApp:run`
  - 快速校验：`.\gradlew.bat :core:compileDebugKotlinAndroid :core:compileKotlinJvm :desktopApp:compileKotlinJvm`
- **数据资产**：`core/src/commonMain/composeResources/files/kanji-dojo-data-base-v15.sql` —— **它其实是 SQLite 数据库文件**（不是 SQL 脚本）。app 通过 `BuildConfig.appDataAssetName`(=`kanji-dojo-data-base-v15.sql`) 从 compose 资源读取它。`.gitignore` 忽略 `*.sql`/`*.wav`/`*.opus`

## 3. 已完成的改动
1. **UI 中文化**
   - `ChineseStrings.kt`（镜像 `EnglishStrings`，`getStrings()` 加 `"zh"`）
   - `composeResources/values/strings.xml` 翻成中文
2. **主题配色（重点）**
   - 原「动态取色」用 MaterialKolor，与 Compose 1.8/material3 版本不兼容导致**全黑/背景不联动**，已**彻底移除**（依赖 `com.materialkolor:material-kolor` 已从 `libs.versions.toml` + `core/build.gradle.kts` 删除）
   - 现在 `Theme.kt` 是**静态 M3 配色**（`lightScheme`/`darkScheme`/`amoledScheme`，明/暗/AMOLED 全字段固定）
   - **自定义主色（静态）**：设置在「主题」项下方输入 `#RRGGBB` → 仅**主色**(`primary`/`onPrimary` 自动黑白对比)变化，其余固定
     - 链路：`ThemeSettingItem`(输入框) → `ThemeManager.currentCustomSeedColor` → `AppTheme`
     - 注意：字段名仍是 `customSeedColor`（语义已变为「自定义主色」）
3. **字体 locale**：`Typography.kt` 移除了强制 `LocaleList("ja")`（修中文排版偏移）
4. **音效（双端）**
   - `PracticeSoundEffect { Click, Correct, Incorrect, Finish }`
   - Android：`SoundPool` 播 `raw/whenright|wrong|click|finish`（`.wav`）
   - 桌面 JVM：`javax.sound` + `mp3spi` 播 `composeResources/files/sounds/*.wav`，异步、并发、Clip 保引用
   - 注入：`AppTheme` 里 `rememberPracticeSoundPlayer()` + `LocalPracticeSounds`
   - 触发点：写字笔顺完成(对/错)、评分按钮/flashcard 翻转/键盘(点击)、**读音选择**(选项点击=Click、选中=对/错)、练习「结束!」按钮=Finish
5. **TTS**：`WordTtsManager`（Android 系统 `TextToSpeech` 选本地日语 voice / JVM PowerShell+SAPI+回退 / iOS AVSpeech）；DI 绑定；`VocabInfoUI` 词详情有 🔊 按钮
6. **词组卡片发音**：`VocabPracticeFlashcardUI` 顶部加 🔊（`WordTtsManager` 读卡上假名）
7. **数据库释义中文化**：`kanji_meaning.meaning` 已回填中文（**16679 / 16692 行**）。英文原库备份在 `C:\Users\Sensei\Desktop\dsdownload\kanji-dojo-data-base-v15-EN.bak`
8. **修复桌面崩溃**：`KanjiDojoApp` 用 `CompositionLocalProvider(LocalThemeManager provides themeManager)` 包裹 `AppTheme`

## 4. 待办 / 未完成
- **`vocab_entry` 等表约 5 万行释义未翻译**（用户明确表示量大、暂不逐条翻）
- **TTS 发音 UI**：用户希望做成「letters 学习页下方」那种样式（该页 `KanaVoiceMenu` 已有假名发音，可再加 `WordTtsManager` 整词发音按钮）
- 音效触发点可继续完善（如更多练习类型）
- `kanji_meaning` 约 **11 行未匹配**（`10**12`/`10**8` 等被翻译时丢了 `**`）仍为英文
- push 到 GitHub：需先把 `origin` 换/加为自己的 fork

## 5. 已知坑
- 删 `material-kolor` 依赖时，曾出现 `EnglishStrings.kt`/`JapaneseStrings.kt` 的 `Duration` 相关 **overload ambiguity / conflicting overloads**（可用 `:core:compileDebugKotlinAndroid` 复现）。本次删除后编译正常——若再波动，检查是否有重复的顶层函数
- 桌面播 `.mp3` 需要 `mp3spi`；现已统一改用 `.wav`（`javax.sound` 原生支持）
- 改 `composeResources/files` 下的库后**必须重新 build**，否则 build assets 里还是旧库（app 不会自动感知）
- 数据库是 SQLite 文件，读写用 `sqlite3`；表：`kanji_meaning(kanji,meaning,priority)`、`vocab_entry` 等

## 6. 相关文件速查
- 主题：`core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/common/theme/{Theme.kt,Color.kt,Typography.kt}`
- 自定义主色链路：`.../settings/items/ThemeSettingItem.kt` · `core/.../core/theme_manager/ThemeManager.kt` · `core/.../core/user_data/preferences/{PreferencesContract.kt,AppPreferences.kt}`
- 音效：`core/src/{commonMain,androidMain,jvmMain}/.../presentation/common/sound/`
- TTS：`core/src/{commonMain,androidMain,jvmMain,iosMain}/.../core/tts/`
- 字符串：`core/.../presentation/common/resources/string/{ChineseStrings.kt,String.kt}`、`composeResources/values/strings.xml`
- 数据：`core/src/commonMain/composeResources/files/kanji-dojo-data-base-v15.sql`
- 音效素材/翻译源：`C:\Users\Sensei\Desktop\dsdownload\`
