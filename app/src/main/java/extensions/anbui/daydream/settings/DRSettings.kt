package extensions.anbui.daydream.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DRSettings {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun interface BooleanCallback {
        fun onResult(value: Boolean)
    }

    object PrefKeys {
        val IS_FIRST_SETUP = booleanPreferencesKey("drIsFirstSetup")
        val IS_USE_BACKUP_TOOL = booleanPreferencesKey("drUseBackupTool")
        val IS_AUTO_CLEAN_UP_AFTER_BUILD = booleanPreferencesKey("isAutoCleanUpAfterBuild")
    }

    @JvmStatic
    fun getIsFirstSetup(context: Context, callback: BooleanCallback) {
        scope.launch {
            val value = context.appDataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[PrefKeys.IS_FIRST_SETUP] ?: true

            withContext(Dispatchers.Main) {
                callback.onResult(value)
            }
        }
    }

    @JvmStatic
    fun setIsFirstSetup(context: Context, value: Boolean) {
        scope.launch {
            context.appDataStore.edit {
                it[PrefKeys.IS_FIRST_SETUP] = value
            }
        }
    }

    @JvmStatic
    fun getUseBackupTool(context: Context, callback: BooleanCallback) {
        scope.launch {
            val value = context.appDataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[PrefKeys.IS_USE_BACKUP_TOOL] ?: true

            withContext(Dispatchers.Main) {
                callback.onResult(value)
            }
        }
    }

    @JvmStatic
    fun setUseBackupTool(context: Context, value: Boolean) {
        scope.launch {
            context.appDataStore.edit {
                it[PrefKeys.IS_USE_BACKUP_TOOL] = value
            }
        }
    }

    @JvmStatic
    fun getAutoCleanUpAfterBuild(context: Context, callback: BooleanCallback) {
        scope.launch {
            val value = context.appDataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[PrefKeys.IS_AUTO_CLEAN_UP_AFTER_BUILD] ?: true

            withContext(Dispatchers.Main) {
                callback.onResult(value)
            }
        }
    }

    @JvmStatic
    fun setAutoCleanUpAfterBuild(context: Context, value: Boolean) {
        scope.launch {
            context.appDataStore.edit {
                it[PrefKeys.IS_AUTO_CLEAN_UP_AFTER_BUILD] = value
            }
        }
    }
}