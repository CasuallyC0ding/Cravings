package com.example.cravings.baseActivities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.net.Uri
import android.view.View
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cravings.R
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole: String? = null

    companion object {
        private const val AWS_ACCESS_KEY = "AKIA6GUTHW7WYQ5CO7GP"
        private const val AWS_SECRET_KEY = "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"
        private const val BUCKET_NAME = "craversbkt"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userRole = intent.getStringExtra("userRole")

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val greetingText = findViewById<TextView>(R.id.greetingText)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val shopNameInput = findViewById<EditText>(R.id.shopNameInput)
        val shopNameLabel = findViewById<TextView>(R.id.shopNameLabel)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val signOutBtn = findViewById<Button>(R.id.signOutBtn)
        val editProfileBtn = findViewById<ImageButton>(R.id.editProfileBtn)
        val profileImageView = findViewById<ImageView>(R.id.profileImage)

        editProfileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        val uid = auth.currentUser?.uid
        if (uid == null || userRole == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (userRole == "Merchant") {
            shopNameInput.visibility = View.VISIBLE
            shopNameLabel.visibility = View.VISIBLE
        } else {
            shopNameInput.visibility = View.GONE
            shopNameLabel.visibility = View.GONE
        }

        // ✅ Load all user data including image
        database.reference.child("users").child(userRole!!).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").value?.toString() ?: "User"
                        val phone = snapshot.child("phone").value?.toString() ?: ""
                        val shopName = snapshot.child("shopName").value?.toString() ?: ""
                        val imageUrl = snapshot.child("profileImage").value?.toString()
                            ?: snapshot.child("profilePicUrl").value?.toString()
                        val firstName = name.split(" ").firstOrNull() ?: name

                        greetingText.text = "Hello, $firstName 👋"
                        nameInput.setText(name)
                        phoneInput.setText(phone)

                        if (userRole == "Merchant") {
                            shopNameInput.setText(shopName)
                        }

                        // ✅ Load profile image if available
                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(this@ProfileActivity)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder) // optional placeholder
                                .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable disk caching
                                .skipMemoryCache(true) // Disable memory caching
                                .into(profileImageView)
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "No data found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        saveBtn.setOnClickListener {
            val updatedName = nameInput.text.toString().trim()
            val updatedPhone = phoneInput.text.toString().trim()

            if (updatedName.isEmpty() || updatedPhone.isEmpty()) {
                Toast.makeText(this, "Please fill out all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mutableMapOf<String, Any>(
                "name" to updatedName,
                "phone" to updatedPhone
            )

            if (userRole == "Merchant") {
                val updatedShopName = shopNameInput.text.toString().trim()
                if (updatedShopName.isEmpty()) {
                    Toast.makeText(this, "Please enter your shop name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                updates["shopName"] = updatedShopName
            }

            database.reference.child("users").child(userRole!!).child(uid).updateChildren(updates)
                .addOnSuccessListener {
                    val firstName = updatedName.split(" ").firstOrNull() ?: updatedName
                    greetingText.text = "Hello, $firstName 👋"
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        signOutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                findViewById<ImageView>(R.id.profileImage).setImageURI(imageUri)
                uploadToS3(imageUri)
            }
        }
    }

    private fun uploadToS3(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val file = getFileFromUri(uri) ?: run {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
            return
        }

        val credentials = BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
        val s3 = AmazonS3Client(credentials, Region.getRegion(Regions.EU_NORTH_1))
        TransferNetworkLossHandler.getInstance(applicationContext)

        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .s3Client(s3)
            .defaultBucket(BUCKET_NAME)
            .build()

        val key = "profile_pictures/$uid.jpg"
        val uploadObserver = transferUtility.upload(BUCKET_NAME, key, file, CannedAccessControlList.PublicRead)

        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
                    val imageUrl = s3.getResourceUrl(BUCKET_NAME, key)
                    val userRef = database.reference.child("users").child(userRole!!).child(uid)
                    val updates = mapOf("profileImage" to imageUrl)
                    userRef.updateChildren(updates)
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
            override fun onError(id: Int, ex: Exception?) {
                Toast.makeText(this@ProfileActivity, "Upload failed: ${ex?.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()
        return file
    }
}
