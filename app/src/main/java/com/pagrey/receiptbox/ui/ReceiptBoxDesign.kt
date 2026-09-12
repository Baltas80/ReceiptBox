package com.pagrey.receiptbox.ui

import androidx.compose.ui.unit.dp

/**
 * ReceiptBox visual direction and shared design tokens.
 *
 * Keep visual decisions here so screens and the Material theme use the same
 * spacing and corner language instead of accumulating one-off values.
 *
 * Design principles:
 * - Fast capture: primary action is always visually dominant.
 * - Finance clarity: totals use strong hierarchy and consistent formatting.
 * - Calm, clean cards with generous spacing.
 * - Light and dark themes from the same component system.
 * - Accessibility: minimum 48dp touch targets and readable contrast.
 */
object ReceiptBoxDesign {
    const val APP_NAME = "ReceiptBox"
    const val TAGLINE = "Tus tickets, siempre a mano"
    const val ADD_RECEIPT = "Añadir ticket"
    const val EMPTY_TITLE = "Aún no tienes tickets"
    const val EMPTY_BODY = "Fotografía o importa tu primer ticket y ReceiptBox organizará sus datos."
    const val SEARCH_PLACEHOLDER = "Buscar tickets"

    // Shared layout rhythm.
    val SCREEN_PADDING = 18.dp
    val COMPACT_SPACING = 8.dp
    val ITEM_SPACING = 14.dp
    val CARD_PADDING = 16.dp
    val TOUCH_TARGET = 48.dp

    // Shared corner language: compact controls -> cards -> hero surfaces.
    val CORNER_COMPACT = 10.dp
    val CORNER_SMALL = 14.dp
    val CORNER_MEDIUM = 20.dp
    val CORNER_LARGE = 26.dp
    val CORNER_EXTRA_LARGE = 32.dp
}
