package com.zelretch.aniiiiict.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiict.BuildConfig

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Implement logout */ }) {
            Text("Logout")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Version: ${BuildConfig.VERSION_NAME}")
    }
}
