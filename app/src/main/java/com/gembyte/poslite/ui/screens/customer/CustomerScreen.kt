package com.gembyte.poslite.ui.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.CustomerEntity
import com.gembyte.poslite.data.model.WeightUnit
import kotlinx.coroutines.launch

@Composable
fun CustomerScreen(
    onBackPressed: () -> Unit
) {

    val context = LocalContext.current

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    val customerDao = db.customerDao()

    val customers by customerDao
        .getCustomers()
        .collectAsState(
            initial = emptyList()
        )

    val scope = rememberCoroutineScope()

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var selectedCustomer by remember {
        mutableStateOf<CustomerEntity?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        Column() {

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
                    text = "Customers",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.weight(1f))

            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4)
            ) {

                items(customers) { customer ->

                    Card(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold
                            )

                            if (customer.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(customer.phone)
                            }

                            if (customer.address.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(customer.address)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row {

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        selectedCustomer =
                                            customer
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        null
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            customerDao.delete(
                                                customer
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                showAddDialog = true
            },
            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
        ) {
            Icon(Icons.Default.Add, null)
        }
    }

    if (showAddDialog) {

        CustomerDialog(
            customer = null,
            onDismiss = {
                showAddDialog = false
            },
            onSave = {
                scope.launch { customerDao.insert(it) }
                showAddDialog = false
            }
        )
    }

    selectedCustomer?.let {

        CustomerDialog(
            customer = it,
            onDismiss = {
                selectedCustomer = null
            },
            onSave = { customer ->
                scope.launch {
                    customerDao.update(
                        customer
                    )
                }

                selectedCustomer = null
            }
        )
    }
}