package com.dimkulik.tampermonkey.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dimkulik.tampermonkey.R
import com.dimkulik.tampermonkey.databinding.ActivityMainBinding
import com.dimkulik.tampermonkey.model.UserScript
import com.dimkulik.tampermonkey.utils.ScriptRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ScriptAdapter
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Tampermonkey"

        setupRecyclerView()
        setupFab()
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshScripts()
    }

    private fun setupRecyclerView() {
        adapter = ScriptAdapter(
            onToggle  = { script -> toggleScript(script) },
            onEdit    = { script -> openEditor(script) },
            onDelete  = { script -> confirmDelete(script) },
            onUpdate  = { script -> updateScript(script) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        refreshScripts()
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            showAddMenu()
        }
    }

    private fun showAddMenu() {
        val options = arrayOf(
            "📋 Вставить код вручную",
            "🔗 Установить по URL",
            "✏️ Создать новый скрипт"
        )
        AlertDialog.Builder(this)
            .setTitle("Добавить скрипт")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openEditor(null)
                    1 -> startActivity(Intent(this, AddScriptActivity::class.java))
                    2 -> openEditor(null)
                }
            }
            .show()
    }

    private fun refreshScripts() {
        val scripts = ScriptRepository.getAll(this)
        adapter.submitList(scripts)
        binding.emptyView.visibility =
            if (scripts.isEmpty()) android.view.View.VISIBLE
            else android.view.View.GONE
    }

    private fun toggleScript(script: UserScript) {
        val updated = script.copy(enabled = !script.enabled)
        ScriptRepository.update(this, updated)
        refreshScripts()
        val state = if (updated.enabled) "включён" else "выключен"
        Toast.makeText(this, "${script.name} $state", Toast.LENGTH_SHORT).show()
    }

    private fun openEditor(script: UserScript?) {
        val intent = Intent(this, EditorActivity::class.java)
        script?.let { intent.putExtra(EditorActivity.EXTRA_SCRIPT_ID, it.id) }
        startActivity(intent)
    }

    private fun confirmDelete(script: UserScript) {
        AlertDialog.Builder(this)
            .setTitle("Удалить скрипт?")
            .setMessage("«${script.name}» будет удалён безвозвратно.")
            .setPositiveButton("Удалить") { _, _ ->
                ScriptRepository.delete(this, script.id)
                refreshScripts()
                Toast.makeText(this, "Скрипт удалён", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateScript(script: UserScript) {
        if (script.updateUrl.isEmpty()) {
            Toast.makeText(this, "URL обновления не указан", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Проверяю обновление...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val req      = Request.Builder().url(script.updateUrl).build()
                val response = client.newCall(req).execute()
                val code     = response.body?.string() ?: return@Thread
                val updated  = UserScript.fromUserJs(code, script.updateUrl)
                    .copy(id = script.id, enabled = script.enabled)
                ScriptRepository.update(this, updated)
                runOnUiThread {
                    refreshScripts()
                    Toast.makeText(this, "«${updated.name}» обновлён до v${updated.version}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // Обработка входящего .user.js файла/URL
    private fun handleIncomingIntent(intent: Intent?) {
        intent ?: return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
        uri ?: return

        val url = uri.toString()
        if (!url.contains(".user.js")) return

        Toast.makeText(this, "Загружаю скрипт...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val req      = Request.Builder().url(url).build()
                val response = client.newCall(req).execute()
                val code     = response.body?.string() ?: return@Thread
                val script   = UserScript.fromUserJs(code, url)

                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Установить скрипт?")
                        .setMessage("Название: ${script.name}\nВерсия: ${script.version}\nАвтор: ${script.author}\n\nОписание: ${script.description}")
                        .setPositiveButton("Установить") { _, _ ->
                            ScriptRepository.add(this, script)
                            refreshScripts()
                            Toast.makeText(this, "«${script.name}» установлен!", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import_url -> {
                startActivity(Intent(this, AddScriptActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
