# Personal Finance App Implementation Plan

This plan outlines the steps to create a simple personal finance Android app in Java, featuring manual tracking, credit card management, and automatic bank notification parsing.

## User Review Required

> [!IMPORTANT]
> The app requires **Notification Access** permission to monitor bank notifications. The user will be guided to the system settings to enable this.
> All data is stored locally using Room (SQLite). No cloud sync is implemented.

## Proposed Changes

### 1. Project Configuration & Dependencies
- Add Room database dependencies.
- Add Material Design 3 support.
- Add Navigation component (optional, but recommended for BottomNav).

### 2. Data Layer (FASE 1)
- **Entities**: `Transaction`, `Card`, `Category`, `NotificationEvent`.
- **DAO**: Interfaces for each entity.
- **Database**: `AppDatabase` class.

### 3. Notification Parsing Logic (FASE 6)
- `NotificationParser`: Logic to extract values (R$), types (Income/Expense), and descriptions from bank notifications (Nubank, Inter, etc.).

### 4. Background Service (FASE 5)
- `FinanceNotificationListener`: Extends `NotificationListenerService` to capture notifications and trigger suggestions.

### 5. UI Components (FASE 2, 3, 4, 7, 8, 9, 10)
- **MainActivity**: Layout with BottomNavigationView.
- **DashboardFragment**: Summary cards, simple bar charts for cash flow.
- **TransactionListFragment**: List of all transactions with filtering.
- **AddTransactionActivity**: Form for manual entry and editing.
- **CardFragment**: Credit card management.
- **SettingsFragment**: Notification monitoring toggle and app selection.
- **NotificationConfirmationActivity/Dialog**: Popup to confirm identified transactions.

## Verification Plan

### Automated Tests
- Unit tests for `NotificationParser` to ensure various bank notification formats are correctly parsed.

### Manual Verification
- Deploy to emulator/device.
- Manually add income/expense and check balance.
- Simulate bank notifications using `adb` or a notification generator app to verify the listener and parser.
- Verify that duplicates are ignored based on the hash.
