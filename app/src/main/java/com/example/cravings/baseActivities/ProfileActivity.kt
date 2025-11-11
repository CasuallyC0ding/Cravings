package com.example.cravings.baseActivities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole: String? = null

    companion object {
        private const val AWS_ACCESS_KEY = ""   // ← keep yours
        private const val AWS_SECRET_KEY = ""   // ← keep yours
        private const val BUCKET_NAME = "craversbkt"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userRole = intent.getStringExtra("userRole")

        // ✅ Change background based on user type
        val bgImage = findViewById<ImageView>(R.id.bgImage)
        if (userRole == "Customer") {
            bgImage.setImageResource(R.drawable.food_pattern_bg_light)
        } else {
            bgImage.setImageResource(R.drawable.food_pattern_bg_dark)
        }

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

        // Select new profile picture
        editProfileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        val uid = auth.currentUser?.uid ?: return

        // Show extra merchant fields
        if (userRole == "Merchant") {
            shopNameInput.visibility = View.VISIBLE
            shopNameLabel.visibility = View.VISIBLE
        }

        // Load user data
        database.reference.child("users").child(userRole!!).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").value?.toString() ?: "User"
                    val phone = snapshot.child("phone").value?.toString() ?: ""
                    val shopName = snapshot.child("shopName").value?.toString() ?: ""
                    val imageUrl = snapshot.child("profileImage").value?.toString()

                    val firstName = name.split(" ").first()
                    greetingText.text = "Hello, $firstName 👋"
                    nameInput.setText(name)
                    phoneInput.setText(phone)

                    if (userRole == "Merchant") shopNameInput.setText(shopName)

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(profileImageView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // ✅ Save Changes + Update UI + Show Toast
        saveBtn.setOnClickListener {
            val updatedName = nameInput.text.toString().trim()
            val updatedPhone = phoneInput.text.toString().trim()

            if (updatedName.isEmpty() || updatedPhone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mutableMapOf<String, Any>(
                "name" to updatedName,
                "phone" to updatedPhone
            )

            if (userRole == "Merchant") {
                val updatedShopName = shopNameInput.text.toString().trim()
                updates["shopName"] = updatedShopName
            }

            database.reference.child("users").child(userRole!!).child(uid)
                .updateChildren(updates)
                .addOnSuccessListener {
                    val firstName = updatedName.split(" ").first()
                    greetingText.text = "Hello, $firstName 👋"
                    Toast.makeText(this, "Changes saved ✅", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Save failed ❌", Toast.LENGTH_SHORT).show()
                }
        }

        // Sign Out
        signOutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure?")
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
            uploadToS3(data.data!!)
            findViewById<ImageView>(R.id.profileImage).setImageURI(data.data)
        }
    }

    private fun uploadToS3(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val file = getFileFromUri(uri) ?: return

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
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return file
    }
}
