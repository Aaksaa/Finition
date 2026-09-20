package com.finition.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.finition.app.databinding.ActivityMainBinding
import com.finition.app.notifications.NotificationHelper
import com.finition.app.util.Format
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkOverdueInvoices()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = destination.label
        }

        maybeRequestNotificationPermission()
    }

    private fun maybeRequestNotificationPermission() {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> checkOverdueInvoices()
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED -> checkOverdueInvoices()
            else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Reminder pass on launch: surfaces overdue invoices as notifications. */
    private fun checkOverdueInvoices() {
        val repository = (application as FinitionApp).repository
        lifecycleScope.launch {
            val overdue = repository.observeOverdueInvoices(Format.startOfToday()).first()
            NotificationHelper.notifyOverdue(this@MainActivity, overdue)
        }
    }
}
