package com.example.cravings.baseActivities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.CartAdapter
import com.example.cravings.models.Product
import com.example.cravings.utils.CartManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.*

class CartActivity : AppCompatActivity() {

    companion object {
        const val RC_LOCATION_PERMISSION = 1001
        const val REQUEST_MAP_PICK = 2001
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private lateinit var proceedButton: Button
    private lateinit var clearCartButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var shopNameText: TextView

    private lateinit var checkShopPickup: CheckBox
    private lateinit var checkDelivery: CheckBox
    private lateinit var deliverySection: LinearLayout
    private lateinit var deliveryCostSection: LinearLayout
    private lateinit var btnCurrentLocation: Button
    private lateinit var btnSelectLocation: Button
    private lateinit var selectedLocationText: TextView
    private lateinit var txtItemsTotal: TextView
    private lateinit var txtDeliveryFee: TextView
    private lateinit var txtFinalTotal: TextView
    private lateinit var auth: FirebaseAuth
    private var cartItems = mutableListOf<Product>()
    private var userLat: Double? = null
    private var userLng: Double? = null
    private var storeLat: Double? = null
    private var storeLng: Double? = null
    private var deliveryFee = 0.0
    private var fetchedStoreLocation = false

    private lateinit var database: FirebaseDatabase

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat = result.data!!.getDoubleExtra("picked_lat", Double.NaN)
            val lng = result.data!!.getDoubleExtra("picked_lng", Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) {
                userLat = lat
                userLng = lng
                selectedLocationText.text = "Lat: %.6f, Lng: %.6f".format(lat, lng)
                tryCalculateDelivery()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupRecyclerView()
        setupButtons()
        setupCheckboxes()
        updateTotals()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewCart)
        proceedButton = findViewById(R.id.proceedButton)
        backButton = findViewById(R.id.backButton)
        clearCartButton = findViewById(R.id.clearCartButton)
        shopNameText = findViewById(R.id.shopNameText)
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
        auth = FirebaseAuth.getInstance()
        checkShopPickup = findViewById(R.id.checkShopPickup)
        checkDelivery = findViewById(R.id.checkDelivery)
        deliverySection = findViewById(R.id.deliverySection)
        deliveryCostSection = findViewById(R.id.deliveryCostSection)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)
        btnSelectLocation = findViewById(R.id.btnSelectLocation)
        selectedLocationText = findViewById(R.id.selectedLocationText)
        txtItemsTotal = findViewById(R.id.txtItemsTotal)
        txtDeliveryFee = findViewById(R.id.txtDeliveryFee)
        txtFinalTotal = findViewById(R.id.txtFinalTotal)

        shopNameText.text = CartManager.shopName ?: "Shop"

        cartItems = if (CartManager.getCartItems().isNotEmpty()) {
            CartManager.getCartItems()
        } else {
            intent.getParcelableArrayListExtra<Product>("cart")?.toMutableList() ?: mutableListOf()
        }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(cartItems) { updateTotals() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        backButton.setOnClickListener { onBackPressed() }
        clearCartButton.setOnClickListener {
            CartManager.clearCart()
            cartItems.clear()
            adapter.notifyDataSetChanged()
            updateTotals()
        }

        btnCurrentLocation.setOnClickListener { ensureLocationPermissionAndGetCurrent() }
        btnSelectLocation.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        proceedButton.setOnClickListener {
            if (!checkShopPickup.isChecked && !checkDelivery.isChecked) {
                Toast.makeText(this, "Please choose a pickup method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (checkDelivery.isChecked) {
                if (userLat == null || userLng == null) {
                    Toast.makeText(this, "Please choose a delivery location", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!fetchedStoreLocation || storeLat == null || storeLng == null) {
                    Toast.makeText(this, "Store location not available. Try again.", Toast.LENGTH_SHORT).show()
                    fetchStoreLocation()
                    return@setOnClickListener
                }
            }

            proceedToCheckout()
        }

    }

    private fun setupCheckboxes() {
        checkShopPickup.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkDelivery.isChecked = false
                deliverySection.visibility = View.GONE
                showItemsTotalOnly()
            }
        }

        checkDelivery.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkShopPickup.isChecked = false
                deliverySection.visibility = View.VISIBLE
                if (!fetchedStoreLocation) fetchStoreLocation()
                updateTotals()
            } else {
                deliverySection.visibility = View.GONE
                showItemsTotalOnly()
            }
        }
    }

    private fun updateTotals() {
        val itemsTotal = cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }

        if (checkDelivery.isChecked) {
            val finalTotal = itemsTotal + deliveryFee
            txtItemsTotal.text = "Items Total: EGP %.2f".format(itemsTotal)
            txtDeliveryFee.text = "Delivery Fee: EGP %.2f".format(deliveryFee)
            txtFinalTotal.text = "Final Total: EGP %.2f".format(finalTotal)
            deliveryCostSection.visibility = View.VISIBLE
        } else {
            showItemsTotalOnly()
        }
    }

    private fun showItemsTotalOnly() {
        val itemsTotal = cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }
        txtItemsTotal.text = "Items Total: EGP %.2f".format(itemsTotal)
        txtDeliveryFee.text = ""
        txtFinalTotal.text = "Total: EGP %.2f".format(itemsTotal)
        deliveryCostSection.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra("updatedCart", ArrayList(cartItems))
        }
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }

    // ---------- Location & delivery fee logic ----------

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

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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
                        selectedLocationText.text = "Lat: %.6f, Lng: %.6f".format(userLat, userLng)
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
                } else {
                    Toast.makeText(this, "Store not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed fetching store: ${e.message}", Toast.LENGTH_SHORT).show()
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
        deliveryFee = max(distanceKm * ratePerKm, 15.0) // minimum fee
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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


    private fun proceedToCheckout() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        val shopUid = CartManager.shopId
        if (shopUid.isNullOrEmpty()) {
            Toast.makeText(this, "Shop ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        val itemsTotal = cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }
        val orderTotal = if (checkDelivery.isChecked) itemsTotal + deliveryFee else itemsTotal

        // Full order details (for Merchant) with status
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
            "deliveryFee" to if (checkDelivery.isChecked) deliveryFee else 0.0,
            "orderTotal" to orderTotal,
            "pickupMethod" to if (checkShopPickup.isChecked) "shop" else "delivery",
            "deliveryLat" to userLat,
            "deliveryLng" to userLng,
            "timestamp" to System.currentTimeMillis(),
            "status" to "Waiting for Seller Approval",  // <--- new status field
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

        // Upload full order for Merchant
        merchantRef.child(orderId).setValue(fullOrderData)
            .addOnSuccessListener {
                // For Customer: just create a reference with shopUid -> orderId
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
                        adapter.notifyDataSetChanged()
                        updateTotals()

                        val intent = Intent(this, HomeCustomerActivity::class.java)
                        intent.putExtra("openOrdersTab", true) // send a flag to open Orders tab
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
