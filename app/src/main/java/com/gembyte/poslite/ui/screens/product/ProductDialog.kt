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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
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

    var name by remember {
        mutableStateOf(product?.productName ?: "")
    }


    var imageUri by remember {
        mutableStateOf(product?.productImage)
    }

    val cropLauncher =
        rememberLauncherForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                imageUri = result.uriContent?.toString()
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
                        cropImageOptions = CropImageOptions(
                            guidelines = CropImageView.Guidelines.ON,
                            fixAspectRatio = false,
                            allowRotation = true,
                            allowFlipping = true,
                            cropShape = CropImageView.CropShape.RECTANGLE
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
                            cropImageOptions = CropImageOptions(
                                guidelines = CropImageView.Guidelines.ON,
                                fixAspectRatio = false,
                                allowRotation = true,
                                allowFlipping = true,
                                cropShape = CropImageView.CropShape.RECTANGLE
                            )
                        )
                    )
                }
            }
        }

    var showImageSourceDialog by remember {
        mutableStateOf(false)
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

    var discount by remember {
        mutableStateOf(
            product?.discount?.toString() ?: "0"
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

    var barcode by remember {
        mutableStateOf(
            product?.barcode ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (product == null)
                    "Add Product"
                else
                    "Edit Product"
            )
        },

        text = {
            Row(modifier = Modifier.fillMaxWidth()) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Card(modifier = Modifier.size(150.dp)) {

                        if (!imageUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        showImageSourceDialog = true
                                    }
                            )

                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Set Image",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                            showImageSourceDialog = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        label = {
                            Text("Product Name")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {
                            barcode = it
                        },
                        label = {
                            Text("Barcode")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = {
                            purchasePrice = it
                        },
                        label = {
                            Text("Purchase Price")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = wholesalePrice,
                        onValueChange = {
                            wholesalePrice = it
                        },
                        label = {
                            Text("Wholesale Price")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = {
                            retailPrice = it
                        },
                        label = {
                            Text("Retail Price")
                        },
                        keyboardOptions = KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {
                            quantity = it
                        },
                        label = {
                            Text("Quantity")
                        },
                        keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = discount,
                        onValueChange = {
                            discount = it
                        },
                        label = {
                            Text("Discount")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = {
                            unitExpanded = !unitExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value = selectedUnit.name,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Unit")
                            },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = {
                                unitExpanded = false
                            }
                        ) {

                            WeightUnit.entries.forEach {

                                DropdownMenuItem(
                                    text = {
                                        Text(it.name)
                                    },
                                    onClick = {

                                        selectedUnit = it
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = {

                    val item = ProductEntity(
                        id = product?.id ?: 0,
                        productName = name,
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                        wholesalePrice = wholesalePrice.toDoubleOrNull() ?: 0.0,
                        retailPrice = retailPrice.toDoubleOrNull() ?: 0.0,
                        quantity = quantity.toIntOrNull() ?: 0,
                        discount = discount.toDoubleOrNull() ?: 0.0,
                        weightUnit = selectedUnit,
                        addDate = System.currentTimeMillis(),
                        productImage = imageUri,
                        barcode = barcode,
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

    if (showImageSourceDialog) {

        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = {
                showImageSourceDialog = false
            },
            title = { Text("Select Image Source") },
            text = {

                Column {

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showImageSourceDialog = false
                            cameraImageUri = createImageUri(context)
                            cameraLauncher.launch(cameraImageUri!!)
                        }
                    ) {
                        Text("📷 Camera")

                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
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