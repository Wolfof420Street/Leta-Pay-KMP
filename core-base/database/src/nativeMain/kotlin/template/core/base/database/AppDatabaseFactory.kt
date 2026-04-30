package template.core.base.database

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class AppDatabaseFactory {
    actual fun databasePath(
        databaseName: String,
        baseDirectory: String?,
    ): String {
        val databaseDirectory = baseDirectory?.takeIf { it.isNotBlank() } ?: documentDirectory()
        return "$databaseDirectory/$databaseName"
    }

    @OptIn(ExperimentalForeignApi::class)
    fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }
}
