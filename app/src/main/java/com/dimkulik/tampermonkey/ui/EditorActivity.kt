package com.dimkulik.tampermonkey.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dimkulik.tampermonkey.R
import com.dimkulik.tampermonkey.databinding.ActivityEditorBinding
import com.dimkulik.tampermonkey.model.UserScript
import com.dimkulik.tampermonkey.utils.ScriptRepository

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCRIPT_ID = "script_id"
        val TEMPLATE = """
// ==UserScript==
// @name         Мой скрипт
// @namespace    https://github.com/dimkulik
// @version      1.0.0
// @description  Описание скрипта
// @author       dimkulik
// @match        https://*/*
// @run-at       document-end
// ==/UserScript==

(function() {
    'use strict';

    // Твой код здесь
    console.log('Скрипт запущен на:', window.location.href);

})();
        """.trimIndent()
    }

    private lateinit var binding: ActivityEditorBinding
    private var existingScript: UserScript? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val scriptId = intent.getStringExtra(EXTRA_SCRIPT_ID)
        if (scriptId != null) {
            existingScript = ScriptRepository.getAll(this).find { it.id == scriptId }
            supportActionBar?.title = existingScript?.name ?: "Редактор"
            binding.etCode.setText(existingScript?.code ?: TEMPLATE)
        } else {
            supportActionBar?.title = "Новый скрипт"
            binding.etCode.setText(TEMPLATE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_save  -> { saveScript(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveScript() {
        val code = binding.etCode.text.toString().trim()
        if (code.isEmpty()) {
            Toast.makeText(this, "Код не может быть пустым", Toast.LENGTH_SHORT).show()
            return
        }

        val script = if (existingScript != null) {
            // Обновляем существующий — парсим мета заново
            UserScript.fromUserJs(code).copy(
                id      = existingScript!!.id,
                enabled = existingScript!!.enabled
            )
        } else {
            UserScript.fromUserJs(code)
        }

        if (existingScript != null) {
            ScriptRepository.update(this, script)
            Toast.makeText(this, "«${script.name}» обновлён", Toast.LENGTH_SHORT).show()
        } else {
            ScriptRepository.add(this, script)
            Toast.makeText(this, "«${script.name}» сохранён", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
