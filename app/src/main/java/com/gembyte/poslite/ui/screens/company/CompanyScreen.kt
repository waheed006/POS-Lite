package com.gembyte.poslite.ui.screens.company

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.CompanyEntity
import com.gembyte.poslite.ui.screens.product.createImageUri
import com.gembyte.poslite.ui.theme.blueLight
import kotlinx.coroutines.launch

@Composable
fun CompanyScreen(
    onBackPressed: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember {
        DatabaseProvider.getDatabase(context)
    }

    val companyDao = db.companyDao()

    val companies by companyDao
        .getCompanies()
        .collectAsState(initial = emptyList())

    var search by rememberSaveable {
        mutableStateOf("")
    }

    var showCompanyDialog by remember {
        mutableStateOf(false)
    }

    var editCompany by remember {
        mutableStateOf<CompanyEntity?>(null)
    }

    var deleteCompany by remember {
        mutableStateOf<CompanyEntity?>(null)
    }

    val filteredCompanies = remember(
        companies,
        search
    ) {

        if (search.isBlank()) {

            companies

        } else {

            companies.filter {
                it.name.contains(
                    search,
                    ignoreCase = true
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBackPressed
                ) {

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "Companies",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                    },
                    modifier = Modifier.width(300.dp),
                    singleLine = true,
                    placeholder = {
                        Text("Search Company")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {

                        if (search.isNotEmpty()) {

                            IconButton(
                                onClick = {
                                    search = ""
                                }
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    }
                )
            }

            HorizontalDivider()

            // Companies Grid
            if (filteredCompanies.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = if (search.isBlank()) {
                            "No companies added"
                        } else {
                            "No company found"
                        },
                        color = Color.Gray
                    )
                }

            } else {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 100.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = filteredCompanies,
                        key = { it.id }
                    ) { company ->

                        CompanyItem(
                            company = company,

                            onEdit = {
                                editCompany = company
                            },

                            onDelete = {
                                deleteCompany = company
                            }
                        )
                    }
                }
            }
        }

        // Floating Add Button
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            onClick = {
                showCompanyDialog = true
            }
        ) {

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Company"
            )
        }
    }

    // Add Company
    if (showCompanyDialog) {

        CompanyDialog(
            company = null,

            onDismiss = {
                showCompanyDialog = false
            },

            onSave = { company ->

                scope.launch {

                    companyDao.insert(company)
                }

                showCompanyDialog = false
            }
        )
    }

    // Edit Company
    editCompany?.let { company ->

        CompanyDialog(
            company = company,

            onDismiss = {
                editCompany = null
            },

            onSave = { updatedCompany ->

                scope.launch {

                    companyDao.update(updatedCompany)
                }

                editCompany = null
            }
        )
    }

    // Delete Company
    deleteCompany?.let { company ->

        AlertDialog(

            onDismissRequest = {
                deleteCompany = null
            },

            title = {
                Text(
                    text = "Delete Company",
                    color = Color.Red
                )
            },

            text = {
                Text(
                    text = "Delete '${company.name}'?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        scope.launch {

                            companyDao.delete(company)
                        }

                        deleteCompany = null
                    }
                ) {

                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleteCompany = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CompanyItem(
    company: CompanyEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Company Logo
            if (!company.image.isNullOrEmpty()) {

                AsyncImage(
                    model = company.image,
                    contentDescription = company.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

            } else {

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            blueLight,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = company.name
                            .take(1)
                            .uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                text = company.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.Center
            ) {

                IconButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Company"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Company"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDialog(
    company: CompanyEntity?,
    onDismiss: () -> Unit,
    onSave: (CompanyEntity) -> Unit
) {

    var name by remember {
        mutableStateOf(company?.name ?: "")
    }

    var description by remember {
        mutableStateOf(company?.description ?: "")
    }

    var imageUri by remember {
        mutableStateOf(company?.image)
    }

    val context = LocalContext.current

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
                        cropImageOptions = CropImageOptions(
                            guidelines = CropImageView.Guidelines.ON,
                            cropShape = CropImageView.CropShape.RECTANGLE
                        )
                    )
                )

            }

        }

    var cameraUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                cameraUri?.let {

                    cropLauncher.launch(
                        CropImageContractOptions(
                            uri = it,
                            cropImageOptions = CropImageOptions(
                                guidelines = CropImageView.Guidelines.ON
                            )
                        )
                    )

                }

            }

        }

    var showSourceDialog by remember {
        mutableStateOf(false)
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {

            Text(

                if (company == null)
                    "Add Company"
                else
                    "Edit Company"

            )

        },

        text = {
            Row {
                Card(
                    modifier = Modifier
                        .size(160.dp)
                        .clickable {
                            showSourceDialog = true
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
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                "Select Logo",
                                color = MaterialTheme.colorScheme.primary
                            )

                        }

                    }

                }

                Spacer(
                    Modifier.width(20.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Company Name")
                        }

                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Description")
                        },
                        minLines = 4
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        CompanyEntity(
                            id = company?.id ?: 0,
                            name = name.trim(),
                            description = description.trim(),
                            image = imageUri
                        )
                    )
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

    if (showSourceDialog) {

        AlertDialog(

            onDismissRequest = {

                showSourceDialog = false

            },

            title = {

                Text("Select Image")

            },

            text = {
                Column {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showSourceDialog = false
                            cameraUri = createImageUri(context)
                            cameraLauncher.launch(
                                cameraUri!!
                            )
                        }

                    ) {
                        Text("📷 Camera")
                    }

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showSourceDialog = false
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