package com.example.cravings.baseActivities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.cravings.R
import com.example.cravings.models.Product
import com.example.cravings.utils.CartManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import kotlin.math.*

class CheckoutActivity : AppCompatActivity() {

    companion object {
        const val RC_LOCATION_PERMISSION = 1001
    }

    private lateinit var backButton: ImageButton
    private lateinit var shopNameText: TextView
    private lateinit var txtItemsTotal: TextView
    private lateinit var txtDeliveryFee: TextView
    private lateinit var txtFinalTotal: TextView

    private lateinit var radioShopPickup: RadioButton
    private lateinit var radioDelivery: RadioButton
    private lateinit var deliveryOptionsCard: androidx.cardview.widget.CardView

    private lateinit var btnCurrentLocation: Button
    private lateinit var btnSelectFromMap: Button
    private lateinit var btnTypeAddress: Button
    private lateinit var addressInputLayout: LinearLayout
    private lateinit var editTextAddress: EditText
    private lateinit var btnConfirmAddress: Button
    private lateinit var selectedLocationText: TextView

    private lateinit var btnPlaceOrder: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var cartItems = mutableListOf<Product>()
    private var userLat: Double? = null
    private var userLng: Double? = null
    private var storeLat: Double? = null
    private var storeLng: Double? = null
    private var deliveryFee = 0.0
    private var fetchedStoreLocation = false

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat = result.data!!.getDoubleExtra("picked_lat", Double.NaN)
            val lng = result.data!!.getDoubleExtra("picked_lng", Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) {
                userLat = lat
                userLng = lng
                selectedLocationText.text = "Location selected: Lat: %.4f, Lng: %.4f".format(lat, lng)
                selectedLocationText.visibility = View.VISIBLE
                tryCalculateDelivery()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        initViews()
        setupListeners()
        loadCartData()
        fetchStoreLocation()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        shopNameText = findViewById(R.id.shopNameText)
        txtItemsTotal = findViewById(R.id.txtItemsTotal)
        txtDeliveryFee = findViewById(R.id.txtDeliveryFee)
        txtFinalTotal = findViewById(R.id.txtFinalTotal)

        radioShopPickup = findViewById(R.id.radioShopPickup)
        radioDelivery = findViewById(R.id.radioDelivery)
        deliveryOptionsCard = findViewById(R.id.deliveryOptionsLayout)

        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)
        btnSelectFromMap = findViewById(R.id.btnSelectFromMap)
        btnTypeAddress = findViewById(R.id.btnTypeAddress)
        addressInputLayout = findViewById(R.id.addressInputLayout)
        editTextAddress = findViewById(R.id.editTextAddress)
        btnConfirmAddress = findViewById(R.id.btnConfirmAddress)
        selectedLocationText = findViewById(R.id.selectedLocationText)

        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        shopNameText.text = CartManager.shopName ?: "Shop"
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        radioShopPickup.setOnClickListener {
            radioDelivery.isChecked = false
            deliveryOptionsCard.visibility = View.GONE
            addressInputLayout.visibility = View.GONE
            selectedLocationText.visibility = View.GONE
            deliveryFee = 0.0
            updateTotals()
        }

        radioDelivery.setOnClickListener {
            radioShopPickup.isChecked = false
            deliveryOptionsCard.visibility = View.VISIBLE
            if (!fetchedStoreLocation) {
                fetchStoreLocation()
            }
        }

        btnCurrentLocation.setOnClickListener {
            addressInputLayout.visibility = View.GONE
            ensureLocationPermissionAndGetCurrent()
        }

        btnSelectFromMap.setOnClickListener {
            addressInputLayout.visibility = View.GONE
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        btnTypeAddress.setOnClickListener {
            addressInputLayout.visibility = View.VISIBLE
            selectedLocationText.visibility = View.GONE
        }

        btnConfirmAddress.setOnClickListener {
            val address = editTextAddress.text.toString().trim()
            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            geocodeAddress(address)
        }

        btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun loadCartData() {
        cartItems = CartManager.getCartItems()
        updateTotals()
    }

    private fun updateTotals() {
        val itemsTotal = cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }
        val finalTotal = itemsTotal + deliveryFee

        txtItemsTotal.text = "Items Total: EGP %.2f".format(itemsTotal)
        txtDeliveryFee.text = "Delivery Fee: EGP %.2f".format(deliveryFee)
        txtFinalTotal.text = "Total: EGP %.2f".format(finalTotal)
    }

    private fun fetchStoreLocation() {
        val shopId = CartManager.shopId
        if (shopId.isNullOrEmpty()) {
            Toast.makeText(this, "Store information not available", Toast.LENGTH_SHORT).show()
            return
        }

        database.reference.child("users").child("Merchant").child(shopId).child("location").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    storeLat = snapshot.child("lat").getValue(Double::class.java)
                    storeLng = snapshot.child("lng").getValue(Double::class.java)
                    fetchedStoreLocation = (storeLat != null && storeLng != null)
                    if (fetchedStoreLocation) tryCalculateDelivery()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed fetching store: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun ensureLocationPermissionAndGetCurrent() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, RC_LOCATION_PERMISSION)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fused = LocationServices.getFusedLocationProviderClient(this)
            fused.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                        selectedLocationText.text = "Current location: Lat: %.4f, Lng: %.4f".format(userLat, userLng)
                        selectedLocationText.visibility = View.VISIBLE
                        tryCalculateDelivery()
                    } else {
                        Toast.makeText(this, "Cannot obtain current location", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun geocodeAddress(address: String) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                userLat = location.latitude
                userLng = location.longitude
                selectedLocationText.text = "Address: $address\nLat: %.4f, Lng: %.4f".format(userLat, userLng)
                selectedLocationText.visibility = View.VISIBLE
                addressInputLayout.visibility = View.GONE
                editTextAddress.text.clear()
                tryCalculateDelivery()
            } else {
                Toast.makeText(this, "Address not found. Please try again.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryCalculateDelivery() {
        if (userLat == null || storeLat == null || userLng == null || storeLng == null) return
        calculateDeliveryFee(userLat!!, userLng!!, storeLat!!, storeLng!!)
        updateTotals()
    }

    private fun calculateDeliveryFee(userLat: Double, userLng: Double, shopLat: Double, shopLng: Double) {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(shopLat - userLat)
        val dLon = Math.toRadians(shopLng - userLng)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(userLat)) * cos(Math.toRadians(shopLat)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = earthRadius * c
        val ratePerKm = 5.0
        deliveryFee = max(distanceKm * ratePerKm, 15.0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_LOCATION_PERMISSION) {
            if (grantResults.size >= 2 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun placeOrder() {
        if (!radioShopPickup.isChecked && !radioDelivery.isChecked) {
            Toast.makeText(this, "Please select a pickup method", Toast.LENGTH_SHORT).show()
            return
        }

        if (radioDelivery.isChecked) {
            if (userLat == null || userLng == null) {
                Toast.makeText(this, "Please select a delivery location", Toast.LENGTH_SHORT).show()
                return
            }
            if (!fetchedStoreLocation || storeLat == null || storeLng == null) {
                Toast.makeText(this, "Store location not available. Try again.", Toast.LENGTH_SHORT).show()
                fetchStoreLocation()
                return
            }
        }

        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        val shopUid = CartManager.shopId
        if (shopUid.isNullOrEmpty()) {
            Toast.makeText(this, "Shop ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        val itemsTotal = cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }
        val orderTotal = if (radioDelivery.isChecked) itemsTotal + deliveryFee else itemsTotal

        val fullOrderData = hashMapOf(
            "customerUid" to currentUserUid,
            "items" to cartItems.map { item ->
                hashMapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "price" to item.price,
                    "quantity" to item.selectedQuantity
                )
            },
            "itemsTotal" to itemsTotal,
            "deliveryFee" to if (radioDelivery.isChecked) deliveryFee else 0.0,
            "orderTotal" to orderTotal,
            "pickupMethod" to if (radioShopPickup.isChecked) "shop" else "delivery",
            "deliveryLat" to userLat,
            "deliveryLng" to userLng,
            "timestamp" to System.currentTimeMillis(),
            "status" to "Waiting for Seller Approval",
            "shopName" to CartManager.shopName
        )

        val merchantRef = database.reference
            .child("users")
            .child("Merchant")
            .child(shopUid)
            .child("orders")
            .child(currentUserUid)

        val orderId = merchantRef.push().key
        if (orderId == null) {
            Toast.makeText(this, "Failed to generate order ID", Toast.LENGTH_SHORT).show()
            return
        }

        merchantRef.child(orderId).setValue(fullOrderData)
            .addOnSuccessListener {
                val customerRef = database.reference
                    .child("users")
                    .child("Customer")
                    .child(currentUserUid)
                    .child("orders")
                    .child(shopUid)
                    .child(orderId)

                customerRef.setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                        CartManager.clearCart()
                        cartItems.clear()

                        val intent = Intent(this, HomeCustomerActivity::class.java)
                        intent.putExtra("openOrdersTab", true)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save order for customer: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to place order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}