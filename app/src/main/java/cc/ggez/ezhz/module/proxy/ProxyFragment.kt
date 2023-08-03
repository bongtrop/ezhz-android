package cc.ggez.ezhz.module.proxy

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import cc.ggez.ezhz.R
import cc.ggez.ezhz.core.appselect.AppSelectActivity


class ProxyFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = "ProxyFragment"

    private lateinit var isProxyRunningSwitch: SwitchPreferenceCompat

    private lateinit var isProxyAutoCheck: CheckBoxPreference
    private lateinit var proxyAutoUrlText: EditTextPreference
    private lateinit var proxyHostText: EditTextPreference
    private lateinit var proxyPortText: EditTextPreference
    private lateinit var proxyTypeList: ListPreference
    private lateinit var dnsText: EditTextPreference

    private lateinit var isAuthCheck: CheckBoxPreference
    private lateinit var authUsernameText: EditTextPreference
    private lateinit var authPasswordText: EditTextPreference

    private lateinit var isTargetGlobalCheck: CheckBoxPreference
    private lateinit var targetApps: Preference
    private lateinit var isTargetBypassModeCheck: CheckBoxPreference

    val appSelectActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val selectedApps = data?.getStringArrayListExtra("selectedApps")
            val targetAppsEdit = targetApps!!.sharedPreferences?.edit()!!
            targetAppsEdit.putStringSet("target_apps", selectedApps?.toSet())
            targetAppsEdit.apply()
            targetApps.summaryProvider = targetApps.summaryProvider
        }
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting_proxy, rootKey)

        isProxyRunningSwitch = findPreference("proxy_running")!!

        isProxyAutoCheck = findPreference("proxy_auto")!!
        proxyAutoUrlText = findPreference("proxy_pac_url")!!
        proxyHostText = findPreference("proxy_host")!!
        proxyPortText = findPreference("proxy_port")!!
        proxyTypeList = findPreference("proxy_type")!!
        dnsText = findPreference("proxy_dns")!!

        isAuthCheck = findPreference("auth_enable")!!
        authUsernameText = findPreference("auth_username")!!
        authPasswordText = findPreference("auth_password")!!

        isTargetGlobalCheck = findPreference("target_global")!!
        targetApps = findPreference("target_apps")!!
        isTargetBypassModeCheck = findPreference("target_bypass_mode")!!

        // App select
        targetApps.setOnPreferenceClickListener {
            val intent = Intent(context, AppSelectActivity::class.java)
            val selectedApps = it.sharedPreferences!!.getStringSet("target_apps", setOf<String>())
            intent.putExtra("selectedApps", ArrayList<String>(selectedApps!!))
            appSelectActivityLauncher.launch(intent)
            true
        }

        targetApps.summaryProvider = Preference.SummaryProvider<Preference> { preference ->
            getTargetAppsSummary(preference)
        }

        val isProxyAuto = findPreference<Preference>("proxy_auto")!!.sharedPreferences!!.getBoolean("proxy_auto", false)
        controlProxyAuto(isProxyAuto)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this);
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "proxy_running" -> {
                val isProxyRunning = sharedPreferences!!.getBoolean("proxy_running", false)
                if (isProxyRunning) {
                    disableAll();
                    if (!ProxySingleton.isConnecting) serviceStart()
                } else {
                    enableAll();
                    if (!ProxySingleton.isConnecting) serviceStop()
                }
            }
            "proxy_auto" -> {
                val isProxyAuto = sharedPreferences!!.getBoolean("proxy_auto", false)
                controlProxyAuto(isProxyAuto)
            }
        }
    }

    private fun getTargetAppsSummary(preference: Preference): String {
        val selectedApps = preference.sharedPreferences?.getStringSet("target_apps", setOf())
        val selectedSize = selectedApps?.size ?: 0
        return if (selectedSize > 0)
            "You have already selected $selectedSize app"
        else
            "Not select any app yet"
    }

    private fun controlProxyAuto(isProxyAuto: Boolean) {
        proxyAutoUrlText.isEnabled = isProxyAuto
        proxyHostText.isEnabled = !isProxyAuto
        proxyPortText.isEnabled = !isProxyAuto
    }

    private fun disableAll() {
        isProxyAutoCheck.isEnabled = false
        proxyAutoUrlText.isEnabled = false
        proxyHostText.isEnabled = false
        proxyPortText.isEnabled = false
        proxyTypeList.isEnabled = false
        dnsText.isEnabled = false

        isAuthCheck.isEnabled = false
        authUsernameText.isEnabled = false
        authPasswordText.isEnabled = false

        isTargetGlobalCheck.isEnabled = false
        targetApps.isEnabled = false
        isTargetBypassModeCheck.isEnabled = false
    }

    private fun enableAll() {
        isProxyAutoCheck.isEnabled = false
        proxyAutoUrlText.isEnabled = false
        proxyHostText.isEnabled = true
        proxyPortText.isEnabled = true
        proxyTypeList.isEnabled = true
        dnsText.isEnabled = true

        isAuthCheck.isEnabled = true
        authUsernameText.isEnabled = true
        authPasswordText.isEnabled = true

        isTargetGlobalCheck.isEnabled = true
        targetApps.isEnabled = true
        isTargetBypassModeCheck.isEnabled = true

    }

    private fun serviceStart() {
        if (ProxyService.isServiceStarted()) return
        val settings: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val proxyProfile = ProxyProfile.fromSharedPref(settings)
        try {
            val it = Intent(requireActivity(), ProxyService::class.java)
            it.putExtras(proxyProfile.toBundle())
            requireActivity().startForegroundService(it)
        } catch (e: Exception) {
            Log.e(TAG, "serviceStart: ${e.message}")
        }
    }

    private fun serviceStop() {
        if (!ProxyService.isServiceStarted()) return
        try {
            requireActivity().stopService(Intent(requireActivity(), ProxyService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "serviceStop: ${e.message}")
        }
    }
}