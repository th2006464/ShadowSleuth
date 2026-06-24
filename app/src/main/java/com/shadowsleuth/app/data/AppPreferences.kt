package com.shadowsleuth.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * 应用偏好设置（除主题外的其他开关）
 */
class AppPreferences(private val context: Context) {

    companion object {
        private val DELETE_PERMISSION_ENABLED = booleanPreferencesKey("delete_permission_enabled")
    }

    /** 删除权限开关（默认 false，必须用户手动开启） */
    val deletePermissionEnabled: Flow<Boolean> = context.appDataStore.data
        .map { preferences -> preferences[DELETE_PERMISSION_ENABLED] ?: false }

    suspend fun setDeletePermissionEnabled(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[DELETE_PERMISSION_ENABLED] = enabled
        }
    }
}
