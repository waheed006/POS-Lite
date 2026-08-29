package com.gembyte.poslite.ui.screens.home

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gembyte.poslite.components.printer.BluetoothPermissionHelper
import com.gembyte.poslite.components.printer.ThermalPrinterHelper
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.CustomerEntity
import com.gembyte.poslite.data.local.entity.ProductEntity
import com.gembyte.poslite.data.local.entity.SaleBillEntity
import com.gembyte.poslite.data.local.entity.SaleItemEntity
import com.gembyte.poslite.data.model.BillCartItem
import com.gembyte.poslite.data.model.CartItem
import com.gembyte.poslite.data.model.PaymentType
import com.gembyte.poslite.ui.screens.product.StockUpdateDialog
import com.gembyte.poslite.ui.theme.blue
import com.gembyte.poslite.ui.theme.blueLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Locale

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
                            text = "Rs ${String.format(Locale.US, "%,.0f", finalBillAmount)}",
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // =====================================================
                            // ADD DISCOUNT: +1
                            // =====================================================

                            OutlinedButton(
                                onClick = {
                                    cartItem.discount =
                                        minOf(
                                            cartItem.discount + 1.0,
                                            cartItem.product.wholesalePrice
                                        )
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "1",
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(
                                Modifier.width(6.dp)
                            )

                            // =====================================================
                            // ADD DISCOUNT: +3
                            // =====================================================

                            OutlinedButton(
                                onClick = {
                                    cartItem.discount =
                                        minOf(
                                            cartItem.discount + 3.0,
                                            cartItem.product.wholesalePrice
                                        )
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "3",
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(
                                Modifier.width(6.dp)
                            )

                            // =====================================================
                            // ADD DISCOUNT: +5
                            // =====================================================

                            OutlinedButton(
                                onClick = {
                                    cartItem.discount =
                                        minOf(
                                            cartItem.discount + 5.0,
                                            cartItem.product.wholesalePrice
                                        )
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "5",
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(
                                Modifier.weight(1f)
                            )

                            // =====================================================
                            // CURRENT DISCOUNT
                            // =====================================================

                            Text(
                                text = "Discount: Rs ${cartItem.discount.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(
                                Modifier.width(4.dp)
                            )

                            // =====================================================
                            // REMOVE DISCOUNT
                            // =====================================================

                            IconButton(
                                onClick = {
                                    cartItem.discount = 0.0
                                    cartItem.showDiscount = false
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove discount",
                                    tint = MaterialTheme.colorScheme.error
                                )
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

        // =========================================================
        // CHECKOUT STATE
        // =========================================================

        var printBill by remember {
            mutableStateOf(false)
        }

        var printDetailedBill by remember {
            mutableStateOf(false)
        }

        var printUrdu by remember {
            mutableStateOf(false)
        }

        var amountReceivedText by remember {
            mutableStateOf("")
        }

        val amountReceived =
            amountReceivedText.toDoubleOrNull()

        // If nothing is entered, consider the exact bill amount as received.
        // This allows the sale to be completed without manually typing the
        // exact amount.
        val effectiveAmountReceived =
            amountReceived ?: finalBillAmount

        val changeAmount =
            maxOf(
                0.0,
                effectiveAmountReceived - finalBillAmount
            )

        val remainingAmount =
            maxOf(
                0.0,
                finalBillAmount - effectiveAmountReceived
            )

        val isPaymentValid =
            effectiveAmountReceived >= finalBillAmount

        val totalItemDiscount = cart.sumOf {
            it.discount * it.quantity
        }

        val totalDiscount =
            totalItemDiscount + overallDiscount

        val totalBillItems =
            cart.sumOf {
                it.quantity
            }


        // =========================================================
        // DIALOG
        // =========================================================

        Dialog(
            onDismissRequest = {
                showCheckoutDialog = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f),

                shape = RoundedCornerShape(28.dp),

                tonalElevation = 6.dp,

                // =================================================
                // WHITE BACKGROUND
                // =================================================
                color = Color.White
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {

                    // =================================================
                    // HEADER
                    // =================================================

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Complete Sale",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Text(
                                text = "Review the bill and complete payment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                showCheckoutDialog = false
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }


                    Spacer(
                        Modifier.height(22.dp)
                    )


                    // =================================================
                    // MAIN TWO SECTIONS
                    // =================================================

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 360.dp),

                        horizontalArrangement =
                            Arrangement.spacedBy(20.dp)
                    ) {


                        // =================================================
                        // LEFT — BILL SUMMARY
                        // =================================================

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),

                            shape = RoundedCornerShape(18.dp),

                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp)
                            ) {

                                Text(
                                    text = "Bill Summary",
                                    style =
                                        MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    Modifier.height(6.dp)
                                )

                                // =================================================
                                // TOTAL ITEMS
                                // =================================================

                                Text(
                                    text = "$totalBillItems ${if (totalBillItems == 1) "Item" else "Items"}",
                                    style =
                                        MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(
                                    Modifier.height(22.dp)
                                )


                                // =================================================
                                // SUBTOTAL
                                // =================================================

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        text = "Subtotal",
                                        modifier = Modifier.weight(1f),
                                        style =
                                            MaterialTheme.typography.bodyLarge
                                    )

                                    Text(
                                        text = "Rs ${subTotal.toInt()}",
                                        fontWeight = FontWeight.Medium
                                    )
                                }


                                Spacer(
                                    Modifier.height(12.dp)
                                )


                                // =================================================
                                // ITEM DISCOUNT
                                // =================================================

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        text = "Item Discount",
                                        modifier = Modifier.weight(1f),
                                        style =
                                            MaterialTheme.typography.bodyLarge
                                    )

                                    Text(
                                        text =
                                            "- Rs ${totalItemDiscount.toInt()}",
                                        color =
                                            MaterialTheme.colorScheme.error
                                    )
                                }


                                Spacer(
                                    Modifier.height(12.dp)
                                )


                                // =================================================
                                // BILL DISCOUNT
                                // =================================================

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        text = "Bill Discount",
                                        modifier = Modifier.weight(1f),
                                        style =
                                            MaterialTheme.typography.bodyLarge
                                    )

                                    Text(
                                        text =
                                            "- Rs ${overallDiscount.toInt()}",
                                        color =
                                            MaterialTheme.colorScheme.error
                                    )
                                }


                                Spacer(
                                    Modifier.height(12.dp)
                                )


                                HorizontalDivider()


                                Spacer(
                                    Modifier.height(12.dp)
                                )


                                // =================================================
                                // TOTAL DISCOUNT
                                // =================================================

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        text = "Total Discount",
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text =
                                            "- Rs ${totalDiscount.toInt()}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }


                                Spacer(
                                    Modifier.height(22.dp)
                                )


                                // =================================================
                                // AMOUNT TO PAY
                                // =================================================

                                Card(
                                    modifier = Modifier.fillMaxWidth(),

                                    shape =
                                        RoundedCornerShape(16.dp),

                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primaryContainer
                                        )
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp)
                                    ) {

                                        Text(
                                            text = "Total: ",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .labelMedium,
                                            fontWeight =
                                                FontWeight.Bold,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onPrimaryContainer
                                        )

                                        Spacer(
                                            Modifier.height(4.dp)
                                        )

                                        Text(
                                            text =
                                                "Rs ${finalBillAmount.toInt()}",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .headlineMedium,
                                            fontWeight =
                                                FontWeight.Bold,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }


                        // =================================================
                        // RIGHT — PAYMENT & RECEIPT
                        // =================================================

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),

                            shape = RoundedCornerShape(18.dp),

                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            ),

                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp)
                            ) {

                                Text(
                                    text = "Payment",
                                    style =
                                        MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(18.dp))

                                // =================================================
                                // RECEIPT
                                // =================================================

                                Text(
                                    text = "Receipt",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(Modifier.height(8.dp))

                                // =================================================
                                // PRINT + DETAILED
                                // =================================================

                                Row(
                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    // -------------------------------
                                    // PRINT RECEIPT
                                    // -------------------------------

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                printBill = !printBill

                                                // If printing is disabled,
                                                // detailed must also be disabled.
                                                if (!printBill) {
                                                    printDetailedBill = false
                                                    printUrdu = false
                                                }
                                            },

                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Checkbox(
                                            checked = printBill,
                                            onCheckedChange = {

                                                printBill = it

                                                if (!it) {
                                                    printDetailedBill = false
                                                    printUrdu = false
                                                }
                                            }
                                        )

                                        Spacer(
                                            Modifier.width(4.dp)
                                        )

                                        Column {

                                            Text(
                                                text = "Print Receipt",
                                                fontWeight =
                                                    FontWeight.Medium
                                            )

                                            Text(
                                                text = "Print after sale",
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall,
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            )
                                        }
                                    }

                                    // -------------------------------
                                    // DETAILED
                                    // -------------------------------

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                enabled = printBill
                                            ) {
                                                printDetailedBill =
                                                    !printDetailedBill

                                                if (!printDetailedBill) {
                                                    printUrdu = false
                                                }
                                            },

                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Checkbox(
                                            checked = printDetailedBill,
                                            enabled = printBill,

                                            onCheckedChange = {

                                                printDetailedBill = it

                                                if (!it) {
                                                    printUrdu = false
                                                }
                                            }
                                        )

                                        Spacer(
                                            Modifier.width(4.dp)
                                        )

                                        Column {

                                            Text(
                                                text = "Detailed",
                                                fontWeight =
                                                    FontWeight.Medium,

                                                color =
                                                    if (printBill) {
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurface
                                                    } else {
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurface
                                                            .copy(
                                                                alpha = 0.4f
                                                            )
                                                    }
                                            )

                                            Text(
                                                text = "Show products",
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall,

                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                                        .copy(
                                                            alpha =
                                                                if (printBill)
                                                                    1f
                                                                else
                                                                    0.4f
                                                        )
                                            )
                                        }
                                    }
                                }

                                // =================================================
                                // LANGUAGE
                                // =================================================

                                if (printBill && printDetailedBill) {

                                    Spacer(
                                        Modifier.height(10.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),

                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = "Language:",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodyMedium,
                                            fontWeight =
                                                FontWeight.Medium
                                        )

                                        Spacer(
                                            Modifier.width(10.dp)
                                        )

                                        Row(
                                            verticalAlignment =
                                                Alignment.CenterVertically,

                                            modifier = Modifier.clickable {
                                                printUrdu = false
                                            }
                                        ) {

                                            RadioButton(
                                                selected = !printUrdu,

                                                onClick = {
                                                    printUrdu = false
                                                }
                                            )

                                            Text("English")
                                        }

                                        Spacer(
                                            Modifier.width(8.dp)
                                        )

                                        Row(
                                            verticalAlignment =
                                                Alignment.CenterVertically,

                                            modifier = Modifier.clickable {
                                                printUrdu = true
                                            }
                                        ) {

                                            RadioButton(
                                                selected = printUrdu,

                                                onClick = {
                                                    printUrdu = true
                                                }
                                            )

                                            Text("Urdu")
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                HorizontalDivider()

                                Spacer(Modifier.height(16.dp))

                                // =================================================
                                // BILL DISCOUNT
                                // =================================================

                                Text(
                                    text = "Bill Discount",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value =
                                        if (overallDiscount == 0.0) {
                                            ""
                                        } else {
                                            overallDiscount.toString()
                                        },

                                    onValueChange = {

                                        overallDiscount =
                                            it.toDoubleOrNull()
                                                ?.coerceAtLeast(0.0)
                                                ?.coerceAtMost(subTotal)
                                                ?: 0.0
                                    },

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    singleLine = true,

                                    label = {
                                        Text("Discount Amount")
                                    },

                                    prefix = {
                                        Text("Rs ")
                                    },

                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType =
                                                KeyboardType.Decimal
                                        )
                                )

                                Spacer(Modifier.height(16.dp))

                                // =================================================
                                // AMOUNT RECEIVED
                                // =================================================

                                Text(
                                    text = "Amount Received",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = amountReceivedText,

                                    onValueChange = {

                                        amountReceivedText =
                                            it.filter { char ->
                                                char.isDigit() ||
                                                        char == '.'
                                            }
                                    },

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    singleLine = true,

                                    label = {
                                        Text("Cash Received")
                                    },

                                    prefix = {
                                        Text("Rs ")
                                    },

                                    // =================================================
                                    // BILL TOTAL AS HINT
                                    // =================================================

                                    placeholder = {

                                        Text(
                                            text =
                                                "Rs ${finalBillAmount.toInt()}",
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                        )
                                    },

                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType =
                                                KeyboardType.Decimal
                                        )
                                )

                                // =================================================
                                // CHANGE / REMAINING
                                // =================================================

                                if (effectiveAmountReceived >= finalBillAmount) {

                                    Spacer(
                                        Modifier.height(12.dp)
                                    )

                                    Card(
                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        shape =
                                            RoundedCornerShape(14.dp),

                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primaryContainer
                                            )
                                    ) {

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 16.dp,
                                                    vertical = 12.dp
                                                ),

                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {

                                            Text(
                                                text = "Change:",

                                                modifier =
                                                    Modifier.weight(1f),

                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodyMedium,

                                                fontWeight =
                                                    FontWeight.SemiBold
                                            )

                                            Text(
                                                text =
                                                    "Rs ${changeAmount.toInt()}",

                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .titleMedium,

                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }
                                    }

                                } else if (
                                    effectiveAmountReceived > 0
                                ) {

                                    Spacer(
                                        Modifier.height(10.dp)
                                    )

                                    Text(
                                        text =
                                            "Remaining: Rs ${
                                                remainingAmount.toInt()
                                            }",

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .error,

                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,

                                        fontWeight =
                                            FontWeight.Medium
                                    )
                                }

                                Spacer(
                                    Modifier.height(22.dp)
                                )

                                // =========================================================
                                // FOOTER BUTTONS
                                // =========================================================

                                Row(
                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.End,

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    // =================================================
                                    // CANCEL
                                    // =================================================

                                    TextButton(
                                        onClick = {
                                            showCheckoutDialog = false
                                        }
                                    ) {

                                        Text(
                                            text = "Cancel"
                                        )
                                    }


                                    Spacer(
                                        Modifier.width(12.dp)
                                    )


                                    // =================================================
                                    // COMPLETE SALE
                                    // =================================================

                                    Button(
                                        enabled = isPaymentValid,

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
                                                    // CREATE BILL
                                                    // ==========================

                                                    val billId =
                                                        salesDao.insertBill(
                                                            SaleBillEntity(
                                                                totalAmount =
                                                                    finalBillAmount,

                                                                totalProfit =
                                                                    totalProfit,

                                                                overallDiscount =
                                                                    overallDiscount,

                                                                paymentType =
                                                                    PaymentType.CASH
                                                            )
                                                        )


                                                    // ==========================
                                                    // SAVE ITEMS
                                                    // ==========================

                                                    salesDao.insertItems(
                                                        cart.map {

                                                            SaleItemEntity(
                                                                billId = billId,

                                                                productId =
                                                                    it.product.id,

                                                                productName =
                                                                    it.product.productName,

                                                                purchasePrice =
                                                                    it.product.purchasePrice,

                                                                wholesalePrice =
                                                                    it.product.wholesalePrice,

                                                                quantity =
                                                                    it.quantity,

                                                                discount =
                                                                    it.discount
                                                            )
                                                        }
                                                    )


                                                    // ==========================
                                                    // REDUCE STOCK
                                                    // ==========================

                                                    cart.forEach {

                                                        productDao.update(
                                                            it.product.copy(
                                                                quantity =
                                                                    it.product.quantity -
                                                                            it.quantity
                                                            )
                                                        )
                                                    }


                                                    // ==========================
                                                    // PRINT
                                                    // ==========================

                                                    val billItems =
                                                        cart.toList()

                                                    if (printBill) {

                                                        if (
                                                            BluetoothPermissionHelper
                                                                .hasBluetoothPermissions(
                                                                    context
                                                                )
                                                        ) {

                                                            ThermalPrinterHelper.printBill(
                                                                context = context,

                                                                billId = billId,

                                                                billDate =
                                                                    System.currentTimeMillis(),

                                                                cart = billItems,

                                                                overallDiscount =
                                                                    overallDiscount,

                                                                finalTotal =
                                                                    finalBillAmount,

                                                                isDetailedBill =
                                                                    printDetailedBill,

                                                                isUrdu =
                                                                    printUrdu
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
                                                    // RESET
                                                    // ==========================

                                                    cart.clear()

                                                    selectedCustomer = null

                                                    overallDiscount = 0.0

                                                    showOverallDiscount = false

                                                    showCheckoutDialog = false
                                                }
                                            }
                                        },

                                        shape =
                                            RoundedCornerShape(12.dp)
                                    )
                                    {

                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )

                                        Spacer(
                                            Modifier.width(8.dp)
                                        )

                                        Text(
                                            text = "Complete Sale"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}