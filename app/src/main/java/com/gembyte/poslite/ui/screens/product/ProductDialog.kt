package com.gembyte.poslite.ui.screens.product

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.CompanyEntity
import com.gembyte.poslite.data.local.entity.ProductEntity
import com.gembyte.poslite.data.model.WeightUnit
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    product: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {

    val context = LocalContext.current

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    val companyDao = db.companyDao()

    val companies by companyDao
        .getCompanies()
        .collectAsState(initial = emptyList())

    /*
     * ---------------------------------------------------
     * Company
     * ---------------------------------------------------
     */

    var companyExpanded by remember {
        mutableStateOf(false)
    }

    var selectedCompany by remember {
        mutableStateOf<CompanyEntity?>(null)
    }

    /*
     * For EDIT:
     *
     * Product already has companyId.
     * Find that company and select it automatically.
     *
     * For NEW:
     * Select first available company by default.
     */
    LaunchedEffect(companies, product?.companyId) {

        if (companies.isNotEmpty()) {

            selectedCompany =
                if (product != null) {

                    companies.firstOrNull {
                        it.id == product.companyId
                    }

                } else {

                    selectedCompany
                        ?: companies.first()
                }
        }
    }

    /*
     * ---------------------------------------------------
     * Product fields
     * ---------------------------------------------------
     */

    var name by remember {
        mutableStateOf(
            product?.productName ?: ""
        )
    }

    var urduName by remember {
        mutableStateOf(product?.urduName ?: "")
    }

    var barcode by remember {
        mutableStateOf(
            product?.barcode ?: ""
        )
    }

    var purchasePrice by remember {
        mutableStateOf(
            product?.purchasePrice?.toString() ?: ""
        )
    }

    var wholesalePrice by remember {
        mutableStateOf(
            product?.wholesalePrice?.toString() ?: ""
        )
    }

    var retailPrice by remember {
        mutableStateOf(
            product?.retailPrice?.toString() ?: ""
        )
    }

    var quantity by remember {
        mutableStateOf(
            product?.quantity?.toString() ?: ""
        )
    }

    var selectedUnit by remember {
        mutableStateOf(
            product?.weightUnit ?: WeightUnit.BOX
        )
    }

    var unitExpanded by remember {
        mutableStateOf(false)
    }

    var imageUri by remember {
        mutableStateOf(
            product?.productImage
        )
    }

    /*
     * ---------------------------------------------------
     * Image picker
     * ---------------------------------------------------
     */

    var showImageSourceDialog by remember {
        mutableStateOf(false)
    }

    val cropLauncher =
        rememberLauncherForActivityResult(
            CropImageContract()
        ) { result ->

            if (result.isSuccessful) {

                imageUri =
                    result.uriContent?.toString()
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {

                cropLauncher.launch(
                    CropImageContractOptions(
                        uri = it,
                        cropImageOptions =
                            CropImageOptions(
                                guidelines =
                                    CropImageView.Guidelines.ON,
                                fixAspectRatio = false,
                                allowRotation = true,
                                allowFlipping = true,
                                cropShape =
                                    CropImageView.CropShape.RECTANGLE
                            )
                    )
                )
            }
        }

    var cameraImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                cameraImageUri?.let {

                    cropLauncher.launch(
                        CropImageContractOptions(
                            uri = it,
                            cropImageOptions =
                                CropImageOptions(
                                    guidelines =
                                        CropImageView.Guidelines.ON,
                                    fixAspectRatio = false,
                                    allowRotation = true,
                                    allowFlipping = true,
                                    cropShape =
                                        CropImageView.CropShape.RECTANGLE
                                )
                        )
                    )
                }
            }
        }

    /*
     * ---------------------------------------------------
     * Dialog
     * ---------------------------------------------------
     */

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {

            Text(
                text =
                    if (product == null)
                        "Add Product"
                    else
                        "Edit Product"
            )
        },

        text = {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                /*
                 * =========================================
                 * LEFT SIDE
                 * =========================================
                 */

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    /*
                     * IMAGE
                     */

                    Card(
                        modifier = Modifier
                            .size(150.dp)
                            .clickable {
                                showImageSourceDialog = true
                            }
                    ) {

                        if (!imageUri.isNullOrEmpty()) {

                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                        } else {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = "Set Image",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    /*
                     * PRODUCT NAME
                     */

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Product Name")
                        },
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = urduName,
                        onValueChange = {
                            urduName = it
                        },
                        label = {
                            Text("Urdu Name")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    /*
                     * BARCODE
                     */

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {
                            barcode = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Barcode")
                        },
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    /*
                     * PURCHASE PRICE
                     */

                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = {
                            purchasePrice = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Purchase Price")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        singleLine = true
                    )
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                /*
                 * =========================================
                 * RIGHT SIDE
                 * =========================================
                 */

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    /*
                     * WHOLESALE PRICE
                     */

                    OutlinedTextField(
                        value = wholesalePrice,
                        onValueChange = {
                            wholesalePrice = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Wholesale Price")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    /*
                     * RETAIL PRICE
                     */

                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = {
                            retailPrice = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Retail Price")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    /*
                     * QUANTITY
                     */

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {
                            quantity = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Quantity")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    /*
                     * COMPANY DROPDOWN
                     *
                     * This replaces Discount.
                     */

                    ExposedDropdownMenuBox(

                        expanded = companyExpanded,

                        onExpandedChange = {

                            companyExpanded =
                                !companyExpanded
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value =
                                selectedCompany?.name
                                    ?: "Select Company",

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
                                    .menuAnchor(
                                        MenuAnchorType.PrimaryNotEditable
                                    )
                                    .fillMaxWidth(),

                            isError =
                                companies.isEmpty()
                        )

                        ExposedDropdownMenu(

                            expanded = companyExpanded,

                            onDismissRequest = {
                                companyExpanded = false
                            }
                        ) {

                            if (companies.isEmpty()) {

                                DropdownMenuItem(

                                    text = {
                                        Text(
                                            "No companies available"
                                        )
                                    },

                                    onClick = {
                                        companyExpanded = false
                                    }
                                )

                            } else {

                                companies.forEach { company ->

                                    DropdownMenuItem(

                                        text = {
                                            Text(
                                                company.name
                                            )
                                        },

                                        onClick = {

                                            selectedCompany =
                                                company

                                            companyExpanded =
                                                false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    /*
                     * UNIT
                     */

                    ExposedDropdownMenuBox(

                        expanded = unitExpanded,

                        onExpandedChange = {

                            unitExpanded =
                                !unitExpanded
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value =
                                selectedUnit.name,

                            onValueChange = {},

                            readOnly = true,

                            label = {
                                Text("Unit")
                            },

                            trailingIcon = {

                                ExposedDropdownMenuDefaults
                                    .TrailingIcon(
                                        expanded =
                                            unitExpanded
                                    )
                            },

                            modifier =
                                Modifier
                                    .menuAnchor(
                                        MenuAnchorType.PrimaryNotEditable
                                    )
                                    .fillMaxWidth()
                        )

                        ExposedDropdownMenu(

                            expanded = unitExpanded,

                            onDismissRequest = {
                                unitExpanded = false
                            }
                        ) {

                            WeightUnit.entries.forEach { unit ->

                                DropdownMenuItem(

                                    text = {
                                        Text(
                                            unit.name
                                        )
                                    },

                                    onClick = {

                                        selectedUnit =
                                            unit

                                        unitExpanded =
                                            false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },

        /*
         * =========================================
         * SAVE
         * =========================================
         */

        confirmButton = {

            TextButton(

                enabled =
                    name.isNotBlank() &&
                            selectedCompany != null,

                onClick = {

                    val company =
                        selectedCompany
                            ?: return@TextButton

                    val item =
                        ProductEntity(
                            id = product?.id ?: 0,
                            companyId = company.id,
                            productName = name.trim(),
                            urduName = urduName.trim(),
                            barcode = barcode.trim(),
                            purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                            wholesalePrice = wholesalePrice.toDoubleOrNull() ?: 0.0,
                            retailPrice = retailPrice.toDoubleOrNull() ?: 0.0,
                            weightUnit = selectedUnit,
                            quantity = quantity.toIntOrNull() ?: 0,
                            discount = 0.0,
                            addDate = product?.addDate ?: System.currentTimeMillis(),
                            productImage = imageUri
                        )

                    onSave(item)
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

    /*
     * =========================================
     * IMAGE SOURCE DIALOG
     * =========================================
     */

    if (showImageSourceDialog) {

        AlertDialog(

            onDismissRequest = {
                showImageSourceDialog = false
            },

            title = {
                Text("Select Image Source")
            },

            text = {

                Column {

                    Button(

                        modifier =
                            Modifier.fillMaxWidth(),

                        onClick = {

                            showImageSourceDialog =
                                false

                            cameraImageUri =
                                createImageUri(context)

                            cameraLauncher.launch(
                                cameraImageUri!!
                            )
                        }
                    ) {

                        Text("📷 Camera")
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Button(

                        modifier =
                            Modifier.fillMaxWidth(),

                        onClick = {

                            showImageSourceDialog =
                                false

                            galleryLauncher.launch(
                                "image/*"
                            )
                        }
                    ) {

                        Text("🖼 Gallery")
                    }
                }
            },

            confirmButton = {}
        )
    }
}

fun createImageUri(
    context: Context
): Uri {
    val imageFile = File(
        context.cacheDir,
        "product_${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}