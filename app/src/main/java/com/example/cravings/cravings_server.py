import json
import requests
from firebase_admin import credentials, initialize_app, db
import google.auth.transport.requests
from google.oauth2 import service_account

# -------------------------------------------------------
# FIREBASE
# -------------------------------------------------------
cred = credentials.Certificate("service_account.json")
initialize_app(cred, {
    "databaseURL": "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
})


# -------------------------------------------------------
# FCM TOKEN (API v1)
# -------------------------------------------------------
def get_access_token():
    SCOPES = ["https://www.googleapis.com/auth/firebase.messaging"]
    creds = service_account.Credentials.from_service_account_file(
        "service_account.json", scopes=SCOPES
    )
    creds.refresh(google.auth.transport.requests.Request())
    return creds.token


# -------------------------------------------------------
# NOTIFICATION SEND HELPER
# -------------------------------------------------------
def notify(token, title, body, **data):
    if not token:
        print("⚠️ No FCM token → skipping")
        return

    # FCM requires strings only
    data = {k: str(v) for k, v in data.items()}

    message = {
        "message": {
            "token": token,
            "notification": {"title": title, "body": body},
            "data": data,
            "android": {
                "priority": "high",
                "notification": {"channel_id": "orders_channel"}
            }
        }
    }

    headers = {
        "Authorization": "Bearer " + get_access_token(),
        "Content-Type": "application/json"
    }

    print(f"📤 Sending → {token[:30]}...")
    response = requests.post(
        "https://fcm.googleapis.com/v1/projects/dbcravings/messages:send",
        headers=headers,
        data=json.dumps(message)
    )
    print("FCM:", response.text)


# -------------------------------------------------------
# STATUS RULES (centralized)
# -------------------------------------------------------
STATUS_RULES = {
    "customer": {
        "order ready":   "Your order from {shop} is ready 🎉",
        "rejected":      "Sorry, your order from {shop} was rejected 😔",
        "waiting for delivery": "Your order from {shop} awaits a driver 🚗",
        "out for delivery": "🛵 Your order from {shop} is on its way!",
        "accepted":      "{shop} accepted your order 👨‍🍳",
        "preparing":     "{shop} is preparing your order 👨‍🍳",
        "delivered":     "Your order from {shop} was delivered 🎉"
    },
    "merchant": {
        "get orders":     "You have a new incoming order!",
        "completed":      "Order completed successfully.",
        "delivered":      "Order #{short} has been delivered."
    }
}


# -------------------------------------------------------
# TOKEN FETCH
# -------------------------------------------------------
def get_tokens(merchant_id, customer_id):
    return {
        "merchant": db.reference(f"users/Merchant/{merchant_id}/fcmToken").get(),
        "customer": db.reference(f"users/Customer/{customer_id}/fcmToken").get()
    }


# -------------------------------------------------------
# STATUS CHANGE HANDLER
# -------------------------------------------------------
def handle_status(merchant_id, customer_id, order_id, status):
    if not status:
        print("⚠️ Empty status, ignored")
        return

    s = status.lower().strip()
    print(f"🔄 Status detected: {s}")

    tokens = get_tokens(merchant_id, customer_id)
    shop = db.reference(f"users/Merchant/{merchant_id}/shopName").get() or "Shop"

    # CUSTOMER STATUS
    if s in STATUS_RULES["customer"]:
        msg = STATUS_RULES["customer"][s].format(shop=shop)
        notify(tokens["customer"], "Order Update 📦", msg,
               type="order_status", status=status,
               orderId=order_id, customerId=customer_id, merchantId=merchant_id)

        # Also notify merchant when delivered
        if s == "delivered":
            notify(tokens["merchant"], "Order Delivered",
                   STATUS_RULES["merchant"]["delivered"].format(short=order_id[-8:]),
                   type="merchant_status", status=status,
                   orderId=order_id, customerId=customer_id, merchantId=merchant_id)
        return

    # MERCHANT STATUS
    if s in STATUS_RULES["merchant"]:
        notify(tokens["merchant"], "Order Update 📦",
               STATUS_RULES["merchant"][s].format(short=order_id[-8:]),
               type="merchant_status", status=status,
               orderId=order_id, customerId=customer_id, merchantId=merchant_id)
        return

    print("ℹ️ Ignored status:", status)


# -------------------------------------------------------
# NEW ORDER HANDLER
# -------------------------------------------------------
def handle_new_order(merchant_id, customer_id, order_id, data):
    print(f"🆕 New Order → {order_id}")

    tokens = get_tokens(merchant_id, customer_id)
    merchant_token = tokens["merchant"]

    customer_name = db.reference(f"users/Customer/{customer_id}/name").get() or "Customer"
    total = data.get("orderTotal", 0)

    notify(
        merchant_token,
        "New Order Received 🛒",
        f"{customer_name} placed an order • EGP {total}",
        type="new_order", orderId=order_id,
        customerId=customer_id, merchantId=merchant_id
    )


# -------------------------------------------------------
# STOCK UPDATE HANDLER
# -------------------------------------------------------
def handle_stock(merchant_id, product_index, new_stock):
    print(f"📦 Stock Update {product_index} → {new_stock}")

    if new_stock is None or int(new_stock) > 0:
        return

    product_name = db.reference(
        f"users/Merchant/{merchant_id}/products/{product_index}/name"
    ).get() or f"Product {product_index}"

    merchant_token = db.reference(
        f"users/Merchant/{merchant_id}/fcmToken"
    ).get()

    notify(
        merchant_token,
        "Stock Alert ⚠️",
        f"'{product_name}' is OUT OF STOCK",
        type="stock_alert",
        merchantId=merchant_id,
        productIndex=product_index,
        productName=product_name   
    )



# -------------------------------------------------------
# MAIN LISTENER
# -------------------------------------------------------
def listener(event):
    if event.path == "/" and event.data:
        print("Ready. Listening…")
        return

    print("\n🔔 EVENT:", event.path, event.data)
    parts = event.path.strip("/").split("/")

    # -----------------------
    # Parent-order node change:
    # Path: /{merchant}/orders/{customer}/{order}
    # -----------------------
    if len(parts) == 4 and parts[1] == "orders":
        merchant, _, customer, order = parts

        # If the event payload is a dict that only contains status-like keys,
        # treat it as a status update instead of a new order.
        if isinstance(event.data, dict):
            keys = set(event.data.keys())
            status_only = keys.issubset({"status", "deliveryDriverId"})
            if status_only:
                # event.data might be {'status': 'Out for Delivery'}
                status_value = event.data.get("status")
                print("ℹ️ Parent-order status-only update detected; routing to status handler.")
                return handle_status(merchant, customer, order, status_value)

        # Otherwise, get the full order object and determine if it's a real new order
        data = db.reference(
            f"users/Merchant/{merchant}/orders/{customer}/{order}"
        ).get() or {}

        if any(k in data for k in ["items", "orderTotal", "products", "orderItems"]):
            return handle_new_order(merchant, customer, order, data)
        return

    # -----------------------
    # Explicit status child node change:
    # Path: /{merchant}/orders/{customer}/{order}/status
    # -----------------------
    if len(parts) == 5 and parts[1] == "orders" and parts[4] == "status":
        merchant, _, customer, order, _ = parts
        print("ℹ️ Status child change detected; routing to status handler.")
        return handle_status(merchant, customer, order, event.data)

    # -----------------------
    # Stock Change
    # Path: /{merchant}/products/{product_index}/stock
    # -----------------------
    if len(parts) == 4 and parts[1] == "products" and parts[3] == "stock":
        merchant, _, product_index, _ = parts
        return handle_stock(merchant, product_index, event.data)

    print("ℹ️ Ignored event")



# -------------------------------------------------------
# START
# -------------------------------------------------------
print("🚀 Notification Server Started")
db.reference("users/Merchant").listen(listener)
