package com.example.cravings

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var profileImage: ImageView
    private lateinit var editProfileBtn: ImageButton
    private val PICK_IMAGE_REQUEST = 1001
    private var selectedImageUri: Uri? = null

    private val AWS_ACCESS_KEY = ""      // 🔹 Replace
    private val AWS_SECRET_KEY = ""      // 🔹 Replace
    private val BUCKET_NAME = "craversbkt"       // 🔹 Replace

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
        ).reference

        val greetingText = findViewById<TextView>(R.id.greetingText)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val signOutBtn = findViewById<Button>(R.id.signOutBtn)

        profileImage = findViewById(R.id.profileImage)
        editProfileBtn = findViewById(R.id.editProfileBtn)

        val uid = auth.currentUser?.uid
        val userRole = intent.getStringExtra("userRole") ?: "users"

        if (uid == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 🔹 Load user data based on role
        database.child("users").child(userRole).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").value?.toString() ?: "User"
                        val phone = snapshot.child("phone").value?.toString() ?: ""
                        val firstName = name.split(" ").firstOrNull() ?: name
                        greetingText.text = "Hello, $firstName 👋"
                        nameInput.setText(name)
                        phoneInput.setText(phone)
                    } else {
                        Toast.makeText(this@HomeActivity, "No data found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })



        // 🔹 Save updates
        saveBtn.setOnClickListener {
            val updatedName = nameInput.text.toString().trim()
            val updatedPhone = phoneInput.text.toString().trim()

            if (updatedName.isEmpty() || updatedPhone.isEmpty()) {
                Toast.makeText(this, "Please fill out both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf("name" to updatedName, "phone" to updatedPhone)
            database.child(userRole).child(uid).updateChildren(updates)
                .addOnSuccessListener {
                    val firstName = updatedName.split(" ").firstOrNull() ?: updatedName
                    greetingText.text = "Hello, $firstName 👋"
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // 🔹 Sign out
        signOutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Edit profile image
        editProfileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                profileImage.setImageURI(uri)
                uploadToS3(uri)
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }


    private fun uploadToS3(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val file = getFileFromUri(uri) ?: run {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
            return
        }

        val credentials = BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
        val s3 = AmazonS3Client(credentials, Region.getRegion(Regions.EU_WEST_1))
        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .s3Client(s3)
            .build()

        val key = "profile_pictures/$uid.jpg"
        val uploadObserver = transferUtility.upload(BUCKET_NAME, key, file)

        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
                    val imageUrl = s3.getResourceUrl(BUCKET_NAME, key)
                    database.child("users").child(uid).child("profileImage").setValue(imageUrl)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@HomeActivity,
                                "Profile picture updated!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
            override fun onError(id: Int, ex: Exception?) {
                Toast.makeText(
                    this@HomeActivity,
                    "Upload failed: ${ex?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}