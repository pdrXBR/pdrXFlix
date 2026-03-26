package com.pdrxflix.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pdrxflix.PdrXFlixApp
import com.pdrxflix.databinding.FragmentSettingsBinding
import com.pdrxflix.ui.AppNavigator

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { (requireActivity().application as PdrXFlixApp).repository }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.switchAutoplay.isChecked = repository.getAutoPlay()
        binding.switchAutoplay.setOnCheckedChangeListener { _, isChecked ->
            repository.setAutoPlay(isChecked)
        }

        binding.btnGrantStorage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.parse("package:${requireContext().packageName}")
                    }
                )
            } else {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${requireContext().packageName}")
                })
            }
        }

        binding.btnRescan.setOnClickListener {
            (activity as? AppNavigator)?.refreshLibrary()
        }

        binding.btnBack.setOnClickListener {
            (activity as? AppNavigator)?.openHome()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingsFragment"
    }
}
