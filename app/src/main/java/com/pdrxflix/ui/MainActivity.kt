package com.pdrxflix.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import com.pdrxflix.PdrXFlixApp
import com.pdrxflix.R
import com.pdrxflix.databinding.ActivityMainBinding
import com.pdrxflix.ui.details.DetailsFragment
import com.pdrxflix.ui.home.HomeFragment
import com.pdrxflix.ui.player.PlayerFragment
import com.pdrxflix.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity(), AppNavigator {

    private lateinit var binding: ActivityMainBinding
    private val readPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> showHome() }

    private val allFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { showHome() }

    private val homeFragment = HomeFragment()
    private val settingsFragment = SettingsFragment()

    private val repository by lazy {
        (application as PdrXFlixApp).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (savedInstanceState == null) {
            showHome()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHome()
                    true
                }
                R.id.nav_settings -> {
                    showSettings()
                    true
                }
                else -> false
            }
        }

        requestStorageAccessIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        requestStorageAccessIfNeeded()
    }

    private fun requestStorageAccessIfNeeded() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager() -> {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                allFilesLauncher.launch(intent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    readPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                    return
                }
            }
            else -> {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    readPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    return
                }
            }
        }
    }

    private fun showHome() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.isVisible = true
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, homeFragment, HomeFragment.TAG)
        }
    }

    private fun showSettings() {
        binding.bottomNav.selectedItemId = R.id.nav_settings
        binding.bottomNav.isVisible = true
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, settingsFragment, SettingsFragment.TAG)
        }
    }

    override fun openDetails(collectionId: Long) {
        binding.bottomNav.isVisible = false
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_right
            )
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, DetailsFragment.newInstance(collectionId), DetailsFragment.TAG)
            addToBackStack(DetailsFragment.TAG)
        }
    }

    override fun openPlayer(collectionId: Long, videoPath: String, startPositionMs: Long, episodeIndex: Int) {
        binding.bottomNav.isVisible = false
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_right
            )
            setReorderingAllowed(true)
            replace(
                R.id.fragmentContainer,
                PlayerFragment.newInstance(collectionId, videoPath, startPositionMs, episodeIndex),
                PlayerFragment.TAG
            )
            addToBackStack(PlayerFragment.TAG)
        }
    }

    override fun closeTransientScreen() {
        supportFragmentManager.popBackStack()
        binding.bottomNav.isVisible = true
    }

    override fun openSettings() {
        showSettings()
    }

    override fun openHome() {
        showHome()
    }

    override fun refreshLibrary() {
        supportFragmentManager.findFragmentByTag(HomeFragment.TAG)?.let { fragment ->
            if (fragment is HomeFragment) fragment.refreshLibrary()
        }
    }
}

interface AppNavigator {
    fun openDetails(collectionId: Long)
    fun openPlayer(collectionId: Long, videoPath: String, startPositionMs: Long = 0L, episodeIndex: Int = 0)
    fun closeTransientScreen()
    fun openSettings()
    fun openHome()
    fun refreshLibrary()
}
