import org.gradle.api.Plugin
import org.gradle.api.Project

class KMPSQLDelightConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.cash.sqldelight")
        }
    }
}
