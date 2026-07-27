package com.gembyte.poslite.ui.screens.customer

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gembyte.poslite.data.local.entity.CustomerEntity

@Composable
fun CustomerDialog(
    customer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {

    var name by remember {
        mutableStateOf(
            customer?.name ?: ""
        )
    }

    var phone by remember {
        mutableStateOf(
            customer?.phone ?: ""
        )
    }

    var address by remember {
        mutableStateOf(
            customer?.address ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (customer == null)
                    "Add Customer"
                else
                    "Edit Customer"
            )
        },

        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Customer Name")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
                    label = {
                        Text("Phone")
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                    },
                    label = {
                        Text("Address")
                    }
                )
            }
        },

        confirmButton = {

            TextButton(
                onClick = {
                    if (name.isBlank())
                        return@TextButton

                    onSave(
                        CustomerEntity(
                            id = customer?.id ?: 0,
                            name = name,
                            phone = phone,
                            address = address
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}