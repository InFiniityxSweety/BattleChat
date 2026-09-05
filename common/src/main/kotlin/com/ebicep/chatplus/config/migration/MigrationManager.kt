package com.ebicep.chatplus.config.migration

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.CONFIG_NAME
import com.ebicep.chatplus.config.CONFIG_VERSION
import com.ebicep.chatplus.config.Config.values
import com.ebicep.chatplus.config.ConfigVariables
import com.ebicep.chatplus.config.json
import kotlinx.serialization.KSerializer
import java.io.File

object MigrationManager {

    private const val LEGACY_CONFIG_DIR = "chatplus"

    private val migrators: List<Migrator<*>> = listOf(
        V2_5,
        V2_1,
        V2,
        V1
    )

    /**
     * BattleChat migration path.
     *
     * First startup looks for the most recent ChatPlus config in the old
     * config/chatplus directory and imports it into BattleChat's own config
     * directory. The old file is never modified or deleted.
     */
    fun tryLegacyChatPlusMigration(configRoot: File, currentConfig: File): Boolean {
        val legacyDirectory = File(configRoot, LEGACY_CONFIG_DIR)
        if (!legacyDirectory.exists()) {
            return false
        }

        val currentLegacyConfig = File(legacyDirectory, "chatplus-v$CONFIG_VERSION.json")
        if (currentLegacyConfig.exists() && migrateCurrentConfig(currentLegacyConfig, currentConfig)) {
            return true
        }

        return tryMigration(legacyDirectory, currentConfig)
    }

    private fun migrateCurrentConfig(oldConfig: File, currentConfig: File): Boolean {
        return try {
            ChatPlus.LOGGER.info("Importing ChatPlus config ${oldConfig.name} into BattleChat as $CONFIG_NAME")
            values = json.decodeFromString(ConfigVariables.serializer(), oldConfig.readText())
            currentConfig.parentFile?.mkdirs()
            currentConfig.writeText(json.encodeToString(ConfigVariables.serializer(), values))
            true
        } catch (exception: Exception) {
            ChatPlus.LOGGER.warn("Failed to import current ChatPlus config ${oldConfig.absolutePath}", exception)
            false
        }
    }

    fun tryMigration(configDirectory: File, currentConfig: File): Boolean {
        ChatPlus.LOGGER.info("Checking for config migration in ${configDirectory.absolutePath}")
        migrators.forEach {
            val oldConfigFileName = "${it.getFileNameVersion()}.json"
            val oldConfig = File(configDirectory, oldConfigFileName)
            if (oldConfig.exists()) {
                ChatPlus.LOGGER.info("Migrating config from $oldConfigFileName to $CONFIG_NAME")
                return try {
                    // update current values with old values
                    values = json.decodeFromString(ConfigVariables.serializer(), oldConfig.readText())
                    // update the config with migrated values
                    val oldConfigValues: Any? = json.decodeFromString(it.getSerializer(), oldConfig.readText())
                    (it as Migrator<Any?>).migrate(oldConfigValues)
                    // write new values
                    currentConfig.parentFile?.mkdirs()
                    currentConfig.writeText(json.encodeToString(ConfigVariables.serializer(), values))
                    true
                } catch (exception: Exception) {
                    ChatPlus.LOGGER.warn("Failed to migrate config $oldConfigFileName", exception)
                    false
                }
            }
        }
        return false
    }

    fun copyFile(file: File, newFile: File) {
        file.copyTo(newFile)
    }

}

interface Migrator<T> {

    fun getFileNameVersion(): String

    fun getSerializer(): KSerializer<T>

    fun migrate(old: T)

}
