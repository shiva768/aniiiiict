package com.zelretch.aniiiiict.ui.common

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AnimationSaneProgressIndicator(modifier: Modifier = Modifier) {
    if (LocalTestMode.current) {
        LinearProgressIndicator(modifier = modifier)
    } else {
        CircularProgressIndicator(modifier = modifier)
    }
}
