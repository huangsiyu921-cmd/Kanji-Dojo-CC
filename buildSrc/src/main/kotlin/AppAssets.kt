data class Asset(
    val fileName: String,
    val url: String?
)

data class AssetLocation(
    val expectedAssets: List<Asset>
)

object AppAssets {

    // 资源文件名保持 GitHub 原版资源的名字（v15），不要跟着内容版本走：
    // PrepareAssetsTask 会删掉不在 expectedAssets 里的文件，并在文件缺失时按 url 重新下载原版英文库。
    const val AppDataAssetFileName = "kanji-dojo-data-base-v15.sql"

    // 词典库内容版本：每次替换/更新库都要 +1。
    // 运行时会和已安装副本的 PRAGMA user_version 比对，不一致就用新资源覆盖，
    // 否则老用户会一直用第一次安装时复制过去的旧库。
    const val AppDataDatabaseVersion = 17

    val kanaVoiceOpus = Asset(
        fileName = "ja-JP-Neural2-B.opus",
        url = "https://github.com/syt0r/Kanji-Dojo-Data/releases/download/voice-v1/ja-JP-Neural2-B.opus"
    )

    val kanaVoiceWav = Asset(
        fileName = "ja-JP-Neural2-B.wav",
        url = "https://github.com/syt0r/Kanji-Dojo-Data/releases/download/voice-v1/ja-JP-Neural2-B.wav"
    )

    val CommonAssetsLocation = AssetLocation(
        expectedAssets = listOf(
            Asset(
                fileName = AppDataAssetFileName,
                url = "https://github.com/syt0r/Kanji-Dojo-Data/releases/download/v15.0/kanji-dojo-data-base-v15.sql"
            ),
            Asset(
                fileName = "text_analysis_preview.json",
                url = null
            ),
            // 练习音效目录（桌面端用 Res.readBytes("files/sounds/xxx.wav") 读取）。
            // 必须登记，否则 prepareKanjiDojoAssets 会把没列出的顶层条目当 unknown 删掉。
            Asset(
                fileName = "sounds",
                url = null
            )
        )
    )

    val AndroidAssetsLocation = AssetLocation(
        expectedAssets = listOf(kanaVoiceOpus)
    )

    val DesktopAssetsLocation = AssetLocation(
        expectedAssets = listOf(kanaVoiceWav)
    )

    val IosAssetsLocation = AssetLocation(
        expectedAssets = listOf(kanaVoiceWav)
    )

}
