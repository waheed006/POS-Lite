package com.gembyte.poslite.components.printer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.gembyte.poslite.data.model.BillCartItem
import java.text.SimpleDateFormat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import java.util.Date
import java.util.Locale


object ThermalPrinterHelper {

    // ============================================================
    // PRINT BILL
    // ============================================================

    fun printBill(
        context: Context,
        billId: Long,
        billDate: Long,
        cart: List<BillCartItem>,
        overallDiscount: Double,
        finalTotal: Double,
        isDetailedBill: Boolean,
        isUrdu: Boolean
    ) {

        try {

            val connection =
                BluetoothPrintersConnections.selectFirstPaired()

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

            val billText =
                if (isDetailedBill) {

                    buildDetailReceipt(
                        printer = printer,
                        billId = billId,
                        date = date,
                        cart = cart,
                        overallDiscount = overallDiscount,
                        total = finalTotal,
                        isUrdu = isUrdu
                    )

                } else {

                    buildReceipt(
                        billId = billId,
                        date = date,
                        cart = cart,
                        overallDiscount = overallDiscount,
                        total = finalTotal
                    )
                }

            printer.printFormattedText(billText)

        } catch (e: Exception) {

            e.printStackTrace()

            Handler(Looper.getMainLooper()).post {

                Toast.makeText(
                    context,
                    e.localizedMessage
                        ?: "Unable to connect to printer",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // TOTAL ITEM DISCOUNT
    // ============================================================

    /**
     * item.discount is DISCOUNT PER UNIT.
     *
     * Example:
     *
     * Unit discount = Rs 10
     * Quantity = 3
     *
     * Total item discount = 10 × 3 = Rs 30
     */
    private fun calculateTotalItemDiscount(
        cart: List<BillCartItem>
    ): Double {

        return cart.sumOf {
            it.discount * it.quantity
        }
    }


    // ============================================================
    // TOTAL DISCOUNT
    // ============================================================

    /**
     * Total discount =
     *
     * Item discounts × quantities
     * +
     * Overall bill discount
     *
     * Example:
     *
     * Item discount:
     * Rs 10 × 3 = Rs 30
     *
     * Overall discount:
     * Rs 20
     *
     * Total discount:
     * Rs 30 + Rs 20 = Rs 50
     */
    private fun calculateTotalDiscount(
        cart: List<BillCartItem>,
        overallDiscount: Double
    ): Double {

        val totalItemDiscount =
            calculateTotalItemDiscount(cart)

        return totalItemDiscount + overallDiscount
    }


    // ============================================================
    // DETAILED RECEIPT
    // ============================================================

    private fun buildDetailReceipt(
        printer: EscPosPrinter,
        billId: Long,
        date: String,
        cart: List<BillCartItem>,
        overallDiscount: Double,
        total: Double,
        isUrdu: Boolean
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
        // ITEM HEADER
        // ==========================

        text.append(
            "[L]U.Price Qty Disc   Subtotal\n"
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

            val quantity =
                item.quantity

            val totalItemDiscount =
                item.discount * quantity

            val effectivePrice =
                unitPrice - item.discount

            val subtotal =
                effectivePrice * quantity


            // ==================================================
            // PRODUCT NAME
            // ==================================================

            if (isUrdu) {

                /**
                 * Urdu mode:
                 *
                 * 1. Use Urdu name if available.
                 * 2. Otherwise, use English product name.
                 *
                 * The name is converted to an image because
                 * the thermal printer may not support Urdu
                 * Unicode characters directly.
                 */

                val selectedName =
                    item.product.urduName
                        .trim()
                        .takeIf { it.isNotEmpty() }
                        ?: item.product.productName

                val productName =
                    "${index + 1}. $selectedName"

                text.append(
                    "[L]<img>${
                        PrinterTextParserImg
                            .bitmapToHexadecimalString(
                                printer,
                                createUrduTextBitmap(productName)
                            )
                    }</img>\n"
                )

            } else {

                // Normal English detailed bill

                text.append(
                    "[L]${index + 1}. ${item.product.productName}\n"
                )
            }


            // ==================================================
            // DETAILS ROW
            // ==================================================

            /**
             * 32 character printer width.
             *
             * U.Price  Qty  Disc  Subtotal
             *
             * No decimal values.
             *
             * IMPORTANT:
             * discount is total discount for this line.
             *
             * Example:
             * Rs 10 × Qty 3 = Rs 30
             */

            text.append(
                String.format(
                    Locale.US,
                    "[L]%-8.0f%-5d%-6.0f%13.0f\n",
                    unitPrice,
                    quantity,
                    totalItemDiscount,
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


        val totalItemDiscount =
            calculateTotalItemDiscount(cart)

        val totalDiscount =
            totalItemDiscount + overallDiscount


        // ==========================
        // DISCOUNT
        // ==========================

        if (totalDiscount > 0) {

            text.append(
                "[R]Discount : Rs ${totalDiscount.toInt()}\n"
            )
        }


        // ==========================
        // TOTAL
        // ==========================

        text.append(
            "[R]<b>Total : Rs ${total.toInt()}</b>\n"
        )

        text.append(
            "[C]------------------------------\n"
        )

        text.append("\n")

        text.append(
            "[C]Thank You for Visit\n"
        )

        text.append(
            "\n\n\n\n"
        )

        return text.toString()
    }


    // ============================================================
    // SHORT RECEIPT
    // ============================================================

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

        cart.forEachIndexed { index, item ->

            val effectivePrice =
                item.product.wholesalePrice -
                        item.discount

            val lineTotal =
                effectivePrice * item.quantity

            val itemName =
                "${index + 1}. ${item.product.productName}"
                    .take(18)

            text.append(
                "[L]$itemName" +
                        "[R]${item.quantity}" +
                        "[R]${lineTotal.toInt()}\n"
            )

            // Individual discount is NOT shown here.
            //
            // It is included in the final Discount amount.
        }


        // ==========================
        // FOOTER
        // ==========================

        text.append(
            "[C]------------------------------\n"
        )


        val totalDiscount =
            calculateTotalDiscount(
                cart = cart,
                overallDiscount = overallDiscount
            )


        // Show discount ONLY if any discount exists.

        if (totalDiscount > 0) {

            text.append(
                "[R]Discount : Rs ${totalDiscount.toInt()}\n"
            )
        }


        // ==========================
        // TOTAL
        // ==========================

        text.append(
            "[R]<b>Total : Rs ${total.toInt()}</b>\n"
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


    // ============================================================
    // CREATE URDU TEXT BITMAP
    // ============================================================

    /**
     * Thermal printers often don't have a Unicode Urdu/Arabic
     * character set or proper Arabic shaping.
     *
     * So instead of sending Urdu characters directly to the
     * printer, Android renders the Urdu text into a Bitmap.
     *
     * The bitmap is then sent to the ESC/POS printer as an image.
     *
     * This also solves:
     *
     * - Urdu character shaping
     * - RTL direction
     * - Urdu characters printing blank
     * - Urdu characters appearing disconnected
     */
    private fun createUrduTextBitmap(
        text: String
    ): Bitmap {

        // 58mm printer with 203 DPI is approximately 384 pixels.
        val bitmapWidth = 384

        val horizontalPadding = 8

        val textWidth =
            bitmapWidth - (horizontalPadding * 2)

        val textPaint =
            TextPaint(
                TextPaint.ANTI_ALIAS_FLAG or
                        TextPaint.SUBPIXEL_TEXT_FLAG
            ).apply {

                color = Color.BLACK

                textSize = 28f

                typeface =
                    Typeface.create(
                        "sans",
                        Typeface.NORMAL
                    )

                isAntiAlias = true
            }


        // ========================================================
        // StaticLayout handles:
        //
        // - RTL
        // - Urdu shaping
        // - Arabic joining
        // - line wrapping
        // ========================================================

        val layout =
            StaticLayout.Builder
                .obtain(
                    text,
                    0,
                    text.length,
                    textPaint,
                    textWidth
                )
                .setAlignment(
                    Layout.Alignment.ALIGN_OPPOSITE
                )
                .setTextDirection(
                    android.text.TextDirectionHeuristics.RTL
                )
                .setIncludePad(true)
                .build()


        val bitmapHeight =
            layout.height + 12


        val bitmap =
            Bitmap.createBitmap(
                bitmapWidth,
                bitmapHeight,
                Bitmap.Config.ARGB_8888
            )


        val canvas =
            Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)


        // Small horizontal padding
        canvas.save()

        canvas.translate(
            horizontalPadding.toFloat(),
            6f
        )

        layout.draw(canvas)

        canvas.restore()


        return bitmap
    }
}