package cc.ggez.ezhz.ui.proxy

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import cc.ggez.ezhz.R

class ProxyFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting_proxy, rootKey)
    }
}