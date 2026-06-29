# BakeFlow

BakeFlow helps bakery owners manage daily operations — products, ingredients, inventory, production, sales, waste, and reports.

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- MVVM + Repository Pattern + StateFlow
- Firebase Auth, Firestore, Storage
- Navigation Compose

## Setup

1. Open this project in Android Studio (Ladybug or newer recommended).
2. Replace `app/google-services.json` with your Firebase project config from the [Firebase Console](https://console.firebase.google.com/).
3. Sync Gradle and run on an emulator or device (minSdk 26).

## Project Structure

```
com.bakeflow.app/
├── auth/           Authentication screens (Phase 2)
├── dashboard/      Daily overview (Phase 2)
├── products/       Product management (Phase 2)
├── inventory/      Ingredient & stock tracking (Phase 2)
├── production/     Production planning (Phase 2)
├── sales/          Sales recording (Phase 2)
├── waste/          Waste logging (Phase 2)
├── reports/        Analytics & reports (Phase 2)
├── common/         Shared constants and UI state types
├── data/           Repository implementations (Phase 2)
├── domain/         Entities, interfaces, use cases (Phase 2)
├── ui/             Theme and reusable Compose components
└── navigation/     Routes and NavHost
```

## Phase 1 Status

Foundation complete: project structure, Firebase SDK wiring, Material 3 theme, navigation, and placeholder screens. No business logic yet.
