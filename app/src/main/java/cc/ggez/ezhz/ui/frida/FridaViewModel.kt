package cc.ggez.ezhz.ui.frida

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FridaViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is frida Fragment"
    }
    val text: LiveData<String> = _text
}