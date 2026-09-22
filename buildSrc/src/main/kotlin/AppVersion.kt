object AppVersion {

    const val versionCode = 3

    const val versionName = "0.0.3"

    // Requires 3 numbers, and MAJOR must be > 0 for the desktop DMG/MSI packaging
    // （jpackage 不允许 0.x，所以桌面安装包版本只能排 1.x；
    //  应用内「关于」页显示的版本仍取自上面的 versionName，即 0.0.3）
    const val desktopAppVersion = "1.0.3"

}
