package com.gembyte.poslite.ui.screens.credit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.CustomerLedgerEntity
import com.gembyte.poslite.data.model.LedgerType
import kotlinx.coroutines.launch

@Composable
fun AddPaymentDialog(
    customerId: Long, remaining: Double, onDismiss: () -> Unit
) {

    val context = LocalContext.current

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    val ledgerDao = db.ledgerDao()

    val scope = rememberCoroutineScope()

    var amount by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Receive Payment")
        },

        text = {

            Column {

                Text(
                    "Remaining : Rs $remaining"
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(value = amount, onValueChange = {
                    amount = it
                }, label = {
                    Text("Amount")
                })
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    val payment = amount.toDoubleOrNull() ?: return@TextButton
                    if (payment > remaining) return@TextButton

                    scope.launch {

                        ledgerDao.insertLedger(
                            CustomerLedgerEntity(
                                customerId = customerId,
                                type = LedgerType.PAYMENT,
                                amount = payment,
                                note = "Customer Payment"
                            )
                        )

                        onDismiss()
                    }
                }) {
                Text("Save")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        })
}