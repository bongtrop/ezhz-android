package cc.ggez.ezhz.module.frida

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import cc.ggez.ezhz.databinding.ItemFridaBinding
import cc.ggez.ezhz.module.frida.model.FridaItem
import cc.ggez.ezhz.module.frida.model.GithubTag

class FridaAdapter: RecyclerView.Adapter<FridaViewHolder>() {
    private var fridaItems = mutableListOf<FridaItem>()
    private lateinit var onExecuteCallback: ((FridaItem, Int) -> Unit)
    private lateinit var onInstallCallback: ((FridaItem, Int) -> Unit)
    private var globalDisabled = false
    @SuppressLint("NotifyDataSetChanged")
    fun setFridaList(fridaItems: List<FridaItem>) {
        this.fridaItems = fridaItems.toMutableList()
        notifyDataSetChanged()
    }

    fun setExecuteListener(callback: (FridaItem, Int) -> Unit) {
        onExecuteCallback = callback
    }

    fun setInstallListener(callback: (FridaItem, Int) -> Unit) {
        onInstallCallback = callback
    }

    @SuppressLint("NotifyDataSetChanged")
    fun disableAll() {
        if (globalDisabled) return
        globalDisabled = true
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun enableAll() {
        if(!globalDisabled) return
        globalDisabled = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FridaViewHolder =
        FridaViewHolder(ItemFridaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: FridaViewHolder, position: Int) {
        holder.apply {
            bind(fridaItems[position], position, onExecuteCallback, onInstallCallback, globalDisabled)
        }
    }

    override fun getItemCount(): Int {
        return fridaItems.size
    }
}
class FridaViewHolder(private val binding: ItemFridaBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FridaItem, position: Int, onExecuteCallback: ((FridaItem, Int) -> Unit), onInstallCallback: ((FridaItem, Int) -> Unit), globalDisabled: Boolean) {
        binding.frida = item
        binding.btnExecute.setOnClickListener {
            onExecuteCallback(item, position)
        }
        binding.btnInstall.setOnClickListener {
            onInstallCallback(item, position)
        }
        binding.globalDisabled = globalDisabled
    }
}