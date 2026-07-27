package com.gembyte.poslite.components.printer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.gembyte.poslite.data.model.BillCartItem
import java.text.SimpleDateFormat
import java.util.*

object ThermalPrinterHelper {

    fun printBill(
        context: Context,
        billId: Long,
        billDate: Long,
        cart: List<BillCartItem>,
        overallDiscount: Double,
        finalTotal: Double,
        isDetailedBill: Boolean
    ) {

        try {

            val connection = BluetoothPrintersConnections.selectFirstPaired()

            if (connection == null) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "No paired printer found",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }

            val printer =
                EscPosPrinter(
                    connection,
                    203,
                    58f,
                    32
                )

            val formatter =
                SimpleDateFormat(
                    "dd MMM yyyy hh:mm a",
                    Locale.getDefault()
                )

            val date =
                formatter.format(Date(billDate))

            val billText = if (isDetailedBill)
                buildDetailReceipt(
                    billId,
                    date,
                    cart,
                    overallDiscount,
                    finalTotal
                )
            else
                buildReceipt(
                    billId,
                    date,
                    cart,
                    overallDiscount,
                    finalTotal
                )

            printer.printFormattedText(
                billText
            )

        } catch (e: Exception) {

            e.printStackTrace()

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    e.localizedMessage ?: "Unable to connect to printer",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun buildDetailReceipt(
        billId: Long,
        date: String,
        cart: List<BillCartItem>,
        overallDiscount: Double,
        total: Double
    ): String {

        val text = StringBuilder()

        // ==========================
        // HEADER
        // ==========================

        text.append("[C]<b>ARHAM WHOLESALE STORE</b>\n")
        text.append("[C]Phone# 0305-8971088\n")
        text.append("[C]------------------------------\n")

        text.append("[L]Bill #$billId\n")
        text.append("[L]$date\n")

        text.append("[C]------------------------------\n")

        // ==========================
        // ITEM HEADER
        // ==========================

        text.append(
            "[L]U.Price    Qty    Disc    Subtotal\n"
        )

        text.append(
            "[C]------------------------------\n"
        )

        // ==========================
        // ITEMS
        // ==========================

        cart.forEachIndexed { index, item ->

            val unitPrice =
                item.product.wholesalePrice

            val discount =
                item.discount

            val effectivePrice =
                unitPrice - discount

            val subtotal =
                effectivePrice * item.quantity

            // Product Name Row
            text.append(
                "[L]${index + 1}. ${item.product.productName}\n"
            )

            // Details Row
            text.append(
                String.format(
                    Locale.getDefault(),
                    "[L]%-10.1f%-7d%-8.1f%.0f\n",
                    unitPrice,
                    item.quantity,
                    discount,
                    subtotal
                )
            )

            text.append("\n")
        }

        // ==========================
        // FOOTER
        // ==========================

        text.append(
            "[C]------------------------------\n"
        )

        if (overallDiscount > 0) {
            text.append(
                "[R]Bill Discount : Rs ${overallDiscount.toInt()}\n"
            )
        }

        text.append(
            "[R]<b>TOTAL : Rs ${total.toInt()}</b>\n"
        )

        text.append(
            "[C]------------------------------\n"
        )

        text.append("\n")
        text.append("[C]Thank You for Visit\n")
        text.append("\n\n\n\n")

        return text.toString()
    }

    private fun buildReceipt(
        billId: Long,
        date: String,
        cart: List<BillCartItem>,
        overallDiscount: Double,
        total: Double
    ): String {

        val text = StringBuilder()

        // ==========================
        // HEADER
        // ==========================

        text.append(
            "[C]<b>ARHAM WHOLESALE STORE</b>\n"
        )

        text.append(
            "[C]Phone# 0305-8971088\n"
        )

        text.append(
            "[C]------------------------------\n"
        )

        text.append(
            "[L]Bill #$billId\n"
        )

        text.append(
            "[L]$date\n"
        )

        text.append(
            "[C]------------------------------\n"
        )

        // ==========================
        // TABLE HEADER
        // ==========================

        text.append(
            "[L]Item[R]Qty[R]Total\n"
        )

        text.append(
            "[C]------------------------------\n"
        )

        // ==========================
        // ITEMS
        // ==========================

        cart.forEachIndexed { index, it ->

            val effectivePrice =
                it.product.wholesalePrice -
                        it.discount

            val lineTotal = effectivePrice * it.quantity

            val itemName =
                "${index + 1}. ${it.product.productName}"
                    .take(18)

            text.append(
                "[L]$itemName" +
                        "[R]${it.quantity}" +
                        "[R]${lineTotal.toInt()}\n"
            )

            if (it.discount > 0) {
                text.append(
                    "[L]Disc: ${it.discount.toInt()}\n"
                )
            }
        }

        // ==========================
        // FOOTER
        // ==========================

        text.append(
            "[C]------------------------------\n"
        )

        if (overallDiscount > 0) {

            text.append(
                "[R]Bill Discount : Rs ${overallDiscount.toInt()}\n"
            )
        }

        text.append(
            "[R]<b>TOTAL : Rs ${total.toInt()}</b>\n"
        )

        text.append(
            "[C]------------------------------\n"
        )

        text.append("\n")

        text.append(
            "[C]Thank You for Visit\n"
        )

        text.append(
            "\n\n\n"
        )

        return text.toString()
    }
}