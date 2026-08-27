package com.gembyte.poslite.ui.screens.product.editProductDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gembyte.poslite.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkProductScreen(
    viewModel: BulkProductViewModel,
    onBack: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    val companies by viewModel.companies.collectAsState()

    var companyExpanded by remember {
        mutableStateOf(false)
    }

    val filteredProducts =
        remember(
            uiState.products,
            uiState.searchQuery,
            uiState.selectedCompanyId,
            uiState.showIncompleteOnly
        ) {

            uiState.products.filter { product ->

                // ------------------------------------
                // SEARCH
                // ------------------------------------

                val query =
                    uiState.searchQuery.trim()

                val matchesSearch =
                    query.isBlank() ||
                            product.productName
                                .contains(
                                    query,
                                    ignoreCase = true
                                ) ||
                            product.urduName
                                .contains(
                                    query,
                                    ignoreCase = true
                                ) ||
                            product.barcode
                                .contains(
                                    query,
                                    ignoreCase = true
                                )

                // ------------------------------------
                // COMPANY
                // ------------------------------------

                val matchesCompany =
                    uiState.selectedCompanyId == null ||
                            product.companyId ==
                            uiState.selectedCompanyId

                // ------------------------------------
                // INCOMPLETE
                // ------------------------------------

                val isIncomplete =
                    product.urduName.isBlank() ||
                            product.barcode.isBlank() ||
                            product.purchasePrice
                                .toDoubleOrNull()
                                ?.let { it <= 0 }
                            ?: true ||
                            product.wholesalePrice
                                .toDoubleOrNull()
                                ?.let { it <= 0 }
                            ?: true

                val matchesIncomplete =
                    !uiState.showIncompleteOnly ||
                            isIncomplete

                matchesSearch &&
                        matchesCompany &&
                        matchesIncomplete
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Product Editor") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {

                    // --------------------------------
                    // INCOMPLETE FILTER
                    // --------------------------------

                    IconButton(
                        onClick = {
                            viewModel.toggleIncompleteFilter()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.FilterAlt,

                            contentDescription =
                                "Show incomplete products",

                            tint =
                                if (
                                    uiState.showIncompleteOnly
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                } else {
                                    LocalContentColor.current
                                }
                        )
                    }
                }
            )
        },

        bottomBar = {
            Surface(
                tonalElevation = 4.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        )
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "${filteredProducts.size} products",

                        modifier =
                            Modifier.weight(1f),

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )

                    Button(
                        onClick = {
                            // Step 2
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Save,
                            contentDescription =
                                null
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text("Save Changes")
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ====================================================
            // SEARCH BAR
            // ====================================================

            OutlinedTextField(

                value =
                    uiState.searchQuery,

                onValueChange = {
                    viewModel.updateSearchQuery(it)
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true,

                label = {
                    Text("Search Product")
                },

                placeholder = {
                    Text(
                        "Name, Urdu name or barcode"
                    )
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,
                        contentDescription =
                            null
                    )
                },

                trailingIcon = {

                    if (
                        uiState.searchQuery.isNotEmpty()
                    ) {

                        IconButton(
                            onClick = {
                                viewModel.updateSearchQuery("")
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Clear,
                                contentDescription =
                                    "Clear search"
                            )
                        }
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ====================================================
            // COMPANY + IMPORT EXPORT
            // ====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                ExposedDropdownMenuBox(

                    expanded =
                        companyExpanded,

                    onExpandedChange = {
                        companyExpanded =
                            !companyExpanded
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    val selectedCompany =
                        companies.firstOrNull {
                            it.id ==
                                    uiState.selectedCompanyId
                        }

                    OutlinedTextField(

                        value =
                            selectedCompany?.name
                                ?: "All Companies",

                        onValueChange = {},

                        readOnly = true,

                        label = {
                            Text("Company")
                        },

                        trailingIcon = {

                            ExposedDropdownMenuDefaults
                                .TrailingIcon(
                                    expanded =
                                        companyExpanded
                                )
                        },

                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                    )

                    ExposedDropdownMenu(

                        expanded =
                            companyExpanded,

                        onDismissRequest = {
                            companyExpanded = false
                        }
                    ) {

                        DropdownMenuItem(

                            text = {
                                Text("All Companies")
                            },

                            onClick = {

                                viewModel
                                    .selectCompany(null)

                                companyExpanded = false
                            }
                        )

                        companies.forEach { company ->

                            DropdownMenuItem(

                                text = {
                                    Text(company.name)
                                },

                                onClick = {

                                    viewModel
                                        .selectCompany(
                                            company.id
                                        )

                                    companyExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                // Import placeholder

                OutlinedButton(
                    onClick = {
                        // Step 4
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.FileDownload,
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.width(4.dp)
                    )

                    Text("Import")
                }

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                // Export placeholder

                OutlinedButton(
                    onClick = {
                        // Step 3
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.FileUpload,
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.width(4.dp)
                    )

                    Text("Export")
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // ====================================================
            // TABLE HEADER
            // ====================================================

            BulkProductTableHeader()

            HorizontalDivider()

            // ====================================================
            // PRODUCTS
            // ====================================================

            if (uiState.isLoading) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }

            } else if (filteredProducts.isEmpty()) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Inventory2,
                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(48.dp)
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            if (
                                uiState.showIncompleteOnly
                            ) {
                                "No incomplete products found"
                            } else {
                                "No products found"
                            }
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    items(
                        items = filteredProducts,
                        key = {
                            it.id
                        }
                    ) { product ->

                        BulkProductRow(
                            product = product,
                            companies = companies,
                            onProductChange = { updatedProduct ->
                                viewModel.updateProduct(
                                    productId = product.id
                                ) {
                                    updatedProduct
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkProductTableHeader() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(
                horizontal = 8.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "Product Name",
            modifier = Modifier.weight(2.0f),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Urdu Name",
            modifier = Modifier.weight(2.0f),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Purchase",
            modifier = Modifier.weight(0.9f),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Wholesale",
            modifier = Modifier.weight(0.9f),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Barcode",
            modifier = Modifier.weight(1.5f),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Company",
            modifier = Modifier.weight(1.5f),
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkProductRow(
    product: EditableProduct,
    companies: List<CompanyEntity>,
    onProductChange: (EditableProduct) -> Unit
) {

    var companyExpanded by remember {
        mutableStateOf(false)
    }

    val selectedCompany =
        companies.firstOrNull {
            it.id == product.companyId
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ====================================================
        // PRODUCT NAME
        // ====================================================

        OutlinedTextField(
            value = product.productName,

            onValueChange = {
                onProductChange(
                    product.copy(
                        productName = it
                    )
                )
            },

            modifier = Modifier
                .weight(2.0f)
                .padding(end = 4.dp),

            singleLine = true,

            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            )
        )

        // ====================================================
        // URDU NAME
        // ====================================================

        OutlinedTextField(
            value = product.urduName,

            onValueChange = {
                onProductChange(
                    product.copy(
                        urduName = it
                    )
                )
            },

            modifier = Modifier
                .weight(2.0f)
                .padding(horizontal = 2.dp),

            singleLine = true,

            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            )
        )

        // ====================================================
        // PURCHASE
        // ====================================================

        OutlinedTextField(
            value = product.purchasePrice,

            onValueChange = { value ->

                if (
                    value.isEmpty() ||
                    value.matches(
                        Regex("""\d*\.?\d*""")
                    )
                ) {

                    onProductChange(
                        product.copy(
                            purchasePrice = value
                        )
                    )
                }
            },

            modifier = Modifier
                .weight(0.9f)
                .padding(horizontal = 2.dp),

            singleLine = true,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),

            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            )
        )

        // ====================================================
        // WHOLESALE
        // ====================================================

        OutlinedTextField(
            value = product.wholesalePrice,

            onValueChange = { value ->

                if (
                    value.isEmpty() ||
                    value.matches(
                        Regex("""\d*\.?\d*""")
                    )
                ) {

                    onProductChange(
                        product.copy(
                            wholesalePrice = value
                        )
                    )
                }
            },

            modifier = Modifier
                .weight(0.9f)
                .padding(horizontal = 2.dp),

            singleLine = true,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),

            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            )
        )

        // ====================================================
        // BARCODE
        // ====================================================

        OutlinedTextField(
            value = product.barcode,

            onValueChange = {
                onProductChange(
                    product.copy(
                        barcode = it
                    )
                )
            },

            modifier = Modifier
                .weight(1.5f)
                .padding(horizontal = 2.dp),

            singleLine = true,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),

            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            )
        )

        // ====================================================
        // COMPANY
        // ====================================================

        ExposedDropdownMenuBox(
            expanded = companyExpanded,

            onExpandedChange = {
                companyExpanded = !companyExpanded
            },

            modifier = Modifier
                .weight(1.5f)
                .padding(start = 4.dp)
        ) {

            OutlinedTextField(
                value =
                    selectedCompany?.name
                        ?: "Select Company",

                onValueChange = {},

                readOnly = true,

                singleLine = true,

                label = {
                    Text("Company")
                },

                trailingIcon = {
                    ExposedDropdownMenuDefaults
                        .TrailingIcon(
                            expanded = companyExpanded
                        )
                },

                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),

                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp
                )
            )

            ExposedDropdownMenu(
                expanded = companyExpanded,

                onDismissRequest = {
                    companyExpanded = false
                }
            ) {

                companies.forEach { company ->

                    DropdownMenuItem(
                        text = {
                            Text(company.name)
                        },

                        onClick = {

                            onProductChange(
                                product.copy(
                                    companyId = company.id
                                )
                            )

                            companyExpanded = false
                        }
                    )
                }
            }
        }
    }
}