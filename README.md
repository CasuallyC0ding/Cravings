# Cravings 🍔

A multi-role food ordering Android app built with Kotlin. Cravings connects customers, merchants, and delivery drivers on a single platform backed by Firebase — with real-time order tracking, push notifications, map-based delivery, and even a peer-to-peer VoIP call feature.

## Features

### Customer
- Browse shops and their product menus
- Add items to a persistent cart (single-shop enforced)
- Checkout with flexible delivery options: current GPS location, map picker, or typed address
- Redeem loyalty points for discounts at checkout
- Live order tracking with push notification updates at every status change
- View full order history with itemised receipts

### Merchant
- Manage product catalogue (add, edit, delete with image upload to AWS S3)
- Receive real-time incoming order notifications
- Progress orders through a status workflow (accepted → preparing → ready / out for delivery → delivered)
- Stock alert push notification when a product hits zero inventory
- In-app VoIP call tab for direct audio communication

### Delivery Driver
- Browse available shops and pick up pending orders
- Accept and manage active deliveries
- Update delivery status in real time

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts + View Binding (Material Design 3) |
| Auth | Firebase Authentication |
| Database | Firebase Realtime Database |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Image Storage | AWS S3 (via AWS Android SDK) |
| Maps | Google Maps SDK + OSMDroid |
| Location | Google Play Services Location |
| Image Loading | Glide |
| VoIP | Raw UDP sockets + Android AudioRecord/AudioTrack |
| Backend Server | Python (`cravings_server.py`) with `firebase-admin` |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 |

## Project Structure

```
app/src/main/java/com/example/cravings/
├── baseActivities/
│   ├── common/
│   │   ├── MainActivity.kt              # Entry point; routes by role & notification type
│   │   ├── LoginActivity.kt             # Role-aware login with FCM token registration
│   │   ├── SignupActivity.kt
│   │   ├── RoleSelectionActivity.kt     # Customer / Merchant / Delivery role picker
│   │   ├── ProfileActivity.kt
│   │   ├── MapPickerActivity.kt         # OSMDroid map for delivery address selection
│   │   ├── VoipActivity.kt              # UDP-based peer-to-peer audio call
│   │   └── NotificationHandlerActivity.kt
│   ├── customer/
│   │   ├── HomeCustomerActivity.kt
│   │   ├── ShopProductsActivity.kt
│   │   ├── CartActivity.kt
│   │   └── CheckoutActivity.kt          # GPS + map + address, loyalty points, order placement
│   └── merchant/
│       ├── HomeMerchantActivity.kt
│       ├── AddProductActivity.kt        # Product creation with S3 image upload
│       ├── EditProductActivity.kt
│       └── ProductActivity.kt
├── delivery/
│   ├── DeliveryOrdersActivity.kt
│   └── DeliveryShopsActivity.kt
├── fragments/
│   ├── customer/
│   │   ├── ShopsFragment.kt
│   │   ├── OrdersFragment.kt
│   │   └── AccountFragment.kt
│   └── merchant/
│       ├── MerchantOrdersFragment.kt    # Real-time order management with status controls
│       ├── MerchantProductsFragment.kt
│       └── CallFragment.kt
├── adapters/                            # RecyclerView adapters for all three roles
├── models/                              # Order, OrderItem, Product, Shop data classes
├── notifications/
│   └── OrderNotificationService.kt     # FCM background message handler
└── utils/
    ├── CartManager.kt                   # In-memory cart singleton
    └── FCMTokenManager.kt
```

## Notification Server

`cravings_server.py` is a lightweight Python backend that listens to Firebase Realtime Database events and dispatches FCM push notifications. Run it on any machine with internet access — it does not need to be deployed to a cloud host.

**Handled events:**

| Firebase path pattern | Trigger | Recipients |
|---|---|---|
| `users/Merchant/{id}/orders/{customer}/{order}` | New order placed | Merchant |
| `…/orders/{customer}/{order}/status` | Status updated | Customer (always), Merchant (on delivery) |
| `users/Merchant/{id}/products/{index}/stock` | Stock reaches 0 | Merchant |

**Order status flow:**

`accepted` → `preparing` → `order ready` / `waiting for delivery` → `out for delivery` → `delivered`

### Running the server

```bash
pip install firebase-admin google-auth requests
# Place your Firebase service account key as service_account.json
python cravings_server.py
```

## Setup

### Prerequisites
- Android Studio Meerkat or newer
- JDK 11
- A Firebase project with Authentication and Realtime Database enabled
- A Google Cloud project with the Maps SDK enabled
- An AWS S3 bucket for product image uploads

### Configuration

1. **Firebase** — download `google-services.json` from the Firebase console and replace the placeholder at `app/google-services.json`. Update the Realtime Database URL in any file that references `dbcravings-default-rtdb.europe-west1.firebasedatabase.app` to match your own project.

2. **Google Maps** — replace `YOUR_API_KEY_HERE` in `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY_HERE" />
   ```

3. **AWS S3** — update the bucket name, region, and credentials in `AddProductActivity.kt` / `EditProductActivity.kt`.

4. **Notification server** — place your Firebase service account JSON as `service_account.json` alongside `cravings_server.py` and run the script.

### Build

```bash
./gradlew assembleDebug
```

Or open in Android Studio and run directly on a device or emulator (API 24+).

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Firebase, FCM, AWS S3, maps |
| `ACCESS_FINE_LOCATION` | GPS location for delivery |
| `POST_NOTIFICATIONS` | Order & stock push notifications |
| `RECORD_AUDIO` | VoIP calls |
| `MODIFY_AUDIO_SETTINGS` | VoIP audio routing |
| `READ_EXTERNAL_STORAGE` | Product image selection |
