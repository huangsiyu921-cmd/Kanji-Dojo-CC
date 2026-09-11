<div align="center">

# Kanji Dojo CC
![Version Badge](https://img.shields.io/badge/version-v0.0.1alpha-blue?style=for-the-badge&labelColor=ffffff&color=ff5555)

</div>

## 关于本项目

Kanji Dojo CC 是基于 [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo) 的汉化与增强版本，在保留原版练习写字、记假名/汉字、SRS 复习、词典搜索等能力的基础上，提供更完整的中文体验。

### 本项目新增 / 改进
- 自定义主色：可在设置中配置主题主色
- TTS 语音：整词/假名发音。目前受 TTS 实现限制，暂时依赖所在系统的外部语音模型；待内置 TTS 完成后再添加对其他系统的支持
- UI 与 `.sql` 数据库释义汉化（持续推进中）

## 参与贡献

欢迎在 [Issues](https://github.com/huangsiyu921-cmd/Kanji-Dojo-CC/issues) 提 bug、许愿新功能，也欢迎提交 PR。特别欢迎为数据库贡献日英 → 日中的释义翻译。

## 构建

- 需要 JDK 17（AGP 8.5.2 的最低要求；JDK 25 会破坏 Gradle 8.7 内置的 Kotlin 编译器）
- Android：`./gradlew :app:assembleFdroidDebug`
- 桌面：`./gradlew :desktopApp:run`
- 快速校验：`./gradlew :core:compileDebugKotlinAndroid :core:compileKotlinJvm :desktopApp:compileKotlinJvm`

## Credits

本项目数据库与字形数据沿用原版 Kanji Dojo 的词典与字形资源，特此署名：

- **KanjiVG**
  - 提供笔画、部首信息
  - License: Creative Commons Attribution-Share Alike 3.0
  - Link: https://kanjivg.tagaini.net/
- **Kanji Dic**
  - 提供汉字信息（释义、读音、分类等）
  - License: Creative Commons Attribution-Share Alike 3.0
  - Link: http://www.edrdg.org/wiki/index.php/KANJIDIC_Project
- **Tanos by Jonathan Waller**
  - 提供汉字 JLPT 分级
  - License: Creative Commons BY
  - Link: http://www.tanos.co.uk/jlpt/
- **JMDict**
  - 日英多语言词典，提供词汇条目
  - License: Creative Commons Attribution-Share Alike 4.0
  - Link: https://www.edrdg.org/jmdict/j_jmdict.html
- **JmdictFurigana**
  - 为 EDICT/JMDict 与 ENAMDICT/JMnedict 补充振假名的开源资源
  - License: Creative Commons Attribution-Share Alike 4.0
  - Link: https://github.com/Doublevil/JmdictFurigana
- **Frequency list by Leeds university**
  - 提供词汇网络使用频率排名
  - License: Creative Commons BY
  - Link: http://corpus.leeds.ac.uk/list.html
- **yomichan-jlpt-vocab**
  - 为 Yomichan 词汇添加 JLPT 分级标签，关联 Tanos 与 JMDict
  - License: Creative Commons Attribution-Share Alike 4.0
  - Link: https://github.com/stephenmk/yomichan-jlpt-vocab

## License

本项目沿用原版 [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo) 的 GPL-3.0 许可证。

> (c) 2022-2023 Yaroslav Shuliak
>
> This is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
>
> This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
>
> You should have received a copy of the GNU General Public License along with this app. If not, see https://www.gnu.org/licenses/.
