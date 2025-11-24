package com.example.cravings.baseActivities.merchant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.*
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.example.cravings.R
import com.example.cravings.baseActivities.merchant.ProductActivity
import com.example.cravings.models.Product_Merchant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream

class AddProductActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var nameField: EditText
    private lateinit var priceField: EditText
    private lateinit var quantityField: EditText
    private lateinit var descField: EditText
    private lateinit var saveBtn: Button
    private lateinit var uploadPhotoBtn: Button
    private lateinit var imagePreview: ImageView
    private lateinit var backButton: ImageButton

    private var selectedImageUri: Uri? = null

    // --- AWS + Firebase ---
    companion object {
        private const val AWS_ACCESS_KEY = "" // your AWS key
        private const val AWS_SECRET_KEY = "" // your AWS secret
        private const val BUCKET_NAME = "craversbkt"
    }

    private val auth = FirebaseAuth.getInstance()
    private val sellerId = auth.currentUser?.uid ?: ""
    private val dbRef =
        FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
    private val TAG = "AddProductActivity"

    // --- Lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // Initialize UI
        backButton = findViewById(R.id.backButton)
        nameField = findViewById(R.id.editProductName)
        priceField = findViewById(R.id.editProductPrice)
        quantityField = findViewById(R.id.editProductQuantity)
        descField = findViewById(R.id.editProductDescription)
        saveBtn = findViewById(R.id.btnSaveProduct)
        uploadPhotoBtn = findViewById(R.id.btnUploadPhoto)
        imagePreview = findViewById(R.id.imagePreview)
        backButton.setOnClickListener { onBackPressed() }
        // Open gallery when clicking "Select Photo"
        uploadPhotoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        // Save product
        saveBtn.setOnClickListener { saveProduct() }
    }

    // --- Image Picker Launcher ---
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                selectedImageUri = result.data!!.data
                imagePreview.setImageURI(selectedImageUri)
                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
            }
        }

    // --- 1. Save Product (Main entry point) ---
    private fun saveProduct() {
        if (sellerId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val name = nameField.text.toString().trim()
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val quantity = quantityField.text.toString().toIntOrNull() ?: 0
        val desc = descField.text.toString().trim()

        if (name.isEmpty() || price <= 0) {
            Toast.makeText(this, "Please enter valid product details", Toast.LENGTH_SHORT).show()
            return
        }

        ensureMerchantPathExists {
            getNextProductId { nextId ->
                // If image is selected, upload it first
                if (selectedImageUri != null) {
                    uploadToS3(selectedImageUri!!) { imageUrl ->
                        saveProductToFirebase(nextId, name, price, quantity, desc, imageUrl)
                    }
                } else {
                    // Save without image
                    saveProductToFirebase(nextId, name, price, quantity, desc, "")
                }
            }
        }
    }

    // --- 2. Upload to S3 and return image URL ---
    private fun uploadToS3(uri: Uri, onUploaded: (String) -> Unit) {
        val file = getFileFromUri(uri) ?: return

        val s3 = AmazonS3Client(
            BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY),
            Region.getRegion(Regions.EU_NORTH_1)
        )
        TransferNetworkLossHandler.getInstance(applicationContext)

        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .s3Client(s3)
            .defaultBucket(BUCKET_NAME)
            .build()

        val key = "products/$sellerId/${System.currentTimeMillis()}.jpg"

        val observer = transferUtility.upload(
            BUCKET_NAME,
            key,
            file,
            CannedAccessControlList.PublicRead
        )

        observer.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
                    val imageUrl = s3.getResourceUrl(BUCKET_NAME, key)
                    onUploaded(imageUrl)
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                val progress = (bytesCurrent.toDouble() / bytesTotal * 100).toInt()
                Log.d(TAG, "Upload progress: $progress%")
            }

            override fun onError(id: Int, ex: Exception?) {
                Log.e(TAG, "S3 upload failed", ex)
                Toast.makeText(this@AddProductActivity, "Upload failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- 3. Write Product to Firebase ---
    private fun saveProductToFirebase(
        nextId: Int,
        name: String,
        price: Double,
        quantity: Int,
        desc: String,
        imageUrl: String
    ) {
        val product = Product_Merchant(
            productId = nextId,
            name = name,
            price = price,
            stock = quantity,
            description = desc,
            imageUrl = imageUrl
        )

        val productRef = dbRef.reference
            .child("users")
            .child("Merchant")
            .child(sellerId)
            .child("products")
            .child(nextId.toString())

        productRef.setValue(product)
            .addOnSuccessListener {
                updateLastProductId(nextId)
                Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ProductActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 4. Firebase Helper Functions ---
    private fun ensureMerchantPathExists(onReady: () -> Unit) {
        val merchantRef = dbRef.reference.child("users").child("Merchant").child(sellerId)
        merchantRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val defaultData = mapOf("lastProductId" to 0, "products" to mapOf<String, Any>())
                    merchantRef.setValue(defaultData).addOnSuccessListener { onReady() }
                } else onReady()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddProductActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getNextProductId(callback: (Int) -> Unit) {
        val lastIdRef = dbRef.reference.child("users").child("Merchant").child(sellerId).child("lastProductId")
        lastIdRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastId = snapshot.getValue(Int::class.java) ?: 0
                callback(lastId + 1)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(1)
            }
        })
    }

    private fun updateLastProductId(newId: Int) {
        dbRef.reference.child("users").child("Merchant").child(sellerId).child("lastProductId").setValue(newId)
    }

    // --- 5. Convert URI to File ---
    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(cacheDir, "${System.currentTimeMillis()}.jpg")
        inputStream.copyTo(FileOutputStream(file))
        return file
    }
}
