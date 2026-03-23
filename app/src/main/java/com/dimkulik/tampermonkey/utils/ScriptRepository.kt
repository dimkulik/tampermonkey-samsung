package com.dimkulik.tampermonkey.utils

import android.content.Context
import com.dimkulik.tampermonkey.model.UserScript
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ScriptRepository {

    private const val PREFS_NAME = "tampermonkey_prefs"
    private const val KEY_SCRIPTS = "scripts"
    private val gson = Gson()

    fun getAll(ctx: Context): MutableList<UserScript> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_SCRIPTS, "[]") ?: "[]"
        val type  = object : TypeToken<MutableList<UserScript>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun save(ctx: Context, scripts: List<UserScript>) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SCRIPTS, gson.toJson(scripts))
            .apply()
    }

    fun add(ctx: Context, script: UserScript) {
        val list = getAll(ctx)
        list.add(script)
        save(ctx, list)
    }

    fun update(ctx: Context, script: UserScript) {
        val list = getAll(ctx)
        val idx  = list.indexOfFirst { it.id == script.id }
        if (idx >= 0) list[idx] = script
        save(ctx, list)
    }

    fun delete(ctx: Context, id: String) {
        val list = getAll(ctx).filter { it.id != id }
        save(ctx, list)
    }

    fun getEnabled(ctx: Context): List<UserScript> =
        getAll(ctx).filter { it.enabled }

    /** Проверяем подходит ли скрипт для данного URL */
    fun matchingScripts(ctx: Context, url: String): List<UserScript> {
        return getEnabled(ctx).filter { script ->
            script.matches.any { pattern -> matchesPattern(pattern, url) } &&
            script.excludes.none { pattern -> matchesPattern(pattern, url) }
        }
    }

    private fun matchesPattern(pattern: String, url: String): Boolean {
        if (pattern == "<all_urls>") return true
        // Конвертируем glob-паттерн в regex
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return try {
            Regex(regex).containsMatchIn(url)
        } catch (e: Exception) {
            false
        }
    }
}
