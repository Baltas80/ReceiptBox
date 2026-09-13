# ReceiptBox

ReceiptBox is a privacy-first Android app for capturing, extracting and organizing purchase receipts locally.

## Current release candidate
- Version: `0.2.0` (`versionCode 2`)
- Package: `com.pagrey.receiptbox`
- Android: `minSdk 26`, `targetSdk 36`

## Features
- Capture receipts with the document scanner
- Import receipt images from the gallery
- OCR receipt text with ML Kit
- Review and edit merchant, date, total, tax, receipt number and category before saving
- Store receipts locally with Room
- Search and browse receipts
- View receipt details and delete or edit entries
- Statistics by month and category
- Dark mode
- JSON backup and restore (receipt data only)
- Full ZIP backup and restore (receipt data plus available receipt images)
- CSV export
- PDF export
- Premium Material 3 interface with shared design tokens

## Privacy
Receipt data is stored locally on the device. ReceiptBox does not require an account for its core workflow.

Android automatic cloud backup is configured not to include the receipt database or private receipt images. Device-to-device transfer can migrate those private receipt files when Android supports the configured transfer rules. The app also provides explicit JSON and full ZIP backups so the user can control when a portable copy is created.

JSON backups preserve receipt data and stored image paths, but do not embed image files. A JSON backup restored on another device may therefore require the original images to be re-imported.

Full ZIP backups are designed for device migration: they embed available receipt images inside the archive and recreate them in the app's private receipt storage during restore. If an image referenced by the backup metadata is unavailable, the receipt is restored without that image and the restore reports the missing image count.

## Technology
- Kotlin
- Jetpack Compose / Material 3
- Room
- CameraX
- Google ML Kit Text Recognition
- ML Kit Document Scanner
- GitHub Actions for automated tests, Lint and release-bundle validation

## Build
Use the Gradle wrapper to build the project. The release build is configured to use signing credentials only when the required release signing environment variables are present; otherwise CI validates an unsigned release AAB.

## Project status
The codebase is maintained as a release candidate. Automated CI validates unit tests, Android Lint, the debug APK and the release AAB on every push to `master`.
