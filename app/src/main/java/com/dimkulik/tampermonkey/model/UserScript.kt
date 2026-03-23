package com.dimkulik.tampermonkey.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class UserScript(
    @SerializedName("id")      val id: String,
    @SerializedName("name")    val name: String,
    @SerializedName("version") val version: String = "1.0.0",
    @SerializedName("description") val description: String = "",
    @SerializedName("author")  val author: String = "",
    @SerializedName("matches") val matches: List<String> = emptyList(),
    @SerializedName("excludes") val excludes: List<String> = emptyList(),
    @SerializedName("code")    val code: String,
    @SerializedName("enabled") var enabled: Boolean = true,
    @SerializedName("updateUrl") val updateUrl: String = "",
    @SerializedName("installUrl") val installUrl: String = "",
    @SerializedName("runAt")   val runAt: String = "document-end"
) {
    companion object {
        /**
         * Парсим .user.js файл — читаем заголовок ==UserScript==
         */
        fun fromUserJs(code: String, installUrl: String = ""): UserScript {
            val meta = parseMetadata(code)
            return UserScript(
                id          = java.util.UUID.randomUUID().toString(),
                name        = meta["name"] ?: "Без названия",
                version     = meta["version"] ?: "1.0.0",
                description = meta["description"] ?: "",
                author      = meta["author"] ?: "",
                matches     = meta.getAll("match") + meta.getAll("include"),
                excludes    = meta.getAll("exclude"),
                code        = code,
                enabled     = true,
                updateUrl   = meta["updateURL"] ?: meta["updateUrl"] ?: "",
                installUrl  = installUrl,
                runAt       = meta["run-at"] ?: "document-end"
            )
        }

        private fun parseMetadata(code: String): MetaMap {
            val map = MetaMap()
            val inBlock = StringBuilder()
            var inside = false
            for (line in code.lines()) {
                val trimmed = line.trim()
                if (trimmed == "// ==UserScript==") { inside = true; continue }
                if (trimmed == "// ==/UserScript==") { inside = false; continue }
                if (!inside) continue
                val m = Regex("//\\s*@(\\S+)\\s+(.+)").matchEntire(trimmed) ?: continue
                map.add(m.groupValues[1].trim(), m.groupValues[2].trim())
            }
            return map
        }
    }
}

class MetaMap {
    private val map = mutableMapOf<String, MutableList<String>>()

    fun add(key: String, value: String) {
        map.getOrPut(key) { mutableListOf() }.add(value)
    }

    operator fun get(key: String): String? = map[key]?.firstOrNull()

    fun getAll(key: String): List<String> = map[key] ?: emptyList()
}
