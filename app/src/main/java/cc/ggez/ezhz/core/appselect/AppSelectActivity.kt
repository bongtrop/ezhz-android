package cc.ggez.ezhz.core.appselect

import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.ggez.ezhz.R
import cc.ggez.ezhz.databinding.AppSelectActivityBinding
import cc.ggez.ezhz.core.appselect.AppSelectUtil.Companion.getApps
import kotlin.concurrent.thread


class AppSelectActivity : AppCompatActivity() {
    private lateinit var binding: AppSelectActivityBinding
    private lateinit var appAdapter: AppAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedApps = intent.getStringArrayListExtra("selectedApps") ?: ArrayList<String>()
         selectedApps.sort()

        binding = AppSelectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setLoading(true)

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)

        binding.rvAppList.layoutManager = layoutManager

        appAdapter = AppAdapter()
        binding.rvAppList.adapter = appAdapter

        binding.sbAppSearch.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                appAdapter.getFilter().filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                appAdapter.getFilter().filter(newText)
                return false
            }
        })

        binding.tbAppType.setOnCheckedChangeListener { _, isChecked ->
            appAdapter.setIsSystemAppShown(isChecked)
        }

        thread {
            val appList = getApps(baseContext)

            appList.forEach {
                if (selectedApps.binarySearch("${it.packageName}:${it.uid}") >= 0) {
                    it.proxyed = true
                }
            }
            appAdapter.addAll(appList.sortedBy { it.proxyed }.reversed() as ArrayList<AppObject>)
            runOnUiThread {
                appAdapter.notifyDataSetChanged()
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.pbLoading.visibility = View.VISIBLE
            binding.rvAppList.visibility = View.GONE
            binding.sbAppSearch.visibility = View.GONE
            binding.tbAppType.visibility = View.GONE
        } else {
            binding.pbLoading.visibility = View.GONE
            binding.rvAppList.visibility = View.VISIBLE
            binding.sbAppSearch.visibility = View.VISIBLE
            binding.tbAppType.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.manu_app_select, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.action_save -> {
                setResult(RESULT_OK, Intent().apply {
                    putStringArrayListExtra("selectedApps", appAdapter.getSelectedApp().map { "${it.packageName}:${it.uid}" } as ArrayList<String>)
                })
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}