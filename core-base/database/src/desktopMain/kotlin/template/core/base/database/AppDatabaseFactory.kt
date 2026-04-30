package template.core.base.database

import java.io.File

actual class AppDatabaseFactory {
    actual fun databasePath(
        databaseName: String,
        baseDirectory: String?,
    ): String {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val appDataDir = when {
            baseDirectory != null -> File(baseDirectory, "LetaPayDatabase")
            os.contains("win") -> File(System.getenv("APPDATA"), "LetaPayDatabase")
            os.contains("mac") -> File(userHome, "Library/Application Support/LetaPayDatabase")
            else -> File(userHome, ".local/share/LetaPayDatabase")
        }

        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }

        return File(appDataDir, databaseName).absolutePath
    }
}
