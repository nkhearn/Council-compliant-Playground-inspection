package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.ChecklistCatalog
import com.example.model.ChecklistQuestionTemplate
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "playsafe_settings",
)

data class InspectorSettings(
    val enabledCategoryIds: Set<String> = ChecklistCatalog.allCategoryIds,
    val disabledQuestionIds: Set<String> = emptySet(),
    val customQuestions: List<ChecklistQuestionTemplate> = emptyList(),
    val defaultInspectorName: String = "",
    val defaultOrganization: String = "Orkney Islands Council — Education, Communities & Housing",
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DISABLED_CATEGORY_IDS = stringSetPreferencesKey("disabled_category_ids")
        val DISABLED_QUESTION_IDS = stringSetPreferencesKey("disabled_question_ids")
        val CUSTOM_QUESTIONS_JSON = stringPreferencesKey("custom_questions_json")
        val DEFAULT_INSPECTOR_NAME = stringPreferencesKey("default_inspector_name")
        val DEFAULT_ORGANIZATION = stringPreferencesKey("default_organization")
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val customListType = Types.newParameterizedType(
        List::class.java,
        ChecklistQuestionTemplate::class.java,
    )
    private val customListAdapter = moshi.adapter<List<ChecklistQuestionTemplate>>(customListType)

    val settingsFlow: Flow<InspectorSettings> = context.settingsDataStore.data.map { prefs ->
        val disabledCategories = prefs[Keys.DISABLED_CATEGORY_IDS] ?: emptySet()
        val enabledCategories = ChecklistCatalog.allCategoryIds - disabledCategories
        val disabledQuestions = prefs[Keys.DISABLED_QUESTION_IDS] ?: emptySet()
        val customJson = prefs[Keys.CUSTOM_QUESTIONS_JSON]
        val customQuestions = if (!customJson.isNullOrBlank()) {
            runCatching { customListAdapter.fromJson(customJson) }.getOrNull().orEmpty()
        } else {
            emptyList()
        }
        val inspectorName = prefs[Keys.DEFAULT_INSPECTOR_NAME] ?: ""
        val organization = prefs[Keys.DEFAULT_ORGANIZATION]
            ?: "Orkney Islands Council — Education, Communities & Housing"

        InspectorSettings(
            enabledCategoryIds = enabledCategories,
            disabledQuestionIds = disabledQuestions,
            customQuestions = customQuestions,
            defaultInspectorName = inspectorName,
            defaultOrganization = organization,
        )
    }

    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { prefs ->
                val current = (prefs[Keys.DISABLED_CATEGORY_IDS] ?: emptySet()).toMutableSet()
                if (enabled) current.remove(categoryId) else current.add(categoryId)
                prefs[Keys.DISABLED_CATEGORY_IDS] = current.toSet()
            }
        }
    }

    suspend fun setQuestionEnabled(questionId: String, enabled: Boolean) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { prefs ->
                val current = (prefs[Keys.DISABLED_QUESTION_IDS] ?: emptySet()).toMutableSet()
                if (enabled) current.remove(questionId) else current.add(questionId)
                prefs[Keys.DISABLED_QUESTION_IDS] = current.toSet()
            }
        }
    }

    suspend fun addCustomQuestion(question: ChecklistQuestionTemplate) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { prefs ->
                val currentJson = prefs[Keys.CUSTOM_QUESTIONS_JSON]
                val currentList = if (!currentJson.isNullOrBlank()) {
                    runCatching { customListAdapter.fromJson(currentJson) }.getOrNull().orEmpty()
                } else {
                    emptyList()
                }
                prefs[Keys.CUSTOM_QUESTIONS_JSON] = customListAdapter.toJson(currentList + question)
            }
        }
    }

    suspend fun removeCustomQuestion(questionId: String) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { prefs ->
                val currentJson = prefs[Keys.CUSTOM_QUESTIONS_JSON]
                val currentList = if (!currentJson.isNullOrBlank()) {
                    runCatching { customListAdapter.fromJson(currentJson) }.getOrNull().orEmpty()
                } else {
                    emptyList()
                }
                val updated = currentList.filterNot { it.id == questionId }
                prefs[Keys.CUSTOM_QUESTIONS_JSON] = customListAdapter.toJson(updated)

                val disabled = (prefs[Keys.DISABLED_QUESTION_IDS] ?: emptySet()).toMutableSet()
                disabled.remove(questionId)
                prefs[Keys.DISABLED_QUESTION_IDS] = disabled.toSet()
            }
        }
    }

    suspend fun resetChecklistDefaults() {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { prefs ->
                prefs[Keys.DISABLED_CATEGORY_IDS] = emptySet()
                prefs[Keys.DISABLED_QUESTION_IDS] = emptySet()
            }
        }
    }

    suspend fun setDefaultInspectorName(name: String) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { prefs ->
                prefs[Keys.DEFAULT_INSPECTOR_NAME] = name
            }
        }
    }

    suspend fun setDefaultOrganization(org: String) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { prefs ->
                prefs[Keys.DEFAULT_ORGANIZATION] = org
            }
        }
    }
}
