package com.gembyte.poslite.ui.screens.reports

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gembyte.poslite.components.printer.BluetoothPermissionHelper
import com.gembyte.poslite.components.printer.ThermalPrinterHelper
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.BillWithItems
import com.gembyte.poslite.data.local.entity.ProductEntity
import com.gembyte.poslite.data.model.BillCartItem
import com.gembyte.poslite.data.model.ReportDay
import com.gembyte.poslite.data.model.WeightUnit
import com.gembyte.poslite.data.model.deleteSale
import com.gembyte.poslite.data.model.exportBackup
import com.gembyte.poslite.data.model.importBackup
import com.gembyte.poslite.data.model.toDateString
import com.gembyte.poslite.ui.theme.blueLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    onBackPressed: () -> Unit
) {

    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    var billToPrint by remember {
        mutableStateOf<BillWithItems?>(null)
    }

    val scope = rememberCoroutineScope()

    val salesDao = db.salesDao()
    val productDao = db.productDao()

    var showDetails by remember {
        mutableStateOf(false)
    }

    val bills by salesDao
        .getBills()
        .collectAsState(
            initial = emptyList()
        )

    var selectedBill by remember {
        mutableStateOf<BillWithItems?>(null)
    }

    var selectedDeleteBill by remember {
        mutableStateOf<BillWithItems?>(null)
    }

    var selectedFilter by remember {
        mutableStateOf("Today")
    }

    var startDate by remember {
        mutableStateOf<Long?>(null)
    }

    var endDate by remember {
        mutableStateOf<Long?>(null)
    }

    val filteredBills = remember(
        bills,
        selectedFilter,
        startDate,
        endDate
    ) {
        val now = System.currentTimeMillis()
        bills.filter { bill ->
            when (selectedFilter) {

                "Today" -> {
                    bill.bill.billDate.toDateString() ==
                            now.toDateString()
                }

                "Month" -> {

                    val billMonth =
                        SimpleDateFormat(
                            "MM-yyyy",
                            Locale.getDefault()
                        ).format(
                            Date(
                                bill.bill.billDate
                            )
                        )

                    val currentMonth =
                        SimpleDateFormat(
                            "MM-yyyy",
                            Locale.getDefault()
                        ).format(
                            Date(now)
                        )

                    billMonth == currentMonth
                }

                else -> {

                    (startDate == null ||
                            bill.bill.billDate >= startDate!!) &&

                            (endDate == null ||
                                    bill.bill.billDate <= endDate!!)
                }
            }
        }
    }

    val reports = remember(filteredBills) {
        filteredBills
            .groupBy {
                it.bill.billDate.toDateString()
            }
            .map { entry ->

                ReportDay(
                    date = entry.key,

                    totalSale =
                        entry.value.sumOf {
                            it.bill.totalAmount
                        },

                    totalProfit =
                        entry.value.sumOf {
                            it.bill.totalProfit
                        },

                    bills =
                        entry.value
                )
            }
            .sortedByDescending {
                it.date
            }
    }

    val totalSales = filteredBills.sumOf {
        it.bill.totalAmount
    }

    val totalProfit = filteredBills.sumOf {
        it.bill.totalProfit
    }

    val totalBills = filteredBills.size

    selectedDeleteBill?.let { bill ->

        AlertDialog(
            onDismissRequest = {
                selectedDeleteBill = null
            },
            title = { Text("Delete Bill") },
            text = { Text("Are you sure you want to delete Bill #${bill.bill.id}? Stock will be restored.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            deleteSale(
                                bill = bill,
                                productDao = productDao,
                                salesDao = salesDao
                            )
                            selectedDeleteBill = null
                        }
                    }
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        selectedDeleteBill = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    billToPrint?.let { bill ->

        AlertDialog(
            onDismissRequest = {
                billToPrint = null
            },

            title = {
                Text("Print Receipt")
            },

            text = {
                Text("Select the receipt format you want to print.")
            },

            confirmButton = {

                Row {

                    TextButton(
                        onClick = {

                            if (
                                BluetoothPermissionHelper.hasBluetoothPermissions(context)
                            ) {

                                scope.launch {

                                    reprintBill(
                                        context = context,
                                        bill = bill,
                                        isDetailedBill = false
                                    )

                                    billToPrint = null
                                }

                            } else {

                                launcher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            }
                        }
                    ) {

                        Text("Short Bill")
                    }

                    TextButton(
                        onClick = {

                            if (
                                BluetoothPermissionHelper.hasBluetoothPermissions(context)
                            ) {

                                scope.launch {

                                    reprintBill(
                                        context = context,
                                        bill = bill,
                                        isDetailedBill = true
                                    )

                                    billToPrint = null
                                }

                            } else {

                                launcher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            }
                        }
                    ) {

                        Text("Detailed Bill")
                    }
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        billToPrint = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    importBackup(
                        context = context,
                        uri = it
                    )

                    Toast.makeText(
                        context,
                        "Backup restored successfully",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    )

    var billSearch by remember {
        mutableStateOf("")
    }

    val searchedBill = remember(billSearch, bills) {
        billSearch.toLongOrNull()?.let { id ->
            bills.firstOrNull {
                it.bill.id == id
            }
        }
    }

    fun openBillByNumber() {

        if (billSearch.isBlank()) {

            Toast.makeText(
                context,
                "Enter bill number",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (searchedBill != null) {

            selectedBill = searchedBill
            billSearch = ""

        } else {

            Toast.makeText(
                context,
                "Bill not found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxSize()
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
                text = "Reports",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {

                            val file = exportBackup(context)
                            Toast.makeText(
                                context,
                                "Backup Saved:\n${file.name}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Backup,
                        contentDescription = null
                    )
                }

                Spacer(Modifier.width(5.dp))

                IconButton(
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/json")
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 10.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        )
        {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Column {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        FilterChip(
                            selected = selectedFilter == "Today",
                            onClick = {
                                selectedFilter = "Today"
                            },
                            label = { Text("Today") }
                        )

                        Spacer(Modifier.width(8.dp))

                        FilterChip(
                            selected = selectedFilter == "Month",
                            onClick = {
                                selectedFilter = "Month"
                            },
                            label = { Text("This Month") }
                        )

                        Spacer(Modifier.width(8.dp))

                        FilterChip(
                            selected = selectedFilter == "All",
                            onClick = {
                                selectedFilter = "All"
                            },
                            label = { Text("All Time") }
                        )

                        Spacer(Modifier.weight(1f))

                        OutlinedTextField(
                            value = billSearch,
                            onValueChange = {
                                billSearch = it.filter { c ->
                                    c.isDigit()
                                }
                            },
                            modifier = Modifier.width(150.dp),
                            singleLine = true,
                            label = { Text("Bill #") },
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Search
                                ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    openBillByNumber()
                                }
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                openBillByNumber()
                            }
                        ) {
                            Icon(Icons.Default.Search, null)
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                showDetails = !showDetails
                            }
                        ) {
                            Icon(
                                if (showDetails)
                                    Icons.Default.VisibilityOff
                                else
                                    Icons.Default.Visibility,
                                null
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            text = "Sales",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 14.sp
                        )

                        Text(
                            text = if (showDetails) "Rs ${totalSales.toInt()}" else "*****",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            text = "Profit",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 14.sp
                        )

                        Text(
                            text = if (showDetails) "Rs ${totalProfit.toInt()}" else "*****",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            text = "Bills",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 14.sp
                        )

                        Text(
                            text = if (showDetails) totalBills.toString() else "*****",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedFilter == "Today") {

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(filteredBills) { bill ->

                    BillCard(
                        bill = bill,
                        onView = {
                            selectedBill = bill
                        },
                        onDelete = {
                            selectedDeleteBill = bill
                        },
                        onPrint = {
                            billToPrint = bill
                        }
                    )
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {

                items(reports) { report ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),

                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Column(
                            Modifier.padding(12.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Text(
                                    "Date : ${report.date}"
                                )

                                Text(
                                    "Sale : Rs ${
                                        report.totalSale.toInt()
                                    }"
                                )

                                Text(
                                    "Profit : Rs ${
                                        report.totalProfit.toInt()
                                    }"
                                )
                            }

                            Spacer(
                                Modifier.height(
                                    10.dp
                                )
                            )

                            LazyRow {

                                items(
                                    report.bills
                                ) { bill ->

                                    BillCard(
                                        bill = bill,
                                        onView = { selectedBill = bill },
                                        onDelete = {
                                            selectedDeleteBill = bill
                                        },
                                        onPrint = {
                                            billToPrint = bill
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        /*LazyColumn(modifier = Modifier.weight(1f)) {
            items(reports) { report ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                )
                {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Date : ${report.date}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Total Sale : Rs ${String.format("%.2f", report.totalSale)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Total Profit : Rs ${String.format("%.2f", report.totalProfit)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(report.bills) { bill ->

                                Card(
                                    modifier = Modifier.width(200.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = blueLight
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    )
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {

                                        Text(
                                            text = "Bill #${bill.bill.id}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Rs ${String.format("%.2f", bill.bill.totalAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {

                                            Spacer(modifier = Modifier.weight(1f))

                                            IconButton(
                                                onClick = {
                                                    selectedBill = bill
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Visibility,
                                                    null
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (BluetoothPermissionHelper.hasBluetoothPermissions(context)) {
                                                        scope.launch {
                                                            reprintBill(
                                                                context,
                                                                bill
                                                            )
                                                        }
                                                    } else {
                                                        launcher.launch(
                                                            arrayOf(
                                                                Manifest.permission.BLUETOOTH_CONNECT,
                                                                Manifest.permission.BLUETOOTH_SCAN
                                                            )
                                                        )
                                                    }
                                                }

                                            ) {

                                                Icon(
                                                    Icons.Default.Print,
                                                    contentDescription = "Print Bill"
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    selectedDeleteBill = bill
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, null)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }*/

    }

    selectedBill?.let {
        BillDetailsDialog(
            bill = it,
            salesDao = salesDao,
            productDao = productDao,
            onDismiss = {
                selectedBill = null
            }
        )
    }
}

@Composable
private fun BillCard(
    bill: BillWithItems,
    onView: () -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier
            .width(200.dp)
            .padding(5.dp),
        colors = CardDefaults.cardColors(containerColor = blueLight)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    )
        ) {

            Text(
                "Bill #${bill.bill.id}"
            )

            Spacer(
                Modifier.height(
                    6.dp
                )
            )

            Text(
                "Rs ${
                    String.format(
                        "%.2f",
                        bill.bill.totalAmount
                    )
                }",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Row {

                Spacer(Modifier.weight(1f))

                IconButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, null)
                }

                IconButton(onClick = onPrint) {
                    Icon(Icons.Default.Print, null)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        }
    }
}

private suspend fun reprintBill(
    context: Context,
    bill: BillWithItems,
    isDetailedBill: Boolean
) {
    withContext(Dispatchers.IO) {
        ThermalPrinterHelper.printBill(
            context = context,
            billId = bill.bill.id,
            billDate = bill.bill.billDate,
            cart = bill.items.map {
                BillCartItem(
                    product = ProductEntity(
                        id = it.productId,
                        productName = it.productName,
                        barcode = "",
                        purchasePrice = it.purchasePrice,
                        wholesalePrice = it.wholesalePrice,
                        retailPrice = 0.0,
                        weightUnit = WeightUnit.BOX,
                        quantity = 0
                    )
                ).apply {
                    quantity = it.quantity
                    discount = it.discount
                }
            },
            overallDiscount = bill.bill.overallDiscount,
            finalTotal = bill.bill.totalAmount,
            isDetailedBill = isDetailedBill
        )
    }
}



