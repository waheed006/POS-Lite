package com.gembyte.poslite.ui.screens.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gembyte.poslite.data.local.entity.InventoryUpdateEntity
import com.gembyte.poslite.data.local.entity.ProductEntity
import kotlinx.coroutines.launch

@Composable
fun StockUpdateDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: suspend (
        ProductEntity,
        InventoryUpdateEntity
    ) -> Unit
) {

    val scope = rememberCoroutineScope()

    var qty by remember {
        mutableStateOf("")
    }

    var updatePrice by remember {
        mutableStateOf(false)
    }

    var purchasePrice by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    var newWholesalePrice by remember {
        mutableStateOf("")
    }

    val addQty =
        qty.toIntOrNull() ?: 0

    val enteredPurchase =
        purchasePrice
            .toDoubleOrNull()
            ?: product.purchasePrice

    val weightedPrice =
        if (
            updatePrice &&
            addQty > 0
        ) {
            (
                    (
                            product.purchasePrice *
                                    product.quantity
                            )
                            +
                            (
                                    enteredPurchase *
                                            addQty
                                    )
                    ) /
                    (
                            product.quantity +
                                    addQty
                            )

        } else {
            product.purchasePrice
        }

    val requireWholesaleUpdate =
        weightedPrice >
                product.wholesalePrice

    val finalWholesalePrice =
        if (requireWholesaleUpdate)
            newWholesalePrice
                .toDoubleOrNull()
                ?: product.wholesalePrice
        else
            product.wholesalePrice

    val canUpdate =
        addQty > 0 &&
                (
                        !requireWholesaleUpdate
                                ||
                                finalWholesalePrice >
                                weightedPrice
                        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Stock for: '${product.productName}'") },
        text = {

            Column {

                Text("Current Stock: ${product.quantity}")

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = qty,
                    onValueChange = {
                        qty = it
                    },
                    label = {
                        Text(
                            "Add Quantity"
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Checkbox(
                        checked = updatePrice,
                        onCheckedChange = {
                            updatePrice = it
                        }
                    )

                    Text(
                        "Purchase price changed"
                    )
                }

                if (updatePrice) {

                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = {
                            purchasePrice = it
                        },
                        label = {
                            Text(
                                "New Purchase Price"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )

                    Text(
                        text =
                            "Weighted Avg: Rs ${
                                String.format(
                                    "%.2f",
                                    weightedPrice
                                )
                            }",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (requireWholesaleUpdate) {

                    Spacer(
                        Modifier.height(
                            12.dp
                        )
                    )

                    Text(
                        text = "⚠ Purchase became higher than selling price",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        Modifier.height(
                            6.dp
                        )
                    )

                    Text(
                        text =
                            "Current Wholesale: Rs ${
                                product.wholesalePrice.toInt()
                            }"
                    )

                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )

                    OutlinedTextField(
                        value = newWholesalePrice,
                        onValueChange = {
                            newWholesalePrice = it
                        },
                        label = {
                            Text(
                                "New Wholesale Price"
                            )
                        },
                        supportingText = {
                            val value =
                                newWholesalePrice
                                    .toDoubleOrNull()

                            if (
                                value != null &&
                                value <= weightedPrice
                            ) {

                                Text(
                                    "Must be greater than purchase price"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }

                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text(
                            "Note (optional)"
                        )
                    }
                )
            }
        },

        confirmButton = {

            TextButton(
                enabled = canUpdate,
                onClick = {

                    scope.launch {

                        val updated = product.copy(
                                quantity = product.quantity + addQty,
                                purchasePrice = weightedPrice,
                                wholesalePrice = finalWholesalePrice
                            )

                        val log = InventoryUpdateEntity(
                                productId = product.id,
                                oldQuantity = product.quantity,
                                addedQuantity = addQty,
                                newQuantity = updated.quantity,
                                oldPurchasePrice = product.purchasePrice,
                                enteredPurchasePrice = if (updatePrice)
                                        enteredPurchase
                                    else
                                        null,
                                finalPurchasePrice = weightedPrice,
                                note = note
                            )

                        onSave(updated, log)
                    }
                }
            ) {

                Text(
                    "Update"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )
}