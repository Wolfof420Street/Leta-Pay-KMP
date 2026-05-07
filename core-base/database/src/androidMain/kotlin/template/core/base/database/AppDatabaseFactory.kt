package template.core.base.database

actual class AppDatabaseFactory {
    actual fun databasePath(
        databaseName: String,
        baseDirectory: String?,
    ): String {
        val rootDirectory = baseDirectory ?: System.getProperty("user.home")
        val databaseDirectory = java.io.File(rootDirectory, "LetaPayDatabase")

        if (!databaseDirectory.exists()) {
            databaseDirectory.mkdirs()
        }

        return java.io.File(databaseDirectory, databaseName).absolutePath
    }
}
