package com.gembyte.poslite.ui.screens.home

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gembyte.poslite.components.printer.BluetoothPermissionHelper
import com.gembyte.poslite.components.printer.ThermalPrinterHelper
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.CustomerEntity
import com.gembyte.poslite.data.local.entity.CustomerLedgerEntity
import com.gembyte.poslite.data.local.entity.CustomerLedgerItemEntity
import com.gembyte.poslite.data.local.entity.ProductEntity
import com.gembyte.poslite.data.local.entity.SaleBillEntity
import com.gembyte.poslite.data.local.entity.SaleItemEntity
import com.gembyte.poslite.data.model.BillCartItem
import com.gembyte.poslite.data.model.LedgerType
import com.gembyte.poslite.data.model.PaymentType
import com.gembyte.poslite.ui.screens.product.StockUpdateDialog
import com.gembyte.poslite.ui.theme.blue
import com.gembyte.poslite.ui.theme.blueLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    )
    { granted ->
        /*if (!granted) {
            Toast.makeText(context, "Bluetooth permission denied", Toast.LENGTH_LONG).show()
        }*/
    }

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    val productDao = db.productDao()
    val salesDao = db.salesDao()
    val customerDao = db.customerDao()
    val ledgerDao = db.ledgerDao()

    val products by productDao
        .getProducts()
        .collectAsState(initial = emptyList())

    val customers by customerDao
        .getCustomers()
        .collectAsState(
            initial = emptyList()
        )

    var search by remember {
        mutableStateOf("")
    }

    var barcodeText by remember {
        mutableStateOf("")
    }

    val cart = remember {
        mutableStateListOf<BillCartItem>()
    }

    var latestAddedId by remember {
        mutableStateOf<Long?>(null)
    }

    var shouldScrollToTop by remember {
        mutableStateOf(false)
    }

    var stockProduct by remember {
        mutableStateOf<ProductEntity?>(null)
    }

    val inventoryDao = db.inventoryUpdateDao()

    val cartListState = rememberLazyListState()

    fun addToCart(product: ProductEntity) {

        if (product.quantity <= 0) return

        val existing = cart.find {
            it.product.id == product.id
        }

        if (existing != null) {

            if (existing.quantity < product.quantity) {
                existing.quantity++
            }

            latestAddedId = existing.product.id

        } else {

            // Add at TOP
            cart.add(
                index = 0,
                element = BillCartItem(product)
            )

            latestAddedId = product.id
        }

        // Clear search automatically
        search = ""
        shouldScrollToTop = true
    }

    LaunchedEffect(shouldScrollToTop) {
        if (shouldScrollToTop) {
            // wait until list recomposes
            yield()
            cartListState.scrollToItem(0)
            shouldScrollToTop = false
        }
    }

    var showCheckoutDialog by remember {
        mutableStateOf(false)
    }

    var overallDiscount by remember {
        mutableDoubleStateOf(0.0)
    }

    var showOverallDiscount by remember {
        mutableStateOf(false)
    }

    var isCreditSale by remember {
        mutableStateOf(false)
    }

    var selectedCustomer by remember {
        mutableStateOf<CustomerEntity?>(null)
    }

    var customerDropdownExpanded by remember {
        mutableStateOf(false)
    }

    val subTotal by remember {
        derivedStateOf {
            cart.sumOf {
                (it.product.wholesalePrice - it.discount) * it.quantity
            }
        }
    }

    val finalBillAmount by remember {
        derivedStateOf {
            maxOf(
                0.0,
                subTotal - overallDiscount
            )
        }
    }

    val hasLossItems by remember {
        derivedStateOf {
            cart.any {
                (it.product.wholesalePrice - it.discount) < it.product.purchasePrice
            }
        }
    }

    val filteredProducts = remember(
        products,
        search
    ) {
        val cleaned = search.trim()
        if (cleaned.all { it.isDigit() }) {
            products
        } else {
            products.filter {
                it.productName.contains(
                    cleaned,
                    true
                )
            }
        }
    }

    LaunchedEffect(barcodeText) {

        if (barcodeText.isBlank()) {
            return@LaunchedEffect
        }

        val product = products.firstOrNull {
            it.barcode.trim() == barcodeText.trim()
        }

        if (product != null) {
            val existing = cart.firstOrNull {
                it.product.id == product.id
            }

            if (existing != null) {
                if (
                    existing.quantity <
                    product.quantity
                ) {
                    existing.quantity++
                }

            } else {
                if (product.quantity > 0) {
                    cart.add(BillCartItem(product))
                }
            }
            barcodeText = ""
        }
    }

    stockProduct?.let { product ->
        StockUpdateDialog(
            product = product,
            onDismiss = {
                stockProduct = null
            },

            onSave = { updated, log ->
                scope.launch {
                    productDao.update(updated)
                    inventoryDao.insert(log)
                    stockProduct = null
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onMenuClick
                ) {

                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null
                    )
                }

                Spacer(Modifier.width(5.dp))

                OutlinedTextField(
                    value = search,

                    onValueChange = { value ->

                        search = value

                        val cleaned = value.trim()

                        val isScannerInput = cleaned.length >= 13 &&
                                cleaned.all {
                                    it.isDigit()
                                }

                        if (isScannerInput) {

                            val matchedProduct = products.firstOrNull { it.barcode == cleaned }

                            if (matchedProduct != null) {
                                addToCart(matchedProduct)
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        "Product not found",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }

                            search = ""
                            shouldScrollToTop = true
                        }
                    },
                    label = { Text("Search or Scan Barcode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (search.isBlank()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Scan or Search to see products",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Products will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }
                }

            } else {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts) { product ->

                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { addToCart(product) },
                                    onLongClick = { stockProduct = product }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {

                            Column {

                                // IMAGE
                                if (!product.productImage.isNullOrEmpty()) {

                                    AsyncImage(
                                        model = product.productImage,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp
                                                )
                                            ),
                                        contentScale = ContentScale.Crop
                                    )

                                } else {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .background(blueLight),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Text(
                                            text = product.productName
                                                .substringBefore(" ")
                                                .take(12),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {

                                    Text(
                                        text = product.productName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "${product.quantity} Available",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {

                                        Text(
                                            text = "Rs. ${product.wholesalePrice.toInt()}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.width(5.dp))

                                        Text(
                                            text = "/ ${product.weightUnit}",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        VerticalDivider()

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight()
        ) {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = blue,
                shape = RoundedCornerShape(10.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "○",
                        fontSize = 24.sp,
                        color = Color.White
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = if (cart.isEmpty()) {
                            "Bill Section"
                        } else {
                            "${cart.size} Item${if (cart.size > 1) "s" else ""}"
                        },
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )

                    if (cart.isNotEmpty()) {

                        Text(
                            text = "Rs ${String.format("%,.0f", finalBillAmount)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LaunchedEffect(latestAddedId) {
                if (latestAddedId != null) {
                    delay(700)
                    latestAddedId = null
                }
            }

            LazyColumn(
                state = cartListState,
                modifier = Modifier.weight(1f)
            ) {

                items(cart) { cartItem ->

                    val animatedColor by animateColorAsState(
                        targetValue =
                            if (latestAddedId == cartItem.product.id)
                                Color(0xFFE8F5E9)
                            else
                                Color.Transparent,
                        label = ""
                    )

                    val effectivePrice =
                        maxOf(0.0, cartItem.product.wholesalePrice - cartItem.discount)
                    val purchasePrice = cartItem.product.purchasePrice
                    val isLossSale = effectivePrice < purchasePrice
                    val isNoProfitSale = effectivePrice == purchasePrice

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                animatedColor,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Image
                        if (!cartItem.product.productImage.isNullOrEmpty()) {

                            AsyncImage(
                                model = cartItem.product.productImage,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )

                        } else {

                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(
                                        blueLight,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    cartItem.product.productName
                                        .substringBefore(" ")
                                        .take(2),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = cartItem.product.productName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Price: Rs. ${effectivePrice.toInt()} / ${cartItem.product.weightUnit}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Subtotal: Rs ${(effectivePrice * cartItem.quantity).toInt()}",
                                fontWeight = FontWeight.Medium
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                IconButton(
                                    onClick = {
                                        if (cartItem.quantity > 1) {
                                            cartItem.quantity--
                                        } else {
                                            cart.remove(cartItem)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = null
                                    )
                                }

                                Text(
                                    text = cartItem.quantity.toString(),
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp
                                    ),
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        if (
                                            cartItem.quantity <
                                            cartItem.product.quantity
                                        ) {
                                            cartItem.quantity++
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        cartItem.showDiscount =
                                            !cartItem.showDiscount
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = null
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        cart.remove(cartItem)
                                    }
                                ) {

                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = cartItem.showDiscount
                    ) {

                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 6.dp
                            )
                        ) {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Text("Discount", modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        if (cartItem.discount > 0) {
                                            cartItem.discount--
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = null
                                    )
                                }

                                Text("Rs ${cartItem.discount.toInt()}")

                                IconButton(
                                    onClick = {
                                        if (
                                            cartItem.discount <
                                            cartItem.product.wholesalePrice
                                        ) {
                                            cartItem.discount++
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.AddCircleOutline,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }

                    if (isLossSale || isNoProfitSale) {

                        Text(
                            text =
                                if (isLossSale)
                                    "⚠ Selling below purchase price"
                                else
                                    "⚠ No profit on this item",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 12.dp
                            )
                        )
                    }

                    HorizontalDivider()
                }
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            if (hasLossItems) {
                Text(text = "⚠ One or more items are being sold below purchase price. Checkout blocked.")
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (cart.isNotEmpty()) {
                        showCheckoutDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = cart.isNotEmpty() && !hasLossItems,
            ) {

                Text(
                    text = "Total Bill: Rs $finalBillAmount",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showCheckoutDialog) {

        var customerName by remember {
            mutableStateOf("")
        }

        var printBill by remember {
            mutableStateOf(false)
        }

        var printDetailedBill by remember {
            mutableStateOf(false)
        }

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = {
                Text("Confirm Sale")
            },

            text = {

                Column {

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = "Credit Sale",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Switch(
                                checked = isCreditSale,
                                onCheckedChange = {
                                    isCreditSale = it
                                }
                            )
                        }
                    }

                    //if (isCreditSale) {

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = {
                            customerDropdownExpanded = !customerDropdownExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Customer")
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = {
                                customerDropdownExpanded = false
                            }
                        ) {
                            customers.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    onClick = {
                                        selectedCustomer = it
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    //}

                    Card {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Row {
                                Text(
                                    "Subtotal",
                                    modifier = Modifier.weight(1f)
                                )

                                Text("Rs $subTotal")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row {
                                Text(
                                    "Bill Discount",
                                    modifier = Modifier.weight(1f)
                                )

                                Text("Rs $overallDiscount")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            HorizontalDivider()

                            Spacer(modifier = Modifier.height(4.dp))

                            Row {
                                Text(
                                    "Final Total",
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    "Rs $finalBillAmount",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showOverallDiscount =
                                !showOverallDiscount
                        }
                    ) {
                        Text("Bill Discount")
                    }

                    if (showOverallDiscount) {

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value =
                                if (overallDiscount == 0.0)
                                    ""
                                else
                                    overallDiscount.toString(),

                            onValueChange = {
                                overallDiscount =
                                    it.toDoubleOrNull()
                                        ?: 0.0
                            },

                            label = {
                                Text("Discount")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = printBill,
                            onCheckedChange = {
                                printBill = it
                            }
                        )
                        Text("Print Bill")
                    }

                    if (printBill) {

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = printDetailedBill,
                                onCheckedChange = {
                                    printDetailedBill = it
                                }
                            )
                            Text("Print Detailed Bill")
                        }
                    }
                }
            },

            confirmButton = {

                Row {

                    if (isCreditSale) {

                        TextButton(
                            enabled = selectedCustomer != null,
                            onClick = {
                                scope.launch {

                                    val customer = selectedCustomer ?: return@launch

                                    // ==========================
                                    // CALCULATE PROFIT
                                    // ==========================

                                    var grossProfit = 0.0

                                    cart.forEach {
                                        grossProfit += (
                                                (
                                                        it.product.wholesalePrice -
                                                                it.discount
                                                        ) -
                                                        it.product.purchasePrice
                                                ) * it.quantity
                                    }

                                    val totalProfit = grossProfit - overallDiscount

                                    // ==========================
                                    // CREATE SALE BILL
                                    // ==========================

                                    val billId = salesDao.insertBill(
                                        SaleBillEntity(
                                            totalAmount = finalBillAmount,
                                            totalProfit = totalProfit,
                                            overallDiscount = overallDiscount,
                                            paymentType = PaymentType.CREDIT
                                        )
                                    )

                                    // ==========================
                                    // SAVE BILL ITEMS
                                    // ==========================

                                    salesDao.insertItems(
                                        cart.map {
                                            SaleItemEntity(
                                                billId = billId,
                                                productId = it.product.id,
                                                productName = it.product.productName,
                                                purchasePrice = it.product.purchasePrice,
                                                wholesalePrice = it.product.wholesalePrice,
                                                quantity = it.quantity,
                                                discount = it.discount
                                            )
                                        }
                                    )

                                    // ==========================
                                    // CREATE CUSTOMER LEDGER
                                    // ==========================

                                    val ledgerId = ledgerDao.insertLedger(
                                        CustomerLedgerEntity(
                                            customerId = customer.id,
                                            type = LedgerType.CREDIT,
                                            amount = finalBillAmount,
                                            note = "Credit Sale Bill #$billId"
                                        )
                                    )

                                    // ==========================
                                    // SAVE LEDGER ITEMS
                                    // ==========================

                                    ledgerDao.insertItems(
                                        cart.map {
                                            CustomerLedgerItemEntity(
                                                ledgerId = ledgerId,
                                                productId = it.product.id,
                                                productName = it.product.productName,
                                                quantity = it.quantity,
                                                salePrice = it.product.wholesalePrice,
                                                discount = it.discount
                                            )
                                        }
                                    )

                                    // ==========================
                                    // REDUCE STOCK
                                    // ==========================

                                    cart.forEach {
                                        productDao.update(
                                            it.product.copy(
                                                quantity = it.product.quantity - it.quantity
                                            )
                                        )
                                    }

                                    // ==========================
                                    // RESET UI
                                    // ==========================

                                    cart.clear()
                                    selectedCustomer = null
                                    overallDiscount = 0.0
                                    showOverallDiscount = false
                                    showCheckoutDialog = false
                                }
                            }

                        ) {
                            Text("Save Credit")
                        }

                    } else {

                        TextButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {

                                        // ==========================
                                        // CALCULATE PROFIT
                                        // ==========================

                                        var grossProfit = 0.0

                                        cart.forEach {

                                            grossProfit += (
                                                    (
                                                            it.product.wholesalePrice -
                                                                    it.discount
                                                            ) -
                                                            it.product.purchasePrice
                                                    ) * it.quantity
                                        }

                                        val totalProfit =
                                            grossProfit - overallDiscount

                                        // ==========================
                                        // CREATE NORMAL BILL
                                        // (appears in reports immediately)
                                        // ==========================

                                        val billId = salesDao.insertBill(
                                            SaleBillEntity(
                                                totalAmount = finalBillAmount,
                                                totalProfit = totalProfit,
                                                overallDiscount = overallDiscount,
                                                paymentType = PaymentType.CASH
                                            )
                                        )

                                        // ==========================
                                        // SAVE BILL ITEMS
                                        // ==========================

                                        salesDao.insertItems(
                                            cart.map {
                                                SaleItemEntity(
                                                    billId = billId,
                                                    productId = it.product.id,
                                                    productName = it.product.productName,
                                                    purchasePrice = it.product.purchasePrice,
                                                    wholesalePrice = it.product.wholesalePrice,
                                                    quantity = it.quantity,
                                                    discount = it.discount
                                                )
                                            }
                                        )

                                        // ==========================
                                        // CUSTOMER RECEIVABLE ENTRY
                                        // ==========================

                                        val customer = selectedCustomer

                                        val ledgerId = ledgerDao.insertLedger(
                                            CustomerLedgerEntity(
                                                customerId = customer?.id ?: 0,
                                                type = LedgerType.PAYMENT,
                                                amount = finalBillAmount,
                                                note = "Bill #$billId"
                                            )
                                        )

                                        // ==========================
                                        // SAVE PRODUCTS INSIDE LEDGER
                                        // ==========================

                                        ledgerDao.insertItems(

                                            cart.map {
                                                CustomerLedgerItemEntity(
                                                    ledgerId = ledgerId,
                                                    productId = it.product.id,
                                                    productName = it.product.productName,
                                                    quantity = it.quantity,
                                                    salePrice = it.product.wholesalePrice,
                                                    discount = it.discount
                                                )
                                            }
                                        )

                                        // ==========================
                                        // REDUCE STOCK
                                        // ==========================

                                        cart.forEach {
                                            productDao.update(
                                                it.product.copy(
                                                    quantity = it.product.quantity - it.quantity
                                                )
                                            )
                                        }

                                        // ==========================
                                        // Print Bill
                                        // ==========================

                                        val billItems = cart.toList()
                                        if (printBill) {

                                            if (BluetoothPermissionHelper.hasBluetoothPermissions(
                                                    context
                                                )
                                            ) {

                                                ThermalPrinterHelper.printBill(
                                                    context = context,
                                                    billId = billId,
                                                    billDate = System.currentTimeMillis(),
                                                    cart = billItems,
                                                    overallDiscount = overallDiscount,
                                                    finalTotal = finalBillAmount,
                                                    isDetailedBill = printDetailedBill
                                                )

                                            } else {

                                                launcher.launch(
                                                    arrayOf(
                                                        Manifest.permission.BLUETOOTH_CONNECT,
                                                        Manifest.permission.BLUETOOTH_SCAN
                                                    )
                                                )
                                            }
                                        }
                                        // ==========================
                                        // RESET UI
                                        // ==========================

                                        cart.clear()
                                        selectedCustomer = null
                                        overallDiscount = 0.0
                                        showOverallDiscount = false
                                        showCheckoutDialog = false
                                    }
                                }
                            }
                        ) {
                            Text("Confirm Sale")
                        }
                    }
                }
            }
        )
    }
}