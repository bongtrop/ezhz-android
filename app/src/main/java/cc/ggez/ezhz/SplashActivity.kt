package cc.ggez.ezhz

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.topjohnwu.superuser.Shell
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class SplashActivity : AppCompatActivity() {
    companion object {
        init {
            Shell.enableVerboseLogging = true
        }
    }

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                startMain()
            } else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("Notification Permission Required")
                builder.setMessage("For foreground service, notification permission is required.")
                builder.setPositiveButton("OK") { _, _ ->
                    startMain()
                }

                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMain()
        }

    }

    private fun startMain() {
        Shell.getShell { shell ->
            if (!shell.isRoot) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("Root Required")
                builder.setMessage("Please root your device and grant su permission first.")
                builder.setPositiveButton("Exit") { _, _ ->
                    finish()
                }

                val dialog: AlertDialog = builder.create()
                dialog.show()
                return@getShell
            }

            copyAssets()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    private fun copyAssets() {
        val assetManager = assets
        var files: Array<String>? = null
        val abi = Build.SUPPORTED_ABIS[0]
        val arch =  if (abi.matches("armeabi-v7a|arm64-v8a".toRegex())) "armeabi-v7a" else "x86"

        files = assetManager.list(arch)

        if (files != null) {
            for (file in files) {
                try {
                    val inFile = assetManager.open("${arch}/$file")
                    val outFile = FileOutputStream("${filesDir.absolutePath}/${file}")
                    copyFile(inFile, outFile)
                    inFile.close()
                    outFile.flush()
                    outFile.close()
                } catch (e: IOException) {
                    Log.e("tag", "Failed to copy asset file: $file", e)
                }
            }
        }
    }
}