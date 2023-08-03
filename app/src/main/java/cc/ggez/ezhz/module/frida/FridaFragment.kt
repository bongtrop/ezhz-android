package cc.ggez.ezhz.module.frida

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.ggez.ezhz.R
import cc.ggez.ezhz.databinding.DialogDownloadBinding
import cc.ggez.ezhz.databinding.FragmentFridaBinding
import cc.ggez.ezhz.module.frida.helper.FridaHelper.Companion.checkFridaServerProcessTag


class FridaFragment : Fragment() {

    private var _binding: FragmentFridaBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val fridaAdapter = FridaAdapter()
    private lateinit var dialogDownloadBinding: DialogDownloadBinding
    private lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fridaViewModel =
            ViewModelProvider(this)[FridaViewModel::class.java]

        _binding = FragmentFridaBinding.inflate(inflater, container, false)
        val root: View = binding.root

        dialogDownloadBinding = DialogDownloadBinding.inflate(LayoutInflater.from(context))
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        builder.setView(dialogDownloadBinding.root)
        dialog = builder.create()


        binding.rvFrida.adapter = fridaAdapter

        fridaAdapter.setInstallListener { fridaItem, position ->
            if (!fridaItem.isInstallable) return@setInstallListener

            if (fridaItem.isInstalled) {
                fridaViewModel.uninstallFridaServer(fridaItem)
            } else {
                fridaViewModel.installFridaServer(fridaItem)
            }
        }

        fridaAdapter.setExecuteListener { fridaItem, position ->
            if (!fridaItem.isExecutable) return@setExecuteListener

            if (fridaItem.isExecuted) {
                fridaViewModel.killFridaServer(fridaItem)
            } else {
                val tag = checkFridaServerProcessTag()
                if (tag.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Frida Server v${tag} has already been executed.", Toast.LENGTH_LONG).show()
                    return@setExecuteListener
                }

                fridaViewModel.executeFridaServer(fridaItem)
            }
        }

        fridaViewModel.fridaItemList.observe(viewLifecycleOwner) { fridaItems ->
            fridaAdapter.setFridaList(fridaItems)
        }

        fridaViewModel.errorMessage.observe(viewLifecycleOwner, {
        })

        fridaViewModel.installProgress.observe(viewLifecycleOwner) {
            if (it == -1) {
                fridaAdapter.enableAll()
                dialog.dismiss()
                dialogDownloadBinding.progress = 0
            } else {
                fridaAdapter.disableAll()
                dialog.show()
                dialogDownloadBinding.progress = it
            }
        }

        fridaViewModel.getAllFridaItems(true)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}