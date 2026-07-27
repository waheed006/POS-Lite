package com.gembyte.poslite.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gembyte.poslite.R
import com.gembyte.poslite.components.pref.PrefManager
import com.gembyte.poslite.ui.theme.blueLight

@Composable
fun PinScreen(
    onLoginSuccess: () -> Unit
) {

    val context = LocalContext.current

    val prefManager = remember {
        PrefManager.getInstance(context)
    }

    val isPinCreated = prefManager.isPinSet()

    var pin by remember {
        mutableStateOf("")
    }

    var logoTapCount by remember {
        mutableIntStateOf(0)
    }

    var resetMode by remember {
        mutableStateOf(false)
    }

    val continueAction: () -> Unit = {

        if (pin.length != 4) {

            Toast.makeText(
                context,
                "Enter 4 digit PIN",
                Toast.LENGTH_SHORT
            ).show()

        } else if (!isPinCreated || resetMode) {

            prefManager.savePin(pin)

            Toast.makeText(
                context,
                "PIN Saved",
                Toast.LENGTH_SHORT
            ).show()

            onLoginSuccess()

        } else {

            if (pin == prefManager.getPin()) {
                onLoginSuccess()
            } else {
                Toast.makeText(
                    context,
                    "Wrong PIN",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(blueLight)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                modifier = Modifier.clickable {
                    logoTapCount++
                    if (logoTapCount >= 15) {
                        resetMode = true
                        Toast
                            .makeText(
                                context,
                                "Reset Mode Enabled",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                },
                text = when {
                    resetMode -> "Reset PIN"
                    isPinCreated -> "Enter PIN"
                    else -> "Create PIN"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 4) {
                        pin = it
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done

                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        continueAction()
                    }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { continueAction() }
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}