package cc.ggez.ezhz.module.sharedpref

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedPrefViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "SharedPref Modifier Coming Soon"
    }
    val text: LiveData<String> = _text
}