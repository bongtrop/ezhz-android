package cc.ggez.ezhz.module.proxy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProxyViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is proxy Fragment"
    }
    val text: LiveData<String> = _text
}