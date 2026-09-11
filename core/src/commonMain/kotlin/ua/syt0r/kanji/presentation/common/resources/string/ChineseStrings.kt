package ua.syt0r.kanji.presentation.common.resources.string

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ua.syt0r.kanji.presentation.common.CommonDateTimeFormat
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.withClickableUrl
import ua.syt0r.kanji.presentation.screen.main.screen.feedback.FeedbackScreen
import kotlin.time.Duration

object ChineseStrings : Strings {

    override val appName: String = "Kanji dojo CC"

    override val hiragana: String = "平假名"
    override val katakana: String = "片假名"

    override val kunyomi: String = "训读"
    override val onyomi: String = "音读"

    override val loading: String = "加载中"

    override val letterPracticeTypeWriting: String = "书写"
    override val letterPracticeTypeReading: String = "单词卡"
    override val vocabPracticeTypeFlashcard: String = "单词卡"
    override val vocabPracticeTypeReadingPicker: String = "读音选择"
    override val vocabPracticeTypeWriting: String = "书写"

    override val reviewStateDone: String = "已完成"
    override val reviewStateDue: String = "待复习"
    override val reviewStateNew: String = "待学习"

    override val home: HomeStrings = ChineseHomeStrings
    override val commonDashboard = ChineseCommonDashboardStrings
    override val dailyLimit: DailyLimitStrings = ChineseDailyLimitStrings
    override val tutorialDialog: TutorialDialogStrings = ChineseTutorialDialogStrings

    override val stats: StatsStrings = ChineseStatsStrings
    override val search: SearchStrings = ChineseSearchStrings
    override val alternativeDialog: AlternativeDialogStrings = ChineseAlternativeDialogStrings

    override val settings: SettingsStrings = ChineseSettingsStrings
    override val reminderDialog: ReminderDialogStrings = ChineseReminderDialogStrings
    override val about: AboutStrings = ChineseAboutStrings
    override val backup: BackupStrings = ChineseBackupStrings
    override val feedback: FeedbackStrings = ChineseFeedbackStrings
    override val sponsor: SponsorStrings = ChineseSponsorStrings

    override val account: AccountScreenStrings = ChineseAccountScreenStrings
    override val sync: SyncScreenStrings = ChineseSyncScreenStrings
    override val syncDialog: SyncDialogStrings = ChineseSyncDialogStrings
    override val syncSnackbar: SyncSnackbarStrings = ChineseSyncSnackbarStrings

    override val deckPicker: DeckPickerStrings = ChineseDeckPickerStrings
    override val deckEdit: DeckEditStrings = ChineseDeckEditStrings
    override val deckDetails: DeckDetailsStrings = ChineseDeckDetailsStrings
    override val commonPractice: CommonPracticeStrings = ChineseCommonPracticeStrings
    override val letterPractice: LetterPracticeStrings = ChineseLetterPracticeStrings
    override val vocabPractice: VocabPracticeStrings = ChineseVocabPracticeStrings
    override val info: InfoScreenStrings = ChineseInfoScreenStrings

    override val urlPickerMessage: String = "打开方式"
    override val urlPickerErrorMessage: String = "未找到网页浏览器"

    override val reminderNotification: ReminderNotificationStrings =
        ChineseReminderNotificationStrings

}

object ChineseHomeStrings : HomeStrings {
    override val screenTitle: String = "Kanji Dojo"
    override val generalDashboardTabLabel: String = "主页"
    override val lettersDashboardTabLabel: String = "文字"
    override val vocabDashboardTabLabel: String = "词汇"
    override val statsTabLabel: String = "统计"
    override val searchTabLabel: String = "搜索"
    override val settingsTabLabel: String = "设置"
}

object ChineseCommonDashboardStrings : CommonDashboardStrings {

    override val emptyScreenMessage: (inlineIconId: String) -> AnnotatedString = { inlineIconId ->
        buildAnnotatedString {
            append("点击")
            appendInlineContent(inlineIconId)
            append("来创建一个卡组.这些卡组将记录您的学习进度")
        }
    }

    override val mergeButton: String = "合并"
    override val mergeCancelButton: String = "取消"
    override val mergeAcceptButton: String = "合并"
    override val mergeTitle: String = "将多个牌组合并为一个"
    override val mergeTitleHint: String = "在此输入标题"
    override val mergeSelectedCount: (Int) -> String = { "$it selected" }
    override val mergeClearSelectionButton: String = "清除"

    override val mergeDialogTitle: String = "合并确认"
    override val mergeDialogMessage: (String, List<String>) -> String = { newTitle, mergedTitles ->
        " ${mergedTitles.size} 个卡片将会被移到新卡组 \"$newTitle\" 卡组: ${mergedTitles.joinToString()}"
    }
    override val mergeDialogCancelButton: String = "取消"
    override val mergeDialogAcceptButton: String = "合并"

    override val sortButton: String = "排序"
    override val sortCancelButton: String = "取消"
    override val sortAcceptButton: String = "应用"
    override val sortTitle: String = "更改牌组顺序"
    override val sortByTimeTitle: String = "按上次复习时间排序"

    override val itemTimeMessage: (Duration?) -> String = {
        "上次复习: " + when {
            it == null -> "还没学呢"
            it.inWholeDays == 1L -> "一天前"
            it.inWholeDays > 0 -> "${it.inWholeDays} 天前"
            else -> "不到一天"
        }
    }
    override val itemTotal: String = "总计"
    override val itemDone: String = "已完成"
    override val itemReview: String = "待复习"
    override val itemNew: String = "待学习"
    override val dailyPracticeTitle: String = "每日练习"
    override val dailyPracticeNew: (Int) -> String = { "学习 ($it)" }
    override val dailyPracticeDue: (Int) -> String = { "复习 ($it)" }
    override val itemGraphProgressTitle: String = "完成度"

    override val selectedPracticeTypeTemplate: (practiceType: String) -> String = {
        "学习模式: $it"
    }
}

object ChineseDailyLimitStrings : DailyLimitStrings {
    override val enableSwitchTitle: String = "每日限制"
    override val enableSwitchDescription: String = "限制应用提示的每日复习数量"
    override val lettersSectionTitle: String = "文字"
    override val vocabSectionTitle: String = "词汇"
    override val combinedLimitSwitchTitle: String = "合并限制"
    override val combinedLimitSwitchDescription: String = "在所有练习类型之间共享限制"
    override val newLabel: String = "待学习"
    override val dueLabel: String = "待复习"
    override val noteMessage: String = "注意：书写和阅读复习分别计入限制"
    override val button: String = "保存"
    override val changesSavedMessage: String = "已保存"
}

object ChineseTutorialDialogStrings : TutorialDialogStrings {
    override val title: String = "教程"

    override val page1: String = """
        • 该应用采用间隔重复系统（SRS）——一种高效的学习方法，可根据你对信息的记忆程度来优化复习时机。
        • 当你进行复习时，SRS会根据你的回忆能力来安排后续的复习。
        • 如果你能轻松回忆起某个内容，复习间隔就会延长；如果你感到吃力，间隔就会缩短。
    """.trimIndent()

    override val page2Top: String = """
        • 为了评估你的回忆能力，该应用会在每次复习后为你提供多种评分选项。
    """.trimIndent()

    override val page2Bottom: String = """
        • 选择最符合你回忆当前复习内容能力的选项。
        • 对自己的答案进行评分，可让应用根据个人记忆能力调整学习节奏。
    """.trimIndent()

    override val page3Top: String = """
        • 每天，每个已复习项目的状态都会更新。
        • 该应用会让你复习若干新项目以及那些已过预定复习时间、需要复习的到期项目。
    """.trimIndent()

    override val page3Bottom: String = """
        • 你可以设置每日上限，以便将学习负担控制在舒适的水平。
    """.trimIndent()

    override val page4Top: String = """
        • 要开始使用该应用，请创建一个牌组。
        • 牌组用于整理你想要掌握的内容。应用中有字母牌组和词汇牌组。
    """.trimIndent()

    override val page4Bottom: String = """
        • 你可以从零开始创建自己的牌组，也可以从多个现成的牌组中进行选择。
    """.trimIndent()

    override val page5: String = """
        • 创建任意牌组后，你就可以开始进行复习了。
        • 应用提供多种练习模式，不妨都尝试一下。
        • 保持连贯——规律练习是稳步进步的关键。不要犹豫降低每日上限，以免过度疲劳。
        • 祝你在掌握日语的道路上好运！ (^_^)/
    """.trimIndent()
}

private val months = listOf(
    "1", "2", "3", "4", "5", "6", "7", "8",
    "9", "10", "11", "12"
)

private fun formatDuration(duration: Duration): String = when {
    duration.inWholeHours > 0 -> "${duration.inWholeHours}h ${duration.inWholeMinutes % 60}m"
    duration.inWholeMinutes > 0 -> "${duration.inWholeMinutes}m ${duration.inWholeSeconds % 60}s"
    else -> "${duration.inWholeSeconds}s"
}

object ChineseStatsStrings : StatsStrings {
    override val todayTitle: String = "今天"
    override val monthTitle: String = "本月"
    override val monthLabel: (day: LocalDate) -> String = {
        it.run { "${months[monthNumber - 1]}, $year" }
    }
    override val yearTitle: String = "今年"
    override val yearDaysPracticedLabel = { practicedDays: Int, daysInYear: Int ->
        "今年学习了: $practicedDays/$daysInYear 天"
    }
    override val totalTitle: String = "总计"
    override val timeSpentTitle: String = "学习时间"
    override val reviewsCountTitle: String = "复习次数"
    override val formattedDuration: (Duration) -> String = { formatDuration(it) }
    override val uniqueLettersReviewed: String = "已复习的不同文字数量"
    override val uniqueWordsReviewed: String = "已复习的不同词汇数量"
}

object ChineseSearchStrings : SearchStrings {
    override val inputHint: String = "搜索文字或词汇"
    override val charactersTitle: (count: Int) -> String = { "文字 ($it)" }
    override val wordsTitle: (count: Int) -> String = { "字母 ($it)" }
    override val radicalsSheetTitle: String = "按部首搜索"
    override val radicalsFoundCharacters: String = "找到的文字"
    override val radicalsEmptyFoundCharacters: String = "未找到"
    override val radicalSheetRadicalsSectionTitle: String = "部首"
}

object ChineseAlternativeDialogStrings : AlternativeDialogStrings {
    override val title: String = "替代表达"
    override val readingsTitle: String = "读音"
    override val meaningsTitle: String = "含义"
    override val reportButton: String = "报告"
    override val closeButton: String = "关闭"
}

object ChineseSettingsStrings : SettingsStrings {
    override val analyticsTitle: String = "分析"
    override val analyticsMessage: String = "允许发送匿名应用使用数据"
    override val themeTitle: String = "主题"
    override val themeSystem: String = "跟随系统"
    override val themeLight: String = "浅色"
    override val themeDark: String = "深色"
    override val themeAmoled: String = "AMOLED"
    override val reminderTitle: String = "提醒通知"
    override val reminderEnabled: String = "已启用"
    override val reminderDisabled: String = "已禁用"
    override val defaultTab: String = "默认选项卡"
    override val feedbackTitle: String = "反馈"
    override val account: String = "账户"
    override val sync: String = "同步（预览）"
    override val backupTitle: String = "备份与恢复"
    override val aboutTitle: String = "关于"
    override val pickerDialogCancel: String = "取消"
    override val pickerDialogApply: String = "应用"
}

object ChineseReminderDialogStrings : ReminderDialogStrings {
    override val title: String = "提醒通知"
    override val noPermissionLabel: String = "缺少通知权限"
    override val noPermissionButton: String = "授予"
    override val enabledLabel: String = "已启用"
    override val timeLabel: String = "时间"
    override val cancelButton: String = "关闭"
    override val applyButton: String = "应用"
}

object ChineseAboutStrings : AboutStrings {
    override val title: String = "关于"
    override val version: (versionName: String) -> String = { "Version: $it" }
    override val versionChangesTitle: String = "版本变更"
    override val versionChangesDescription: String = "应用变更历史"
    override val versionChangesButton: String = "关闭"
    override val githubTitle: String = "GitHub"
    override val githubDescription: String = "源代码、错误报告、讨论"
    override val creditsTitle: String = "致谢"
    override val creditsDescription: String = "使用的库和数据来源"
    override val githubDialogTitle: String = "前往项目主页"
    override val githubDialogMessage: String = "前往 Kanji Dojo CC 的项目主页"
    override val githubDialogConfirm: String = "前往"
    override val githubDialogCancel: String = "取消"
}

object ChineseBackupStrings : BackupStrings {
    override val title: String = "备份与恢复"
    override val backupButton: String = "创建备份"
    override val restoreButton: String = "从备份恢复"
    override val unknownError: String = "未知错误"
    override val restoreVersionMessage: (Long, Long) -> String = { backupVersion, currentVersion ->
        "Database version: $backupVersion (Current: $currentVersion)"
    }
    override val restoreTimeMessage: (LocalDateTime) -> String = {
        "Create time: ${it.format(CommonDateTimeFormat)}"
    }
    override val restoreNote: String = "注意！所有当前进度将被所选备份中的进度替换"
    override val restoreApplyButton: String = "恢复"
    override val completeMessage: String = "完成"
}

object ChineseFeedbackStrings : FeedbackStrings {
    override val title: String = "反馈"
    override val topicTitle: String = "主题"
    override val topicGeneral: String = "常规"
    override val topicExpression: (id: Long, screen: FeedbackScreen) -> String = { id, screen ->
        val screenName: String = when (screen) {
            FeedbackScreen.WritingPractice -> "Writing practice"
            FeedbackScreen.ReadingPractice -> "Reading practice"
            FeedbackScreen.Search -> "Search"
            FeedbackScreen.CharacterInfo -> "Letter info"
            FeedbackScreen.VocabPractice -> "Vocab practice"
        }
        "$screenName, expression $id"
    }
    override val messageLabel: String = "在此输入反馈"
    override val button: String = "发送"
    override val successMessage: String = "反馈已发送"
    override val errorMessage: (String?) -> String = { "Error: $it" }
    override val githubMessage: String = "如有建议或 bug，可前往 GitHub 的项目主页进行反馈"
    override val githubButton: String = "前往 GitHub"
}

object ChineseSponsorStrings : SponsorStrings {
    override val message: String = """
        Kanji Dojo 的开发始于 2021 年，由一个人，并且它对于所有想要学习日语的用户保持免费。

        如果您觉得这个应用有用，请考虑在经济上支持这个项目，每一份贡献都很重要。

        经济支持将使我能够更专注于开发，带来额外功能，添加更多语音内容和翻译。
    """.trimIndent()

    override val buyCoffee: String = "给原作者买杯咖啡"
}

object ChineseDeckPickerStrings : DeckPickerStrings {

    override val title: String = "选择牌组"

    override val customDeckButton: String = "创建空牌组"
    override val kanaTitle: String = "假名"

    override val kanaDescription = { urlColor: Color ->
        buildAnnotatedString {
            append(
                "日语假名是日语书写系统中使用的一套音节文字。假名主要有两种类型： \n" +
                        " • 平假名——用于书写日语固有词汇和语法成分\n" +
                        " • 片假名——常用于书写外来语、人名和技术术语等\n" +
                        "假名字符表示音素，因此它们是日语读写中不可或缺的一部分. "
            )
            withClickableUrl(
                url = "https://en.wikipedia.org/wiki/Kana",
                color = urlColor
            ) {
                append("点我了解更多.")
            }
        }
    }
    override val hiragana: String = "平假名"
    override val katakana: String = "片假名"

    override val jltpTitle: String = "JLPT"
    override val jlptDescription: StringResolveScope<AnnotatedString> = {
        buildAnnotatedString {
            append("日语能力测试（JLPT）是一项面向非母语者的标准化、标准参照考试，旨在评估和认证日语熟练程度，涵盖语言知识、阅读能力和听力能力. ")
            withClickableUrl(
                url = "https://en.wikipedia.org/wiki/Japanese-Language_Proficiency_Test",
                color = MaterialTheme.extraColorScheme.link
            ) {
                append("点我了解更多.")
            }
        }
    }
    override val jlptItem: (level: Int) -> String = { "JLPT・N$it" }

    override val gradeTitle: String = "年级"
    override val gradeDescription = { urlColor: Color ->
        buildAnnotatedString {
            withClickableUrl("https://en.wikipedia.org/wiki/J%C5%8Dy%C5%8D_kanji", urlColor) {
                append("常用汉字")
            }
            append(" 常用汉字是日本教育部正式维护的 2136 个常用汉字列表。 ")
            append("所有这些汉字都在日本学校中教授:\n")
            append(" •  小学（1-6 年级）教授 1026 个汉字 (the ")
            withClickableUrl("https://en.wikipedia.org/wiki/Ky%C5%8Diku_kanji", urlColor) {
                append("教育汉字")
            }
            append(")\n")
            append(" •  中学（7-12 年级）追加教授 1110 个汉字")
        }
    }
    override val gradeItemNumbered: (Int) -> String = { "Grade $it" }
    override val gradeItemSecondary: String = "中学"
    override val gradeItemNames: String = "人名用汉字（Jinmeiyō）"
    override val gradeItemNamesVariants: String = "常用汉字的 Jinmeiyō 变体"

    override val wanikaniTitle: String = "WaniKani"
    override val wanikaniDescription = { urlColor: Color ->
        buildAnnotatedString {
            append("这是由 Tofugu 运营的 WaniKani 网站中按级别划分的汉字列表. ")
            withClickableUrl("https://www.wanikani.com/kanji?difficulty=pleasant", urlColor) {
                append("点我了解更多. ")
            }
        }
    }
    override val wanikaniItem: (Int) -> String = { "WaniKani 第 $it 级" }

    override val vocabOtherTitle: String = "其他"
    override val vocabOtherDescription: AnnotatedString = buildAnnotatedString {
        append("涵盖常见主题的小型词汇集，助您轻松入门")
    }

    override val vocabDeckItemWordsCountLabel: (words: Int) -> String = { "$it words" }

    override val vocabDeckTitleTime: String = "时间"
    override val vocabDeckTitleWeek: String = "星期"
    override val vocabDeckTitleCommonVerbs: String = "常用动词"
    override val vocabDeckTitleColors: String = "颜色"
    override val vocabDeckTitleRegularFood: String = "普通食物"
    override val vocabDeckTitleJapaneseFood: String = "日式食物"
    override val vocabDeckTitleGrammarTerms: String = "语法术语"
    override val vocabDeckTitleAnimals: String = "动物"
    override val vocabDeckTitleBody: String = "身体"
    override val vocabDeckTitleCommonPlaces: String = "常见场所"
    override val vocabDeckTitleCities: String = "城市"
    override val vocabDeckTitleTransport: String = "交通"

}

object ChineseDeckEditStrings : DeckEditStrings {
    override val createTitle: String = "创建牌组"
    override val ediTitle: String = "编辑牌组"
    override val searchHint: String = "输入假名或汉字"
    override val editingModeSearchTitle: String = "搜索"
    override val editingModeRemovalTitle: String = "移除"
    override val editingModeDetailsTitle: String = "详情"
    override val vocabDetailsEmptyMessage: (inlineIconId: String) -> AnnotatedString = {
        buildAnnotatedString {
            append("暂无卡片。要添加新卡片，请先保存此词库，然后在搜索界面、写复习答案时以及应用内的其他位置使用 ")
            appendInlineContent(it)
            append(" 图标")
        }
    }
    override val completeMessage: String = "完成"
    override val saveTitle: String = "保存更改"
    override val saveInputHint: String = "牌组标题"
    override val saveButtonDefault: String = "保存"
    override val saveButtonCompleted: String = "完成"
    override val deleteTitle: String = "删除确认"
    override val deleteMessage: (deckTitle: String) -> String = {
        "确定删除 \"$it\" 词库吗？?"
    }
    override val deleteButtonDefault: String = "删除"
    override val deleteButtonCompleted: String = "完成"

    override val unknownTitle: String = "未知文字"
    override val unknownMessage: (characters: List<String>) -> String = {
        "有些字母没被找到: ${it.joinToString()}"
    }
    override val unknownButton: String = "关闭"

    override val leaveConfirmationTitle: String = "离开确认"
    override val leaveConfirmationMessage: String = "所有更改将丢失"
    override val leaveConfirmationCancel: String = "取消"
    override val leaveConfirmationAccept: String = "离开"
}

object ChineseDeckDetailsStrings : DeckDetailsStrings {
    override val emptyListMessage: String = "这里没有内容"
    override val detailsGroupTitle: (index: Int) -> String = { "Group $it" }
    override val firstTimeReviewMessage: (LocalDateTime?) -> String = {
        "第一次学习时间: " + when (it) {
            null -> "从没看过"
            else -> groupDetailsDateTimeFormatter(it)
        }
    }
    override val lastTimeReviewMessage: (LocalDateTime?) -> String = {
        "上次复习时间: " + when (it) {
            null -> "从没看过"
            else -> groupDetailsDateTimeFormatter(it)
        }
    }
    override val groupDetailsButton: String = "开始"

    override val expectedReviewDate: (LocalDate?) -> String =
        { "Expected Review: ${it ?: "-"}" }
    override val lastReviewDate: (LocalDateTime?) -> String = {
        "Last Review: ${it?.date ?: "-"}"
    }
    override val repetitions: (Int) -> String = { "Repetitions: $it" }
    override val lapses: (Int) -> String = { "Lapses: $it" }

    override val dialogCommon: LetterDeckDetailDialogCommonStrings =
        ChineseLetterDeckDetailDialogCommonStrings
    override val filterDialog: FilterDialogStrings = ChineseFilterDialogStrings
    override val sortDialog: SortDialogStrings = ChineseSortDialogStrings
    override val layoutDialog: PracticePreviewLayoutDialogStrings =
        ChinesePracticePreviewLayoutDialogStrings

    override val multiselectTitle: (selectedCount: Int) -> String = { "$it Selected" }
    override val multiselectDataNotLoaded: String = "加载中，请稍候……"
    override val multiselectNoSelected: String = "请至少选择一个组"

    override val filterAllLabel: String = "全部"
    override val filterNoneLabel: String = "无"
    override val kanaGroupsModeActivatedLabel: String = "假名分组模式"
    override val shareLetterDeckClipboardMessage: String = "牌组中的文字已复制到剪贴板"

}

object ChineseLetterDeckDetailDialogCommonStrings : LetterDeckDetailDialogCommonStrings {
    override val buttonCancel: String = "取消"
    override val buttonApply: String = "应用"
}

object ChineseFilterDialogStrings : FilterDialogStrings {
    override val title: String = "筛选"
}

object ChineseSortDialogStrings : SortDialogStrings {
    override val title: String = "排序"
    override val sortOptionAddOrder: String = "添加顺序"
    override val sortOptionAddOrderHint: String = "↑ 新项目在后\\n↓ 新项目在前"
    override val sortOptionFrequency: String = "频率"
    override val sortOptionFrequencyHint: String = "字符在报纸中出现的频率\\n↑ 高频在前\\n↓ 低频在前"
    override val sortOptionName: String = "名称"
    override val sortOptionNameHint: String = "↑ 从小到大\\n↓ 从大到小"
    override val sortOptionReviewTime: String = "预期复习时间"
    override val sortOptionReviewTimeHint: String = "↑ 未复习的在前\\n↓ 最晚安排的在前"
}

object ChinesePracticePreviewLayoutDialogStrings : PracticePreviewLayoutDialogStrings {
    override val title: String = "布局"
    override val singleCharacterOptionLabel: String = "单个文字"
    override val groupsOptionLabel: String = "分组"
    override val kanaGroupsTitle: String = "假名分组"
    override val kanaGroupsSubtitle: String = "如果练习包含所有假名字符，则按假名表划分组大小"
}

object ChineseCommonPracticeStrings : CommonPracticeStrings {
    override val configurationTitle: String = "配置"
    override val configurationSelectedItemsLabel: String = "已选："
    override val configurationCharactersPreview: String = "文字预览"
    override val shuffleConfigurationTitle: String = "乱序"
    override val shuffleConfigurationMessage: String = "打乱复习顺序"
    override val configurationCompleteButton: String = "开始"

    override val additionalKanaReadingsNote: (List<String>) -> String = {
        "Note: can also be written as ${it.joinToString()}"
    }

    override val formattedSrsInterval: (Duration) -> String = { formattedSrsDuration(it) }
    override val flashcardRevealButton: String = "显示答案"
    override val againButton: String = "重新"
    override val hardButton: String = "困难"
    override val goodButton: String = "良好"
    override val easyButton: String = "简单"

    override val summaryTimeSpentValue: (Duration) -> String = { formatDuration(it) }

    override val earlyFinishDialogTitle: String = "结束练习？"
    override val earlyFinishDialogMessage: String = "前往总结页面，当前进度已保存"
    override val earlyFinishDialogCancelButton: String = "取消"
    override val earlyFinishDialogAcceptButton: String = "结束"
}

object ChineseLetterPracticeStrings : LetterPracticeStrings {
    override val configurationTitle: (practiceType: String) -> String = { "字母练习・$it" }
    override val hintStrokesTitle: String = "笔画提示"
    override val hintStrokesMessage: String = "控制何时显示文字的笔画提示"
    override val hintStrokeNewOnlyMode: String = "仅新"
    override val hintStrokeAllMode: String = "全部"
    override val hintStrokeNoneMode: String = "从不"
    override val inputModeTitle: String = "输入模式"
    override val inputModeMessage: String = "选择逐笔验证还是整体验证文字"
    override val inputModeStroke: String = "逐笔验证"
    override val inputModeCharacter: String = "整体验证"
    override val kanaRomajiTitle: String = "在假名练习中显示罗马字"
    override val kanaRomajiMessage: String = "复习假名时显示罗马字而非假名"
    override val noTranslationLayoutTitle: String = "无翻译布局"
    override val noTranslationLayoutMessage: String = "在书写练习中隐藏文字翻译"
    override val leftHandedModeTitle: String = "左手模式"
    override val leftHandedModeMessage: String = "调整书写练习屏幕横屏模式下的输入位置"

    override val headerWordsMessage: (count: Int) -> String = {
        "Examples ($it)"
    }
    override val studyFinishedButton: String = "继续"
    override val noKanjiTranslationsLabel: String = "[无翻译]"

    override val altStrokeEvaluatorTitle: String = "严格笔画评估器"
    override val altStrokeEvaluatorMessage: String = "用于笔画评估的替代算法"

    override val variantsTitle: String = "变体："
    override val variantsHint: String = "点击显示"
    override val unicodeTitle: (String) -> String = { "Unicode: $it" }
    override val strokeCountTitle: (count: Int) -> String = { "Stroke count: $it" }
}

object ChineseVocabPracticeStrings : VocabPracticeStrings {
    override val configurationTitle: (practiceType: String) -> String = {
        "词组练习・$it"
    }
    override val readingMeaningConfigurationTitle: String = "始终显示含义"
    override val readingMeaningConfigurationMessage: String = "选择未选择答案时的含义可见性"
    override val translationInFrontConfigurationTitle: String = "翻译在前"
    override val translationInFrontConfigurationMessage: String = "闪卡隐藏时显示翻译而非单词"
    override val writingKanaReadingConfigurationTitle: String = "显示假名读音"
    override val writingKanaReadingConfigurationMessage: String = "即使在输入答案之前也显示假名读音"
    override val detailsButton: String = "详情"
}

object ChineseInfoScreenStrings : InfoScreenStrings {
    override val strokesMessage: (count: Int) -> AnnotatedString = {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.toString()) }
            if (it == 1) append(" 画")
            else append(" 画")
        }
    }
    override val clipboardCopyMessage: String = "已复制"
    override val radicalsSectionTitle: (count: Int) -> String = { "部首 ($it)" }
    override val noRadicalsMessage: String = "无部首"
    override val wordsSectionTitle: (count: Int) -> String = { "表达 ($it)" }
    override val romajiMessage: (romaji: List<String>) -> String = {
        "罗马音读法: ${it.joinToString()}"
    }
    override val gradeMessage: (grade: Int) -> String = {
        when {
            it <= 6 -> "常用汉字, 在 $it 年级教"
            it == 8 -> "常用汉字, 在初中教"
            it >= 9 -> "人名用 汉字，用于姓名"
            else -> throw IllegalStateException("未知年级 $it")
        }
    }
    override val jlptMessage: (level: Int) -> String = { "JLPT 等级 $it" }
    override val frequencyMessage: (frequency: Int) -> String = {
        "$it 报纸最常用的1500个单词"
    }

}

object ChineseReminderNotificationStrings : ReminderNotificationStrings {
    override val channelName: String = "提醒通知"
    override val title: String = "复习时间到！"
    override val noDetailsMessage: String = "立即继续学习日语"
    override val newOnlyMessage: (Int) -> String = {
        "$it 张卡片今天要学习"
    }
    override val dueOnlyMessage: (Int) -> String = {
        "$it 张卡片今天要复习"
    }
    override val message: (Int, Int) -> String = { new, due ->
        "$new 张新卡片和 $due 张卡片今天要学习"
    }
}


object ChineseAccountScreenStrings : AccountScreenStrings {
    override val title = "账户"
    override val loggedOutMessage = "未登陆"
    override val signInButton = "登陆"
    override val signOutButton = "登出"
    override val emailTitle = "电子邮件"
    override val subscriptionTitle = "订阅"
    override val subscriptionStatusActive = "有效"
    override val subscriptionStatusExpired = "已过期"
    override val subscriptionStatusInactive = "未激活"
    override val subscriptionValidUntilTemplate = "有效期至 %s"
    override val issueNoConnectionTitle = "无网络连接"
    override val issueNoConnectionMessage = "显示缓存数据"
    override val issueSessionExpiredTitle = "会话已过期"
    override val issueSessionExpiredMessage = "点击重新登陆"
    override val issueSubscriptionOutdatedTitle = "订阅状态过期"
    override val issueSubscriptionOutdatedMessage = "点击刷新"
    override val issueOtherTitle = "错误"
    override val issueOtherMessageFallback = "未知错误"
}

object ChineseSyncScreenStrings : SyncScreenStrings {
    override val title = "同步（预览）"
    override val guideTitle: String = "在设备间同步进度"
    override val guideMessage =
        "自动上传你的数据到云端，将其保留为备份，并在所有设备间保持同步"
    override val guideStepAccountTitle = "创建账户并登录"
    override val guideStepAccountMessage: String = "前往账户"
    override val guideStepSubscriptionTitle =
        "将来可能需要付费订阅"
    override val guideStepSubscriptionMessage: String = "预览期间免费，直至另行通知，请关注我们的 Discord 服务器获取更新"
    override val accountErrorMessage: String = "你的账户存在错误"
    override val syncButton = "立即同步"
    override val statusTitle = "状态"
    override val statusMessageLoading = "正在检查服务器更新..."
    override val statusMessageDataDiffer = "本地与远端数据不一致"
    override val statusMessageLocalNewer = "可上传更新数据"
    override val statusMessageUpToDate = "与服务器同步中"
    override val statusMessageError = "错误"
    override val statusMessageUploading = "上传中"
    override val statusMessageDownloading = "下载中"
    override val statusMessageCanceled = "已取消，点击同步按钮重新开始"
    override val localDataTitle = "本地数据"
    override val localDataIdTemplate = "ID: %s"
    override val localDataTimestampTemplate = "时间戳：%s"

    override val errorNoConnectionTitle = "无连接"
    override val errorNoConnectionMessage = "无法访问服务器"
    override val errorSessionExpiredTitle = "会话已过期"
    override val errorSessionExpiredMessage = "点击重新登录"
    override val errorNoSubscriptionTitle = "订阅状态已过期"
    override val errorNoSubscriptionMessage = "请在账户页面更新你的订阅状态"
    override val errorOtherTitle = "错误"
    override val errorOtherMessageFallback = "未知错误"
}

object ChineseSyncDialogStrings : SyncDialogStrings {
    override val title = "同步"
    override val buttonCancel = "取消"
    override val buttonUpload = "上传"
    override val buttonDownload = "下载"
    override val buttonAccount = "账户"
    override val uploadingMessage = "上传中..."
    override val downloadingMessage = "下载中..."
    override val conflictRemoteNewerTitle = "发现新数据"
    override val conflictRemoteNewerMessage = "服务器上的数据比本地副本更新"
    override val conflictIncompatibleTitle = "数据冲突"
    override val conflictIncompatibleMessage = "自上次同步以来远端和本地数据都被修改，无法合并结果"
    override val errorNoNetworkTitle = "无网络"
    override val errorNoNetworkMessage = "无法建立网络连接"
    override val errorNoSubscriptionTitle = "订阅已过期"
    override val errorNoSubscriptionMessage = "你的订阅已过期，同步将被禁用，请在账户页面刷新订阅状态"
    override val errorNotAuthenticatedTitle = "会话已过期"
    override val errorNotAuthenticatedMessage = "请重新登录账户以继续"
    override val errorUnexpectedErrorTitle = "意外错误"
    override val errorUnexpectedErrorMessage = "未知问题"
    override val errorUnsupportedDataTitle = "服务器上的数据不受支持"
    override val errorUnsupportedDataMessage = "服务器上的数据由更新版本的应用创建，与当前安装版本不兼容。请更新应用以取回你的数据，或将本地数据上传到服务器"
}

object ChineseSyncSnackbarStrings : SyncSnackbarStrings {
    override val errorNoConnection = "无连接"
    override val errorNoSubscription = "订阅已过期"
    override val errorNotAuthenticated = "登录数据已过期"
    override val errorDataNotSupported = "远端数据不受支持"
    override val errorMessageTemplate = "同步错误：%s"
    override val errorMessageNoReason = "同步错误"
    override val actionButton = "详情"
}