package com.badgebot.controller

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.badgebot.controller.ble.BlePermissions
import com.badgebot.controller.ui.BadgeBotApp
import com.badgebot.controller.ui.ControllerViewModel
import com.badgebot.controller.ui.theme.BadgeBotTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BadgeBotTheme {
                val viewModel: ControllerViewModel = viewModel()

                var hasPermissions by remember {
                    mutableStateOf(hasAllPermissions())
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    hasPermissions = result.values.all { it }
                    if (hasPermissions) {
                        viewModel.startScan()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    BadgeBotApp(
                        viewModel = viewModel,
                        hasPermissions = hasPermissions,
                        onRequestPermissions = {
                            permissionLauncher.launch(BlePermissions.required)
                        },
                    )
                }
            }
        }
    }

    private fun hasAllPermissions(): Boolean =
        BlePermissions.required.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
}
