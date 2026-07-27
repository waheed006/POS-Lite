package com.gembyte.poslite.ui.screens.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.ProductEntity
import com.gembyte.poslite.data.model.ProductFilter
import com.gembyte.poslite.ui.theme.blueLight
import kotlinx.coroutines.launch

@Composable
fun ProductScreen(onBackPressed: () -> Unit) {

    val context = LocalContext.current

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    val dao = db.productDao()

    val products by dao.getProducts().collectAsState(initial = emptyList())

    var stockProduct by remember {
        mutableStateOf<ProductEntity?>(null)
    }

    val inventoryDao = db.inventoryUpdateDao()

    val scope = rememberCoroutineScope()

    var search by remember {
        mutableStateOf("")
    }

    var selectedFilter by remember {
        mutableStateOf(ProductFilter.ALL)
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var selectedProduct by remember {
        mutableStateOf<ProductEntity?>(null)
    }

    val filteredProducts = remember(
        products, search, selectedFilter
    ) {
        products.filter {
                it.productName.contains(search, true)
            }.filter {
                when (selectedFilter) {
                    ProductFilter.ALL -> true
                    ProductFilter.LOW_STOCK -> it.quantity in 1..5
                    ProductFilter.OUT_OF_STOCK -> it.quantity == 0
                }
            }
    }

    val totalInventoryValue = remember(products) {
        products.sumOf {
            it.purchasePrice * it.quantity
        }
    }

    var productToDelete by remember {
        mutableStateOf<ProductEntity?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
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
                    }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Products",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                var filterExpanded by remember {
                    mutableStateOf(false)
                }

                Box {
                    TextButton(
                        onClick = {
                            filterExpanded = true
                        }) {

                        Text(
                            text = when (selectedFilter) {
                                ProductFilter.ALL -> "All"
                                ProductFilter.LOW_STOCK -> "Low Stock"
                                ProductFilter.OUT_OF_STOCK -> "Out of Stock"
                            }
                        )

                        Icon(
                            Icons.Default.ArrowDropDown, contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = filterExpanded, onDismissRequest = {
                            filterExpanded = false
                        }) {
                        DropdownMenuItem(text = { Text("All") }, onClick = {
                            selectedFilter = ProductFilter.ALL
                            filterExpanded = false
                        })

                        DropdownMenuItem(text = { Text("Low Stock") }, onClick = {
                            selectedFilter = ProductFilter.LOW_STOCK
                            filterExpanded = false
                        })

                        DropdownMenuItem(text = { Text("Out of Stock") }, onClick = {
                            selectedFilter = ProductFilter.OUT_OF_STOCK
                            filterExpanded = false
                        })
                    }
                }
            }

            OutlinedTextField(value = search, onValueChange = {
                search = it
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp), label = {
                Text("Search Product")
            })

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5), modifier = Modifier.weight(1f)
            ) {

                items(
                    items = filteredProducts, key = { it.id }) { product ->
                    ProductCard(product = product, onEdit = {
                        selectedProduct = it
                    }, onDelete = {
                        productToDelete = it
                    }, onStock = {
                        stockProduct = it
                    })
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Inventory : Rs ${
                        String.format("%.2f", totalInventoryValue)
                    } | Total Products: ${products.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        FloatingActionButton(
            onClick = {
                showAddDialog = true
            }, modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {

            Icon(
                Icons.Default.Add, null
            )
        }
    }

    if (showAddDialog) {

        ProductDialog(product = null, onDismiss = {
            showAddDialog = false
        }, onSave = { product ->
            scope.launch {
                dao.insert(product)
            }

            showAddDialog = false
        })
    }

    selectedProduct?.let { product ->
        ProductDialog(product = product, onDismiss = {
            selectedProduct = null
        }, onSave = { updated ->
            scope.launch {
                dao.update(updated)
            }
            selectedProduct = null
        })
    }

    productToDelete?.let { product ->

        AlertDialog(onDismissRequest = {
            productToDelete = null
        }, title = {
            Text(
                text = "Delete Product",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Red
            )
        }, text = {
            Text(
                text = "Are you sure you want to delete '${product.productName}'?",
                fontSize = 16.sp,
                color = Color.Black
            )
        }, confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        dao.delete(product)
                    }
                    productToDelete = null
                }) {
                Text("Delete")
            }
        }, dismissButton = {
            TextButton(
                onClick = {
                    productToDelete = null
                }) {
                Text("Cancel")
            }
        })
    }

    stockProduct?.let {
        StockUpdateDialog(
            product = it, onDismiss = {
                stockProduct = null
            },

            onSave = { product, log ->

                scope.launch {
                    dao.update(product)
                    inventoryDao.insert(log)
                    stockProduct = null
                }
            })
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onEdit: (ProductEntity) -> Unit,
    onStock: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    val borderColor = when {
        product.quantity == 0 -> Color.Red
        product.quantity <= 5 -> Color(0xFFFF9800)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor)
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // IMAGE
            if (!product.productImage.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(product.productImage)
                        .crossfade(false).size(300) // resize
                        .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(blueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = product.productName.substringBefore(" ").take(12),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.padding(
                    top = 12.dp, start = 12.dp, end = 12.dp
                )
            ) {
                // Product Name
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stock
                Text(
                    text = when {
                        product.quantity == 0 -> "Out of Stock"
                        product.quantity <= 5 -> "Low Stock (${product.quantity})"
                        else -> "${product.quantity} Available"
                    }, color = when {
                        product.quantity == 0 -> Color.Red
                        product.quantity <= 5 -> Color(0xFFFF9800)
                        else -> Color.Gray
                    }, style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Rs. ${product.wholesalePrice.toInt()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "/ ${product.weightUnit}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    IconButton(
                        onClick = {
                            onStock(product)
                        }) {
                        Icon(
                            Icons.Default.Inventory, null
                        )
                    }

                    IconButton(
                        onClick = {
                            onEdit(product)
                        }) {
                        Icon(
                            Icons.Default.Edit, null
                        )
                    }

                    IconButton(
                        onClick = {
                            onDelete(product)
                        }) {
                        Icon(
                            Icons.Default.Delete, null
                        )
                    }
                }
            }
        }
    }
}