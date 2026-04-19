package com.boringdroid.settings

import android.content.ContentResolver
import android.provider.Settings

/**
 * Per-package windowing-mode override map, persisted as a comma-delimited list in
 * [Settings.Secure] under [KEY]. The same key is later observed by the enforcer component in
 * BoringdroidSystemUI (M5.7b) so a write here also drives future launches once enforcement lands.
 */
object AppWindowingModeStore {

    const val KEY: String = "boringdroid_app_windowing_modes"

    enum class Mode(val token: String) {
        DEFAULT("default"),
        FREEFORM("freeform"),
        FULLSCREEN("fullscreen"),
        ;

        companion object {
            fun fromToken(token: String): Mode =
                values().firstOrNull { it.token == token } ?: DEFAULT
        }
    }

    fun load(resolver: ContentResolver): Map<String, Mode> {
        val raw = Settings.Secure.getString(resolver, KEY).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw
            .split(',')
            .mapNotNull { entry ->
                val pair = entry.trim().split('=', limit = 2)
                if (pair.size == 2 && pair[0].isNotEmpty()) {
                    pair[0] to Mode.fromToken(pair[1])
                } else {
                    null
                }
            }
            .toMap()
    }

    fun save(resolver: ContentResolver, map: Map<String, Mode>) {
        val serialized =
            map.entries
                .filter { it.value != Mode.DEFAULT }
                .joinToString(",") { "${it.key}=${it.value.token}" }
        Settings.Secure.putString(resolver, KEY, serialized.ifEmpty { null })
    }

    fun put(resolver: ContentResolver, packageName: String, mode: Mode) {
        val current = load(resolver).toMutableMap()
        if (mode == Mode.DEFAULT) {
            current.remove(packageName)
        } else {
            current[packageName] = mode
        }
        save(resolver, current)
    }
}
