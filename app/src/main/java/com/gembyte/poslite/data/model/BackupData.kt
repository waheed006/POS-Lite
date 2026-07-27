package com.gembyte.poslite.data.model

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.gembyte.poslite.data.local.db.DatabaseProvider
import com.gembyte.poslite.data.local.entity.InventoryUpdateEntity
import com.gembyte.poslite.data.local.entity.ProductEntity
import com.gembyte.poslite.data.local.entity.SaleBillEntity
import com.gembyte.poslite.data.local.entity.SaleItemEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupData(
    val backupDate: Long,
    val products: List<ProductEntity>,
    val bills: List<SaleBillEntity>,
    val saleItems: List<SaleItemEntity>,
    val inventoryUpdates: List<InventoryUpdateEntity>
)

suspend fun exportBackup(
    context: Context
): File {

    val db = DatabaseProvider.getDatabase(context)

    val backup = BackupData(
        backupDate = System.currentTimeMillis(),
        products = db.productDao().getAllProducts(),
        bills = db.salesDao().getAllBills(),
        saleItems = db.salesDao().getAllSaleItems(),
        inventoryUpdates = db.inventoryUpdateDao().getAll()
    )

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val date =
        SimpleDateFormat(
            "dd_MM_yy_hhmm_a",
            Locale.getDefault()
        ).format(Date())

    val file = File(
        context.getExternalFilesDir(null),
        "pos_lite_backup_$date.json"
    )

    file.writeText(json.encodeToString(backup))
    return file
}

private val json = Json {
    ignoreUnknownKeys = true
}

suspend fun importBackup(
    context: Context,
    uri: Uri
) {

    val db = DatabaseProvider.getDatabase(context)

    val jsonText =
        context.contentResolver
            .openInputStream(uri)
            ?.bufferedReader()
            ?.use {
                it.readText()
            }
            ?: return

    val backup =
        json.decodeFromString<BackupData>(
            jsonText
        )

    db.withTransaction {

        db.inventoryUpdateDao()
            .deleteAll()

        db.salesDao()
            .deleteAllItems()

        db.salesDao()
            .deleteAllBills()

        db.productDao()
            .deleteAllProducts()

        backup.products.forEach {

            db.productDao()
                .insert(it)
        }

        backup.bills.forEach {

            db.salesDao()
                .insertBill(it)
        }

        db.salesDao()
            .insertItems(
                backup.saleItems
            )

        db.inventoryUpdateDao()
            .insertAll(
                backup.inventoryUpdates
            )
    }
}