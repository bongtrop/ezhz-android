package cc.ggez.ezhz.ui.sharedpref

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedPrefViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is shared pref Fragment"
    }
    val text: LiveData<String> = _text
}