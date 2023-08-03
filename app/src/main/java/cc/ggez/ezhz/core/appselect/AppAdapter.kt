package cc.ggez.ezhz.core.appselect

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import cc.ggez.ezhz.databinding.ItemAppBinding

class AppAdapter : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private var isSystemAppShown = false
    val rawAppList = ArrayList<AppObject>()
    var appList = ArrayList<AppObject>()


    // create an inner class with name ViewHolder
    // It takes a view argument, in which pass the generated class of single_item.xml
    // ie SingleItemBinding and in the RecyclerView.ViewHolder(binding.root) pass it like this
    inner class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    // inside the onCreateViewHolder inflate the view of SingleItemBinding
    // and return new ViewHolder object containing this layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    // bind the items with each item
    // of the list languageList
    // which than will be
    // shown in recycler view
    // to keep it simple we are
    // not setting any image data to view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(appList[position]){
                binding.tvAppName.text = this.name
                binding.tvPackageName.text = this.packageName
                binding.ivAppIcon.setImageDrawable(this.icon)
                binding.cardApp.isChecked = this.proxyed
                binding.cardApp.setOnClickListener {
                    binding.cardApp.isChecked = !binding.cardApp.isChecked
                    this.proxyed = binding.cardApp.isChecked
                }
            }
        }
    }

    // return the size of languageList
    override fun getItemCount(): Int {
        return appList.size
    }

    fun addAll(appList: ArrayList<AppObject>) {
        rawAppList.clear()
        rawAppList.addAll(appList)
        this.appList.clear()
        this.appList.addAll(getAppListFromRaw())
    }

    fun getFilter(): Filter {
        return cityFilter
    }

    fun getSelectedApp(): ArrayList<AppObject> {
        return ArrayList(appList.filter { it.proxyed })
    }

    fun setIsSystemAppShown(isSystemAppShown: Boolean) {
        this.isSystemAppShown = isSystemAppShown
        appList.clear()
        appList.addAll(getAppListFromRaw())
        notifyDataSetChanged()
    }

    fun getAppListFromRaw(): ArrayList<AppObject> {
        return ArrayList(rawAppList.filter { !it.system || isSystemAppShown || it.proxyed })
    }

    private val cityFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredAppList: ArrayList<AppObject> = ArrayList()
            if (constraint.isNullOrEmpty()) {
                getAppListFromRaw().let { filteredAppList.addAll(it) }
            } else {
                val query = constraint.toString().trim().lowercase()
                getAppListFromRaw().forEach {
                    if (it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) || it.proxyed) {
                        filteredAppList.add(it)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredAppList
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results?.values is ArrayList<*>) {
                appList.clear()
                appList.addAll(results.values as ArrayList<AppObject>)
                notifyDataSetChanged()
            }
        }
    }
}