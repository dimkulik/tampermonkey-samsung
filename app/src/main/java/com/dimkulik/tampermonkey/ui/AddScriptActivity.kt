package com.dimkulik.tampermonkey.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dimkulik.tampermonkey.databinding.ActivityAddScriptBinding
import com.dimkulik.tampermonkey.model.UserScript
import com.dimkulik.tampermonkey.utils.ScriptRepository
import okhttp3.OkHttpClient
import okhttp3.Request

class AddScriptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddScriptBinding
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScriptBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Установить по URL"

        binding.btnInstall.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isEmpty()) {
                binding.etUrl.error = "Введите URL"
                return@setOnClickListener
            }
            installFromUrl(url)
        }

        // Популярные источники скриптов
        binding.btnGreasyfork.setOnClickListener {
            binding.etUrl.setText("https://greasyfork.org/")
            Toast.makeText(this, "Найдите скрипт на Greasy Fork и вставьте прямую ссылку на .user.js", Toast.LENGTH_LONG).show()
        }
    }

    private fun installFromUrl(url: String) {
        val finalUrl = if (!url.endsWith(".user.js") && !url.contains(".user.js?"))
            url else url

        binding.btnInstall.isEnabled = false
        binding.btnInstall.text = "Загружаю..."

        Thread {
            try {
                val req      = Request.Builder().url(finalUrl).build()
                val response = client.newCall(req).execute()

                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this, "Ошибка: ${response.code}", Toast.LENGTH_SHORT).show()
                        resetButton()
                    }
                    return@Thread
                }

                val code = response.body?.string() ?: ""
                if (code.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this, "Файл пустой", Toast.LENGTH_SHORT).show()
                        resetButton()
                    }
                    return@Thread
                }

                val script = UserScript.fromUserJs(code, finalUrl)

                runOnUiThread {
                    resetButton()
                    AlertDialog.Builder(this)
                        .setTitle("Установить скрипт?")
                        .setMessage(
                            "Название: ${script.name}\n" +
                            "Версия: ${script.version}\n" +
                            "Автор: ${script.author}\n\n" +
                            "Описание: ${script.description}\n\n" +
                            "Работает на:\n${script.matches.joinToString("\n")}"
                        )
                        .setPositiveButton("Установить") { _, _ ->
                            ScriptRepository.add(this, script)
                            Toast.makeText(this, "«${script.name}» установлен!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }
        }.start()
    }

    private fun resetButton() {
        binding.btnInstall.isEnabled = true
        binding.btnInstall.text = "Установить"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
