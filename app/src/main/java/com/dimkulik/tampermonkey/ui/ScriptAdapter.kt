package com.dimkulik.tampermonkey.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dimkulik.tampermonkey.databinding.ItemScriptBinding
import com.dimkulik.tampermonkey.model.UserScript

class ScriptAdapter(
    private val onToggle: (UserScript) -> Unit,
    private val onEdit:   (UserScript) -> Unit,
    private val onDelete: (UserScript) -> Unit,
    private val onUpdate: (UserScript) -> Unit,
) : ListAdapter<UserScript, ScriptAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val b: ItemScriptBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(script: UserScript) {
            b.tvName.text    = script.name
            b.tvVersion.text = "v${script.version}"
            b.tvAuthor.text  = if (script.author.isNotEmpty()) "by ${script.author}" else ""
            b.tvDesc.text    = script.description
            b.tvMatches.text = script.matches.take(3).joinToString(", ")

            b.switchEnabled.isChecked = script.enabled
            b.switchEnabled.setOnCheckedChangeListener(null)
            b.switchEnabled.setOnCheckedChangeListener { _, _ -> onToggle(script) }

            b.btnEdit.setOnClickListener   { onEdit(script) }
            b.btnDelete.setOnClickListener { onDelete(script) }
            b.btnUpdate.setOnClickListener { onUpdate(script) }

            // Показываем кнопку обновления только если есть URL
            b.btnUpdate.visibility = if (script.updateUrl.isNotEmpty())
                android.view.View.VISIBLE else android.view.View.GONE

            // Визуальное состояние включён/выключен
            b.root.alpha = if (script.enabled) 1.0f else 0.5f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScriptBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<UserScript>() {
        override fun areItemsTheSame(a: UserScript, b: UserScript) = a.id == b.id
        override fun areContentsTheSame(a: UserScript, b: UserScript) = a == b
    }
}
