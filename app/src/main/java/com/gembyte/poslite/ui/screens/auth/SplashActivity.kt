package com.gembyte.poslite.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gembyte.poslite.R
import com.gembyte.poslite.ui.theme.blue
import com.gembyte.poslite.ui.theme.blueLight
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToPin: () -> Unit
) {

    LaunchedEffect(Unit) {
        delay(1500)
        onNavigateToPin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(blue),
        contentAlignment = Alignment.Center
    ) {
        Column {

            Image(
                painter = painterResource(R.drawable.icon_logo),
                contentDescription = null,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}