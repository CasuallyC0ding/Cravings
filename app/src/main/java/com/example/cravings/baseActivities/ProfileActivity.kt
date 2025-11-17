package com.example.cravings.baseActivities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cravings.R
import com.example.cravings.utils.CartManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {
    private var selectedImageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole: String? = null

    // Location variables
    private var merchantLat: Double? = null
    private var merchantLng: Double? = null

    companion object {
        private const val AWS_ACCESS_KEY = "" // your key
        private const val AWS_SECRET_KEY = "" // your key
        private const val BUCKET_NAME = "craversbkt"
        private const val RC_LOCATION_PERMISSION = 2001
    }

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat = result.data!!.getDoubleExtra("picked_lat", Double.NaN)
            val lng = result.data!!.getDoubleExtra("picked_lng", Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) {
                merchantLat = lat
                merchantLng = lng
                updateLocationDisplay()
                Toast.makeText(this, "Location selected successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userRole = intent.getStringExtra("userRole") ?: "Customer"
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val profileImage = findViewById<ImageView>(R.id.profileImage)
        val editProfileBtn = findViewById<ImageButton>(R.id.editProfileBtn)
        val greetingText = findViewById<TextView>(R.id.greetingText)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val shopNameLabel = findViewById<TextView>(R.id.shopNameLabel)
        val shopNameInput = findViewById<EditText>(R.id.shopNameInput)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)
        val locationText = findViewById<TextView>(R.id.locationText)
        val setLocationBtn = findViewById<Button>(R.id.setLocationBtn)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val signOutBtn = findViewById<Button>(R.id.signOutBtn)

        backButton.setOnClickListener { finish() }

        if (userRole == "Merchant") {
            shopNameInput.visibility = View.VISIBLE
            shopNameLabel.visibility = View.VISIBLE
            locationSection.visibility = View.VISIBLE
        }

        val uid = auth.currentUser?.uid ?: return

        // Load profile data
        database.reference.child("users").child(userRole!!).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").value?.toString() ?: ""
                    val phone = snapshot.child("phone").value?.toString() ?: ""
                    val shopName = snapshot.child("shopName").value?.toString() ?: ""
                    val imageUrl = snapshot.child("profileImage").value?.toString()

                    val firstName = name.split(" ").firstOrNull() ?: "User"
                    greetingText.text = "Hello, $firstName 👋"

                    nameInput.setText(name)
                    phoneInput.setText(phone)
                    if (userRole == "Merchant") {
                        shopNameInput.setText(shopName)

                        // Load location if exists
                        val locationSnapshot = snapshot.child("location")
                        if (locationSnapshot.exists()) {
                            merchantLat = locationSnapshot.child("lat").getValue(Double::class.java)
                            merchantLng = locationSnapshot.child("lng").getValue(Double::class.java)
                            updateLocationDisplay()
                        }
                    }

                    Glide.with(this@ProfileActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(profileImage)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Change profile photo
        editProfileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        // Set location button
        setLocationBtn.setOnClickListener {
            showLocationOptionsDialog()
        }

        // Save changes
        saveBtn.setOnClickListener {
            val updatedName = nameInput.text.toString().trim()
            val updatedPhone = phoneInput.text.toString().trim()

            val updates = mutableMapOf<String, Any>(
                "name" to updatedName,
                "phone" to updatedPhone
            )

            if (userRole == "Merchant") {
                updates["shopName"] = shopNameInput.text.toString().trim()

                // Save location if set
                if (merchantLat != null && merchantLng != null) {
                    updates["location"] = mapOf(
                        "lat" to merchantLat!!,
                        "lng" to merchantLng!!
                    )
                }
            }

            database.reference.child("users").child(userRole!!).child(uid)
                .updateChildren(updates)
                .addOnSuccessListener {
                    // ✅ Update greeting instantly
                    val firstName = updatedName.split(" ").firstOrNull() ?: "User"
                    greetingText.text = "Hello, $firstName 👋"

                    Toast.makeText(this, "Changes saved ✅", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed ❌", Toast.LENGTH_SHORT).show()
                }
        }

        // Sign out
        signOutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    auth.signOut()
                    CartManager.clearCart()
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showLocationOptionsDialog() {
        val options = arrayOf("📍 Use Current Location", "🗺 Choose on Map")
        AlertDialog.Builder(this)
            .setTitle("Select Location Method")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ensureLocationPermissionAndGetCurrent()
                    1 -> openMapPicker()
                }
            }
            .show()
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
                        merchantLat = location.latitude
                        merchantLng = location.longitude
                        updateLocationDisplay()
                        Toast.makeText(this, "Current location captured", Toast.LENGTH_SHORT).show()
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

    private fun openMapPicker() {
        val intent = Intent(this, MapPickerActivity::class.java)
        mapPickerLauncher.launch(intent)
    }

    private fun updateLocationDisplay() {
        val locationText = findViewById<TextView>(R.id.locationText)
        if (merchantLat != null && merchantLng != null) {
            locationText.text = "📍 Location: Lat: %.4f, Lng: %.4f".format(merchantLat, merchantLng)
            locationText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        } else {
            locationText.text = "📍 No location set"
            locationText.setTextColor(resources.getColor(android.R.color.darker_gray))
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            uploadToS3(data.data!!)
            findViewById<ImageView>(R.id.profileImage).setImageURI(data.data)
        }
    }

    private fun uploadToS3(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val file = getFileFromUri(uri) ?: return

        val s3 = AmazonS3Client(
            BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY),
            Region.getRegion(Regions.EU_NORTH_1)
        )
        TransferNetworkLossHandler.getInstance(applicationContext)

        val transferUtility = TransferUtility.builder().context(applicationContext).s3Client(s3).defaultBucket(BUCKET_NAME).build()
        val key = "profile_pictures/$uid.jpg"

        transferUtility.upload(BUCKET_NAME, key, file, CannedAccessControlList.PublicRead)
            .setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    if (state == TransferState.COMPLETED) {
                        val imageUrl = s3.getResourceUrl(BUCKET_NAME, key)
                        database.reference.child("users").child(userRole!!).child(uid)
                            .updateChildren(mapOf("profileImage" to imageUrl))
                    }
                }
                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                override fun onError(id: Int, ex: Exception?) {}
            })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(cacheDir, "${System.currentTimeMillis()}.jpg")
        inputStream.copyTo(FileOutputStream(file))
        return file
    }
}