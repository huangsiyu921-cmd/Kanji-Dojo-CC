package ua.syt0r.kanji.di

import android.app.ActivityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import ua.syt0r.kanji.AndroidMainBuildConfig
import ua.syt0r.kanji.core.AndroidThemeManager
import ua.syt0r.kanji.core.BuildConfig
import ua.syt0r.kanji.core.app_data.AppDataDatabaseProvider
import ua.syt0r.kanji.core.app_data.AndroidAppDataDatabaseProvider
import ua.syt0r.kanji.core.backup.AndroidBackupArchiveHandler
import ua.syt0r.kanji.core.backup.BackupArchiveHandler
import ua.syt0r.kanji.core.file.AndroidPlatformFileHandler
import ua.syt0r.kanji.core.file.PlatformFileHandler
import ua.syt0r.kanji.core.logger.LoggerConfiguration
import ua.syt0r.kanji.core.notification.ReminderNotificationContract
import ua.syt0r.kanji.core.notification.ReminderNotificationHandleScheduledActionUseCase
import ua.syt0r.kanji.core.notification.ReminderNotificationManager
import ua.syt0r.kanji.core.notification.ReminderNotificationScheduler
import ua.syt0r.kanji.core.sync.AndroidSyncBackupFileProvider
import ua.syt0r.kanji.core.sync.SyncBackupFileProvider
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.tts.AndroidKanaTtsManager
import ua.syt0r.kanji.core.tts.AndroidWordTtsManager
import ua.syt0r.kanji.core.tts.FileWordTtsCache
import ua.syt0r.kanji.core.tts.KanaTtsManager
import ua.syt0r.kanji.core.tts.VoicevoxAndroidTtsManager
import ua.syt0r.kanji.core.tts.WordTtsCache
import ua.syt0r.kanji.core.tts.WordTtsManager
import ua.syt0r.kanji.core.tts.Neural2BKanaVoiceData
import ua.syt0r.kanji.core.user_data.AndroidUserDataDatabasePlatformHandler
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.user_data.preferences.DefaultUserPreferencesMigrationManager
import ua.syt0r.kanji.presentation.backupScreenComponents
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.AndroidReminderSettingListItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.settingItemsQualifier

actual val platformComponentsModule: Module = module {

    factory { LoggerConfiguration(isEnabled = BuildConfig.DEBUG) }

    factory { ExoPlayer.Builder(androidContext()).build() }

    factory<SyncBackupFileProvider> {
        AndroidSyncBackupFileProvider(
            context = androidContext()
        )
    }

    factory<KanaTtsManager> {
        AndroidKanaTtsManager(
            player = get(),
            voiceData = Neural2BKanaVoiceData(
                assetPath = "files/${AndroidMainBuildConfig.kanaVoiceAssetName}"
            )
        )
    }

    single<WordTtsCache> {
        // Private app storage: the system's cacheDir can be wiped under pressure, and these files
        // are also managed from the settings screen.
        FileWordTtsCache(androidContext().filesDir.resolve("tts-cache"))
    }

    single<WordTtsManager> {
        // Bundled VOICEVOX engine first; the system voice stays as the fallback, mirroring the
        // desktop module (see TTS-HANDOFF.md).
        VoicevoxAndroidTtsManager(
            context = androidContext(),
            fallback = AndroidWordTtsManager(context = androidContext()),
            cache = get()
        )
    }

    single<AppDataDatabaseProvider> {
        AndroidAppDataDatabaseProvider(
            context = androidContext()
        )
    }

    single<UserDataDatabaseContract.PlatformHandler> {
        AndroidUserDataDatabasePlatformHandler(
            context = androidContext(),
            migrationProvider = get()
        )
    }

    factory<PlatformFileHandler> {
        AndroidPlatformFileHandler(
            contentResolver = androidContext().contentResolver
        )
    }

    factory<BackupArchiveHandler> {
        AndroidBackupArchiveHandler(
            contentResolver = androidContext().contentResolver
        )
    }

    single<DataStore<*>> {
        PreferenceDataStoreFactory.create(
            migrations = DefaultUserPreferencesMigrationManager.DefaultMigrations,
            produceFile = { androidContext().preferencesDataStoreFile("preferences") }
        )
    }

    single<ThemeManager> {
        AndroidThemeManager(appPreferences = get())
    }

    factory<WorkManager> { WorkManager.getInstance(androidContext()) }
    factory<NotificationManagerCompat> { NotificationManagerCompat.from(androidContext()) }
    factory<ActivityManager> { androidContext().getSystemService<ActivityManager>()!! }

    factory<ReminderNotificationContract.Scheduler> {
        ReminderNotificationScheduler(
            workManger = get(),
            timeUtils = get()
        )
    }

    factory<ReminderNotificationContract.Manager> {
        ReminderNotificationManager(
            context = androidContext(),
            notificationManager = get()
        )
    }

    factory<ReminderNotificationContract.HandleScheduledActionUseCase> {
        ReminderNotificationHandleScheduledActionUseCase(
            activityManager = get(),
            letterSrsManager = get(),
            vocabSrsManager = get(),
            notificationManager = get(),
            appPreferences = get(),
            scheduler = get(),
            analyticsManager = get()
        )
    }

    factory {
        AndroidReminderSettingListItem(
            appPreferences = get(),
            reminderScheduler = get(),
            analyticsManager = get()
        )
    }

    factory(settingItemsQualifier) {
        listOf(
            get<AndroidReminderSettingListItem>()
        )
    }

    backupScreenComponents()

}