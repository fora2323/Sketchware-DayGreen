package extensions.anbui.daydream.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore by preferencesDataStore(
    name = "dr_settings"
)