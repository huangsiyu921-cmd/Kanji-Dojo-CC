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

    override val appName: String = "Kanji Dojo"

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
            append("Create deck by clicking on ")
            appendInlineContent(inlineIconId)
            append(" button. Decks are used to track your progress")
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
        "Following ${mergedTitles.size} decks will be merged into the new \"$newTitle\" deck: ${mergedTitles.joinToString()}"
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
    override val dailyPracticeNew: (Int) -> String = { "New ($it)" }
    override val dailyPracticeDue: (Int) -> String = { "Due ($it)" }
    override val itemGraphProgressTitle: String = "完成度"

    override val selectedPracticeTypeTemplate: (practiceType: String) -> String = {
        "Practice Type: $it"
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
}

object ChineseSponsorStrings : SponsorStrings {
    override val message: String = """
        Development of Kanji Dojo started in 2021 by a single person and it remains free for all users who want to learn Japanese
        
        If you find the app useful please consider supporting this project financially, every contribution counts
        
        Financial support will allow me to focus more on development, bring extra features, add more voiced content and translations
    """.trimIndent()

}

object ChineseDeckPickerStrings : DeckPickerStrings {

    override val title: String = "选择牌组"

    override val customDeckButton: String = "创建空牌组"
    override val kanaTitle: String = "假名"

    override val kanaDescription = { urlColor: Color ->
        buildAnnotatedString {
            append(
                "Japanese kana characters are a set of syllabic characters used in the Japanese writing system. There are two main types of kana: \n" +
                        " • Hiragana - used for native Japanese words and grammatical elements\n" +
                        " • Katakana - often used for loanwords, names, and technical terms\n" +
                        "Kana characters represent sound units, making them an essential part of reading and writing in the Japanese language. "
            )
            withClickableUrl(
                url = "https://en.wikipedia.org/wiki/Kana",
                color = urlColor
            ) {
                append("More info.")
            }
        }
    }
    override val hiragana: String = "平假名"
    override val katakana: String = "片假名"

    override val jltpTitle: String = "JLPT"
    override val jlptDescription: StringResolveScope<AnnotatedString> = {
        buildAnnotatedString {
            append("The Japanese-Language Proficiency Test, or JLPT, is a standardized criterion-referenced test to evaluate and certify Japanese language proficiency for non-native speakers, covering language knowledge, reading ability, and listening ability. ")
            withClickableUrl(
                url = "https://en.wikipedia.org/wiki/Japanese-Language_Proficiency_Test",
                color = MaterialTheme.extraColorScheme.link
            ) {
                append("More info.")
            }
        }
    }
    override val jlptItem: (level: Int) -> String = { "JLPT・N$it" }

    override val gradeTitle: String = "年级"
    override val gradeDescription = { urlColor: Color ->
        buildAnnotatedString {
            withClickableUrl("https://en.wikipedia.org/wiki/J%C5%8Dy%C5%8D_kanji", urlColor) {
                append("The Jōyō kanji")
            }
            append(" is a list of 2,136 frequently used characters maintained officially by the Japanese Ministry of Education. ")
            append("All these characters are taught in Japanese schools:\n")
            append(" • 1,026 kanji taught in primary school (Grade 1-6) (the ")
            withClickableUrl("https://en.wikipedia.org/wiki/Ky%C5%8Diku_kanji", urlColor) {
                append("kyōiku kanji")
            }
            append(")\n")
            append(" • 1,110 additional kanji taught in secondary school (Grade 7-12)")
        }
    }
    override val gradeItemNumbered: (Int) -> String = { "Grade $it" }
    override val gradeItemSecondary: String = "中学"
    override val gradeItemNames: String = "人名用汉字（Jinmeiyō）"
    override val gradeItemNamesVariants: String = "常用汉字的 Jinmeiyō 变体"

    override val wanikaniTitle: String = "WaniKani"
    override val wanikaniDescription = { urlColor: Color ->
        buildAnnotatedString {
            append("Kanji lists according to levels on website WaniKani by Tofugu. ")
            withClickableUrl("https://www.wanikani.com/kanji?difficulty=pleasant", urlColor) {
                append("More info. ")
            }
        }
    }
    override val wanikaniItem: (Int) -> String = { "WaniKani Level $it" }

    override val vocabOtherTitle: String = "其他"
    override val vocabOtherDescription: AnnotatedString = buildAnnotatedString {
        append("A collection of small vocabulary decks covering common topics to help you get started")
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
            append("No cards. To add new cards save this deck and use ")
            appendInlineContent(it)
            append(" icon on search screen, during writing reviews and other places in the app")
        }
    }
    override val completeMessage: String = "完成"
    override val saveTitle: String = "保存更改"
    override val saveInputHint: String = "牌组标题"
    override val saveButtonDefault: String = "保存"
    override val saveButtonCompleted: String = "完成"
    override val deleteTitle: String = "删除确认"
    override val deleteMessage: (deckTitle: String) -> String = {
        "Are you sure you want to delete \"$it\" deck?"
    }
    override val deleteButtonDefault: String = "删除"
    override val deleteButtonCompleted: String = "完成"

    override val unknownTitle: String = "未知文字"
    override val unknownMessage: (characters: List<String>) -> String = {
        "Some letters were not found: ${it.joinToString()}"
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
        "First review time: " + when (it) {
            null -> "Never"
            else -> groupDetailsDateTimeFormatter(it)
        }
    }
    override val lastTimeReviewMessage: (LocalDateTime?) -> String = {
        "Last review time: " + when (it) {
            null -> "Never"
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
    override val configurationTitle: (practiceType: String) -> String = { "Letter Practice・$it" }
    override val hintStrokesTitle: String = "笔画提示"
    override val hintStrokesMessage: String = "控制何时显示文字的笔画提示"
    override val hintStrokeNewOnlyMode: String = "仅新"
    override val hintStrokeAllMode: String = "全部"
    override val hintStrokeNoneMode: String = "从不"
    override val inputModeTitle: String = "输入模式"
    override val inputModeMessage: String = "选择逐笔验证还是整体验证文字"
    override val inputModeStroke: String = "笔画"
    override val inputModeCharacter: String = "文字"
    override val kanaRomajiTitle: String = "在假名练习中显示罗马字"
    override val kanaRomajiMessage: String = "复习假名时显示罗马字而非假名"
    override val noTranslationLayoutTitle: String = "无翻译布局"
    override val noTranslationLayoutMessage: String = "在书写练习中隐藏文字翻译"
    override val leftHandedModeTitle: String = "左手模式"
    override val leftHandedModeMessage: String = "调整书写练习屏幕横屏模式下的输入位置"

    override val headerWordsMessage: (count: Int) -> String = {
        "Examples ($it)"
    }
    override val studyFinishedButton: String = "复习"
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
        "Vocab Practice・$it"
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
            if (it == 1) append(" stroke")
            else append(" strokes")
        }
    }
    override val clipboardCopyMessage: String = "已复制"
    override val radicalsSectionTitle: (count: Int) -> String = { "Radicals ($it)" }
    override val noRadicalsMessage: String = "无部首"
    override val wordsSectionTitle: (count: Int) -> String = { "Expressions ($it)" }
    override val romajiMessage: (romaji: List<String>) -> String = {
        "Romaji readings: ${it.joinToString()}"
    }
    override val gradeMessage: (grade: Int) -> String = {
        when {
            it <= 6 -> "Jōyō kanji, taught in $it grade"
            it == 8 -> "Jōyō kanji, taught in junior high"
            it >= 9 -> "Jinmeiyō kanji, used in names"
            else -> throw IllegalStateException("Unknown grade $it")
        }
    }
    override val jlptMessage: (level: Int) -> String = { "JLPT level $it" }
    override val frequencyMessage: (frequency: Int) -> String = {
        "$it of 2500 most used kanji in newspapers"
    }

}

object ChineseReminderNotificationStrings : ReminderNotificationStrings {
    override val channelName: String = "提醒通知"
    override val title: String = "复习时间到！"
    override val noDetailsMessage: String = "立即继续学习日语"
    override val newOnlyMessage: (Int) -> String = {
        "$it new cards to study today"
    }
    override val dueOnlyMessage: (Int) -> String = {
        "$it due cards to review today"
    }
    override val message: (Int, Int) -> String = { new, due ->
        "$new new cards and $due due cards to review today"
    }
}


object ChineseAccountScreenStrings : AccountScreenStrings {
    override val title = "Account"
    override val loggedOutMessage = "Logged out"
    override val signInButton = "Sign in"
    override val signOutButton = "Sign out"
    override val emailTitle = "E-mail"
    override val subscriptionTitle = "Subscription"
    override val subscriptionStatusActive = "Active"
    override val subscriptionStatusExpired = "Expired"
    override val subscriptionStatusInactive = "Inactive"
    override val subscriptionValidUntilTemplate = "Valid until %s"
    override val issueNoConnectionTitle = "No Connection"
    override val issueNoConnectionMessage = "Showing cached data"
    override val issueSessionExpiredTitle = "Session Expired"
    override val issueSessionExpiredMessage = "Click to sign in again"
    override val issueSubscriptionOutdatedTitle = "Subscription status outdated"
    override val issueSubscriptionOutdatedMessage = "Click to refresh"
    override val issueOtherTitle = "Error"
    override val issueOtherMessageFallback = "Unknown error"
}

object ChineseSyncScreenStrings : SyncScreenStrings {
    override val title = "Sync (Preview)"
    override val guideTitle: String = "在设备间同步进度"
    override val guideMessage =
        "Automatically upload your data to the cloud, keep it as a backup and stay in sync across all your devices"
    override val guideStepAccountTitle = "Create account and sign in"
    override val guideStepAccountMessage: String = "前往账户"
    override val guideStepSubscriptionTitle =
        "Might require a paid subscription in future"
    override val guideStepSubscriptionMessage: String = "预览期间免费，直至另行通知，请关注我们的 Discord 服务器获取更新"
    override val accountErrorMessage: String = "你的账户存在错误"
    override val syncButton = "Sync now"
    override val statusTitle = "Status"
    override val statusMessageLoading = "Checking server for updates..."
    override val statusMessageDataDiffer = "Local and remote data differs"
    override val statusMessageLocalNewer = "Can upload updated data"
    override val statusMessageUpToDate = "Up to date with the server"
    override val statusMessageError = "Error"
    override val statusMessageUploading = "Uploading"
    override val statusMessageDownloading = "Downloading"
    override val statusMessageCanceled = "Canceled, click on sync button to restart"
    override val localDataTitle = "Local Data"
    override val localDataIdTemplate = "ID: %s"
    override val localDataTimestampTemplate = "Timestamp: %s"

    override val errorNoConnectionTitle = "No Connection"
    override val errorNoConnectionMessage = "Couldn't access the server"
    override val errorSessionExpiredTitle = "Session Expired"
    override val errorSessionExpiredMessage = "Click to sign in again"
    override val errorNoSubscriptionTitle = "Subscription status outdated"
    override val errorNoSubscriptionMessage = "Update your subscription status on account screen"
    override val errorOtherTitle = "Error"
    override val errorOtherMessageFallback = "Unknown error"
}

object ChineseSyncDialogStrings : SyncDialogStrings {
    override val title = "Sync"
    override val buttonCancel = "Cancel"
    override val buttonUpload = "Upload"
    override val buttonDownload = "Download"
    override val buttonAccount = "Account"
    override val uploadingMessage = "Uploading..."
    override val downloadingMessage = "Downloading..."
    override val conflictRemoteNewerTitle = "New Data Found"
    override val conflictRemoteNewerMessage = "Data on the server is newer than your local copy"
    override val conflictIncompatibleTitle = "Data Conflict"
    override val conflictIncompatibleMessage =
        "Both remote and local data were changed since the last sync, result can't be merged"
    override val errorNoNetworkTitle = "No Network"
    override val errorNoNetworkMessage = "Couldn't establish network connection"
    override val errorNoSubscriptionTitle = "Subscription Expired"
    override val errorNoSubscriptionMessage =
        "Your subscription has expired, sync will be disabled, refresh your subscription status on the Account screen"
    override val errorNotAuthenticatedTitle = "Session Expired"
    override val errorNotAuthenticatedMessage = "Sign in to your account again to continue"
    override val errorUnexpectedErrorTitle = "Unexpected Error"
    override val errorUnexpectedErrorMessage = "Unknown issue"
    override val errorUnsupportedDataTitle = "Data on the server is unsupported"
    override val errorUnsupportedDataMessage =
        "The data on the server was created using the newer version of the application and is not compatible with the currently installed version. Update the app to retrieve your data or upload your local data to the server"
}

object ChineseSyncSnackbarStrings : SyncSnackbarStrings {
    override val errorNoConnection = "No Connection"
    override val errorNoSubscription = "Subscription expired"
    override val errorNotAuthenticated = "Sign in data expired"
    override val errorDataNotSupported = "Remote data unsupported"
    override val errorMessageTemplate = "Sync Error: %s"
    override val errorMessageNoReason = "Sync Error"
    override val actionButton = "Details"
}