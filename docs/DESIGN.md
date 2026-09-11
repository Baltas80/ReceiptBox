# ReceiptBox — Visual Design

## Direction
ReceiptBox should feel like a practical personal utility rather than an accounting suite: clean, quick and trustworthy.

## Core navigation
- **Inicio**: monthly spend summary, recent receipts and primary capture action.
- **Tickets**: searchable chronological list with merchant, date, category and total.
- **Añadir**: camera/import flow, followed by OCR review.
- **Detalle**: receipt image, extracted fields, category and actions.
- **Ajustes**: storage, export, privacy and appearance.

## Home hierarchy
1. Header: ReceiptBox + concise monthly context.
2. Summary card: total spent this month and ticket count.
3. Primary CTA: Añadir ticket.
4. Recent tickets.
5. Bottom navigation.

## Capture flow
Camera/import → processing state → OCR result review → edit fields → save.

The review screen must make uncertainty obvious. OCR values remain editable and the original image is preserved.

## Component rules
- Minimum 48dp interactive targets.
- Consistent 16dp horizontal content padding.
- Rounded cards with restrained elevation.
- One dominant primary action per screen.
- Empty states explain the next action instead of showing decorative content.
- Light/dark theme supported from the same component system.

## Future premium surface
Keep space for a non-intrusive banner/ad slot without compromising the capture CTA. Premium can later remove advertising and unlock advanced analytics, warranty tracking and cloud sync.
