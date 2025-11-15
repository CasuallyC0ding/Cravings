package com.example.cravings.baseActivities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.*
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.bumptech.glide.Glide
import com.example.cravings.R
import com.example.cravings.models.Product_Merchant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream

class EditProductActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var imagePreview: ImageView
    private lateinit var uploadPhotoBtn: Button
    private lateinit var nameField: EditText
    private lateinit var priceField: EditText
    private lateinit var quantityField: EditText
    private lateinit var descField: EditText
    private lateinit var updateBtn: Button
    private lateinit var deleteBtn: Button
    private lateinit var backButton: ImageButton

    // --- AWS + Firebase ---
    companion object {
        private const val AWS_ACCESS_KEY = "" // your key
        private const val AWS_SECRET_KEY = "" // your key
        private const val BUCKET_NAME = "craversbkt"
    }

    private val auth = FirebaseAuth.getInstance()
    private val sellerId = auth.currentUser?.uid ?: ""
    private lateinit var dbRef: DatabaseReference
    private var selectedProductId: String? = null
    private var currentImageUrl: String? = null
    private var selectedImageUri: Uri? = null
    private val TAG = "EditProductActivity"

    // --- AWS S3 client ---
    private val s3Client by lazy {
        AmazonS3Client(
            BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY),
            Region.getRegion(Regions.EU_NORTH_1)
        )
    }

    // --- Image Picker ---
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedImageUri = result.data?.data
                imagePreview.setImageURI(selectedImageUri)
            }
        }

    // --- onCreate ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        // Initialize views
        imagePreview = findViewById(R.id.imagePreview)
        uploadPhotoBtn = findViewById(R.id.btnUploadPhoto)
        nameField = findViewById(R.id.editProductName)
        priceField = findViewById(R.id.editProductPrice)
        quantityField = findViewById(R.id.editProductQuantity)
        descField = findViewById(R.id.editProductDescription)
        updateBtn = findViewById(R.id.btnUpdateProduct)
        deleteBtn = findViewById(R.id.btnDeleteProduct)
        backButton = findViewById(R.id.backButton)

        dbRef = FirebaseDatabase
            .getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users")
            .child("Merchant")
            .child(sellerId)
            .child("products")

        selectedProductId = intent.getStringExtra("productId")
        if (selectedProductId.isNullOrEmpty()) {
            Toast.makeText(this, "No product selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadProductDetails(selectedProductId!!)

        backButton.setOnClickListener { finish() }

        uploadPhotoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        updateBtn.setOnClickListener { updateProduct() }
        deleteBtn.setOnClickListener { deleteProduct() }
    }

    // --- Load product from Firebase ---
    private fun loadProductDetails(productId: String) {
        dbRef.child(productId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.getValue(Product_Merchant::class.java)
                if (product != null) {
                    nameField.setText(product.name)
                    priceField.setText(product.price.toString())
                    quantityField.setText(product.stock.toString())
                    descField.setText(product.description)
                    currentImageUrl = product.imageUrl

                    if (!product.imageUrl.isNullOrEmpty()) {
                        Glide.with(this@EditProductActivity)
                            .load(product.imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .into(imagePreview)
                    }
                } else {
                    Toast.makeText(this@EditProductActivity, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading product: ${error.message}")
            }
        })
    }

    // --- Update product ---
    private fun updateProduct() {
        val id = selectedProductId ?: return
        val name = nameField.text.toString().trim()
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val stock = quantityField.text.toString().toIntOrNull() ?: 0
        val desc = descField.text.toString().trim()

        if (name.isEmpty() || price <= 0) {
            Toast.makeText(this, "Enter valid product details", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            // Upload new image first
            uploadToS3(selectedImageUri!!) { newUrl ->
                // Optionally delete old one from AWS
                deleteOldImageFromS3(currentImageUrl)
                saveProductUpdates(id, name, price, stock, desc, newUrl)
            }
        } else {
            // Keep same image
            saveProductUpdates(id, name, price, stock, desc, currentImageUrl ?: "")
        }
    }

    // --- Save updated data to Firebase ---
    private fun saveProductUpdates(
        id: String,
        name: String,
        price: Double,
        stock: Int,
        desc: String,
        imageUrl: String
    ) {
        val updates = mapOf(
            "name" to name,
            "price" to price,
            "stock" to stock,
            "description" to desc,
            "imageUrl" to imageUrl
        )

        dbRef.child(id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Product updated ✅", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Delete product (Firebase + AWS) ---
    private fun deleteProduct() {
        val id = selectedProductId ?: return

        // Step 1: Delete from Firebase
        dbRef.child(id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Product deleted from database ✅", Toast.LENGTH_SHORT).show()

                // Step 2: Delete image from AWS (if exists)
                deleteOldImageFromS3(currentImageUrl)

                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Delete failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Upload to AWS S3 ---
    private fun uploadToS3(uri: Uri, onUploaded: (String) -> Unit) {
        val file = getFileFromUri(uri) ?: return
        TransferNetworkLossHandler.getInstance(applicationContext)

        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .s3Client(s3Client)
            .defaultBucket(BUCKET_NAME)
            .build()

        val key = "products/$sellerId/${System.currentTimeMillis()}.jpg"

        transferUtility.upload(BUCKET_NAME, key, file, CannedAccessControlList.PublicRead)
            .setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    if (state == TransferState.COMPLETED) {
                        val imageUrl = s3Client.getResourceUrl(BUCKET_NAME, key)
                        onUploaded(imageUrl)
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                override fun onError(id: Int, ex: Exception?) {
                    Log.e(TAG, "S3 upload failed", ex)
                    Toast.makeText(this@EditProductActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // --- Delete old image from S3 ---
    private fun deleteOldImageFromS3(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) return
        try {
            // Extract key correctly regardless of URL format
            val key = when {
                imageUrl.contains(".amazonaws.com/") ->
                    imageUrl.substringAfter(".amazonaws.com/").substringBefore("?")
                imageUrl.contains("s3.amazonaws.com/") ->
                    imageUrl.substringAfter("s3.amazonaws.com/").substringBefore("?")
                else -> null
            }

            if (key != null && s3Client.doesObjectExist(BUCKET_NAME, key)) {
                s3Client.deleteObject(DeleteObjectRequest(BUCKET_NAME, key))
                Log.i(TAG, "✅ Deleted from S3: $key")
            } else {
                Log.w(TAG, "⚠️ Image key not found or object doesn't exist: $key")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete S3 image: ${e.message}", e)
        }
    }


    // --- Convert URI to File ---
    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(cacheDir, "${System.currentTimeMillis()}.jpg")
        inputStream.copyTo(FileOutputStream(file))
        return file
    }
}
