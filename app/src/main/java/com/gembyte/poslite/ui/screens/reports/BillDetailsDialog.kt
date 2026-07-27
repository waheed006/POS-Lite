package com.gembyte.poslite.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gembyte.poslite.data.local.dao.ProductDao
import com.gembyte.poslite.data.local.dao.SalesDao
import com.gembyte.poslite.data.local.entity.BillWithItems
import com.gembyte.poslite.data.local.entity.SaleItemEntity
import com.gembyte.poslite.data.model.toDateString
import kotlinx.coroutines.launch

@Composable
fun BillDetailsDialog(
    bill: BillWithItems,
    salesDao: SalesDao,
    productDao: ProductDao,
    onDismiss: () -> Unit
) {

    var editMode by remember {
        mutableStateOf(false)
    }

    val removedItems = remember {
            mutableStateListOf<SaleItemEntity>()
        }

    val visibleItems = remember(bill) {
            bill.items.toMutableStateList()
        }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Bill #${bill.bill.id}"
            )
        },
        text = {
            Column {
                Text(
                    "Date : ${
                        bill.bill.billDate
                            .toDateString()
                    }"
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier =
                        Modifier.height(300.dp)
                ) {

                    items(visibleItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {

                            Box {

                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                ) {

                                    Text(
                                        item.productName
                                    )

                                    Text(
                                        "Qty : ${item.quantity}"
                                    )

                                    Text(
                                        "Price : ${item.wholesalePrice}"
                                    )

                                    Text(
                                        "Discount : ${item.discount}"
                                    )

                                    Text(
                                        "Subtotal : ${
                                            String.format(
                                                "%.2f",
                                                (
                                                        item.wholesalePrice -
                                                                item.discount
                                                        ) *
                                                        item.quantity
                                            )
                                        }"
                                    )
                                }

                                if (editMode) {

                                    IconButton(

                                        modifier = Modifier
                                            .fillMaxWidth(1f)
                                            .align(Alignment.TopEnd),
                                        onClick = {
                                            removedItems.add(
                                                item
                                            )

                                            visibleItems.remove(
                                                item
                                            )
                                        }

                                    ) {

                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                Text("Total Sale : Rs ${String.format("%.2f", bill.bill.totalAmount)}")

                if (bill.bill.overallDiscount > 0) {
                    Text("Bill Discount : Rs ${bill.bill.overallDiscount}")
                }

                Text("Profit : Rs ${String.format("%.2f", bill.bill.totalProfit)}")
            }
        },

        confirmButton = {

            Row {

                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        "Cancel"
                    )
                }

                if (!editMode) {

                    TextButton(
                        onClick = {

                            editMode =
                                true
                        }
                    ) {

                        Text(
                            "Edit"
                        )
                    }

                } else {

                    TextButton(
                        enabled = removedItems.isNotEmpty(),
                        onClick = {
                            scope.launch {

                                salesDao.updateBillAfterItemRemoval(
                                        bill = bill,
                                        removedItems = removedItems,
                                        productDao = productDao
                                    )

                                onDismiss()
                            }
                        }

                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    )
}