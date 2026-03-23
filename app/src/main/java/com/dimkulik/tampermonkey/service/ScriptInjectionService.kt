package com.dimkulik.tampermonkey.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.dimkulik.tampermonkey.utils.ScriptRepository

/**
 * Этот сервис — точка входа для Samsung Internet.
 * Браузер вызывает его когда загружает страницу.
 * Мы возвращаем JavaScript который нужно выполнить на странице.
 */
class ScriptInjectionService : Service() {

    companion object {
        private const val TAG = "TampermonkeyService"
        const val ACTION_REQUEST = "com.samsung.android.sbrowser.contentService.REQUEST"
        const val EXTRA_URL = "url"
        const val EXTRA_RESULT = "result"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        val url = intent.getStringExtra(EXTRA_URL) ?: ""
        Log.d(TAG, "Page loaded: $url")

        if (url.isNotEmpty()) {
            val scripts = ScriptRepository.matchingScripts(applicationContext, url)
            Log.d(TAG, "Matching scripts for $url: ${scripts.size}")

            if (scripts.isNotEmpty()) {
                val combinedJs = buildInjectionScript(scripts.map { it.code }, url)
                // Отправляем JS обратно в браузер
                val result = Intent(ACTION_REQUEST)
                result.putExtra(EXTRA_RESULT, combinedJs)
                sendBroadcast(result)
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Оборачиваем скрипты в безопасную среду выполнения
     * с базовыми реализациями GM_* функций
     */
    private fun buildInjectionScript(scripts: List<String>, pageUrl: String): String {
        val gmApi = """
(function() {
    // ── GM API реализация ──────────────────────────────────────
    const _storage = {};

    window.GM_setValue = function(key, value) {
        try { localStorage.setItem('_gm_' + key, JSON.stringify(value)); } catch(e) {}
    };
    window.GM_getValue = function(key, defaultValue) {
        try {
            const v = localStorage.getItem('_gm_' + key);
            return v !== null ? JSON.parse(v) : defaultValue;
        } catch(e) { return defaultValue; }
    };
    window.GM_deleteValue = function(key) {
        try { localStorage.removeItem('_gm_' + key); } catch(e) {}
    };
    window.GM_listValues = function() {
        const keys = [];
        for (let i = 0; i < localStorage.length; i++) {
            const k = localStorage.key(i);
            if (k && k.startsWith('_gm_')) keys.push(k.slice(4));
        }
        return keys;
    };
    window.GM_log = function(...args) { console.log('[UserScript]', ...args); };
    window.GM_notification = function(details) {
        if (typeof details === 'string') details = { text: details };
        console.log('[GM_notification]', details.title || 'Info', details.text);
    };
    window.GM_openInTab = function(url) { window.open(url, '_blank'); };
    window.GM_setClipboard = function(text) {
        navigator.clipboard?.writeText(text).catch(() => {});
    };
    window.GM_addStyle = function(css) {
        const style = document.createElement('style');
        style.textContent = css;
        (document.head || document.documentElement).appendChild(style);
    };
    window.GM_xmlhttpRequest = function(details) {
        const xhr = new XMLHttpRequest();
        xhr.open(details.method || 'GET', details.url);
        if (details.headers) {
            Object.entries(details.headers).forEach(([k, v]) => {
                try { xhr.setRequestHeader(k, v); } catch(e) {}
            });
        }
        xhr.onload = () => details.onload?.({ 
            responseText: xhr.responseText, 
            status: xhr.status,
            finalUrl: xhr.responseURL 
        });
        xhr.onerror = (e) => details.onerror?.(e);
        xhr.send(details.data || null);
    };
    // Алиасы
    window.GM = {
        setValue: window.GM_setValue,
        getValue: window.GM_getValue,
        deleteValue: window.GM_deleteValue,
        listValues: window.GM_listValues,
        xmlHttpRequest: window.GM_xmlhttpRequest,
        openInTab: window.GM_openInTab,
        notification: window.GM_notification,
        addStyle: window.GM_addStyle,
        log: window.GM_log,
    };
    window.unsafeWindow = window;
    window.GM_info = {
        script: { name: 'UserScript', version: '1.0' },
        scriptHandler: 'TampermonkeySamsung',
        version: '1.0.0'
    };
})();
        """.trimIndent()

        val userScripts = scripts.joinToString("\n\n") { code ->
            """
// ── User Script ─────────────────────────────────────────────
(function() {
    'use strict';
    $code
})();
            """.trimIndent()
        }

        return "$gmApi\n\n$userScripts"
    }
}
