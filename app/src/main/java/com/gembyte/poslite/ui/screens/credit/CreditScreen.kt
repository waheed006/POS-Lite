package com.gembyte.poslite.ui.screens.credit

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.model.CustomerBalance
import com.gembyte.poslite.data.model.LedgerType
import com.gembyte.poslite.data.model.exportBackup
import com.gembyte.poslite.data.model.toDateString
import kotlinx.coroutines.launch

@Composable
fun CreditScreen(
    onBackPressed: () -> Unit
) {

    val context = LocalContext.current

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    val scope = rememberCoroutineScope()

    val ledgerDao = db.ledgerDao()

    val balances by ledgerDao
        .getCustomersWithBalance()
        .collectAsState(
            initial = emptyList()
        )

    var selectedCustomer by remember {
        mutableStateOf<CustomerBalance?>(null)
    }

    val totalOutstanding =
        balances.sumOf { it.balance }

    var expandedCustomerId by remember {
        mutableStateOf<Long?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    onBackPressed()
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Credit Screen",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.weight(1f))

        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            )
            {

                Text(
                    "Total Outstanding",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Rs $totalOutstanding",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        LazyColumn {

            items(balances) { customer ->

                Card(
                    shape = RoundedCornerShape(20.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = customer.customerName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = "Current Receivable",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Text(
                                    text = "Rs ${customer.balance}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    expandedCustomerId =
                                        if (expandedCustomerId == customer.customerId)
                                            null
                                        else
                                            customer.customerId
                                }
                            ) {
                                Text(
                                    if (
                                        expandedCustomerId ==
                                        customer.customerId
                                    )
                                        "Hide"
                                    else
                                        "History"
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = expandedCustomerId == customer.customerId
                        ) {

                            Column {

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider()

                                Spacer(modifier = Modifier.height(12.dp))

                                val ledgerEntries by ledgerDao
                                    .getCustomerLedger(customer.customerId)
                                    .collectAsState(
                                        initial = emptyList()
                                    )

                                ledgerEntries.forEach { entry ->

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .surfaceVariant
                                            )
                                    ) {

                                        Column(
                                            modifier =
                                                Modifier.padding(12.dp)
                                        ) {

                                            Text(
                                                text =
                                                    entry.ledger.date
                                                        .toDateString(),
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .labelMedium
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (
                                                entry.ledger.type ==
                                                LedgerType.CREDIT
                                            ) {

                                                Text(
                                                    "Credit Sale",
                                                    fontWeight =
                                                        FontWeight.Bold
                                                )

                                                Text(
                                                    "Rs ${entry.ledger.amount}"
                                                )

                                                if (
                                                    entry.items.isNotEmpty()
                                                ) {

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text(
                                                        "Products",
                                                        fontWeight =
                                                            FontWeight.SemiBold
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    entry.items.forEach {

                                                        Text(
                                                            "• ${it.productName} × ${it.quantity}"
                                                        )
                                                    }
                                                }

                                            } else {

                                                Text(
                                                    "Payment Received",
                                                    fontWeight =
                                                        FontWeight.Bold
                                                )

                                                Text(
                                                    "Rs ${entry.ledger.amount}"
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Button(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    onClick = {
                                        selectedCustomer = customer
                                    }
                                ) {
                                    Text("Receive Payment")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCustomer?.let {

        AddPaymentDialog(
            customerId = it.customerId,
            remaining = it.balance,
            onDismiss = {
                selectedCustomer = null
            }
        )
    }
}