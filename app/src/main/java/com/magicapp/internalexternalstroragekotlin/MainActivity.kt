

package com.magicapp.internalexternalstroragekotlin

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.*
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private val isPersistent : Boolean = true
    private val isInternal : Boolean = false
    private var readPermissionGranted : Boolean = false
    private var writePermissionGranted : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        checkStoragePaths()
        createInternalFIle()
        requestPermissions()
    }

    private fun initViews() {

        val btn_take_photo = findViewById<Button>(R.id.btn_take_photo)
        val btn_delete_internal = findViewById<Button>(R.id.btn_delete_inter)
        val btn_delete_external = findViewById<Button>(R.id.btn_delete_exter)

        btn_delete_internal.setOnClickListener {
            deleteTempFolder("Eshonxo'ja")
        }

        val btn_save_iternal = findViewById<Button>(R.id.btn_sav_inter)
        btn_save_iternal.setOnClickListener {
            saveInternalFile("Eshonxo'ja")
        }
        val btn_read_internal = findViewById<Button>(R.id.btn_read_inter)
        btn_read_internal.setOnClickListener {
            readInternalFile()
        }

        val btn_save_external = findViewById<Button>(R.id.btn_sav_exter)
        btn_save_external.setOnClickListener {
            saveExternalFile("Eshonxo'ja")
        }

        val btn_read_external = findViewById<Button>(R.id.btn_read_exter)
        btn_read_external.setOnClickListener {
            readExternalFile()
        }

        btn_take_photo.setOnClickListener {
            takePhoto.launch()
        }

    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val filename = UUID.randomUUID().toString()
        
        val isPhotoSaved = if (isInternal){
            savePhotoToInternalStorage(filename, bitmap!!)
        }else {
            if (writePermissionGranted) {
                savePhotoToExternalStorage(filename, bitmap!!)
            } else {
                false
            }
        }
        if (isPhotoSaved) {
            Toast.makeText(
                this,
                "Photo saved successfully",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Failed to save photo",
                Toast.LENGTH_SHORT
            ).show()
        }
        
    }

    private fun savePhotoToExternalStorage(filename: String, bitmap: Bitmap): Boolean {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }
        return try {
            contentResolver.insert(collection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't bitmap.")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
        }



    private fun savePhotoToInternalStorage(filename: String, bitmap: Bitmap): Boolean {
        return try {
            openFileOutput("$filename.jpg", MODE_PRIVATE).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw  IOException("Couldn't bitmap.")
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

//    var dir = filesDir
//    var file = File(dir, "my_filename")
//    var deleted = file.delete()


    private fun deleteTempFolder(dir: String) {
        val myDir = File(Environment.getExternalStorageDirectory().toString() + "/" + dir)
        if (myDir.isDirectory) {
            val children = myDir.list()
            for (i in children.indices) {
                File(myDir, children[i]).delete()
            }
        }
    }

    @SuppressLint("LongLogTag")
    private fun readExternalFile() {
        val fileName = "pdp_external.txt"
        try {
            val fileInputStream: FileInputStream
            fileInputStream = if (isPersistent) {
                openFileInput(fileName)
            } else {
                val file = File(cacheDir, fileName)
                FileInputStream(file)
            }
            val inputStreamReader = InputStreamReader(fileInputStream, Charset.forName("UTF-8"))
            val lines: MutableList<String?> = ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()
            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }
            val readText = TextUtils.join("/n", lines)
            Toast.makeText(
                this,
                "Read from file %s successful",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Read from file %s failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveExternalFile(data: String) {
        val fileName = "pdp_external.txt"
        val file : File
        file = if (isPersistent) {
            File (getExternalFilesDir(null), fileName)
        } else {
            File(externalCacheDir, fileName)
        }
        try {
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Toast.makeText(
                this,
                "Write to %s successful",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Write to %s failed",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    @SuppressLint("LongLogTag")
    private fun readInternalFile() {
        val fileName = "pdp_internal.txt"
        try {
            val fileInputStream: FileInputStream
            fileInputStream = if (isPersistent) {
                openFileInput(fileName)
            } else {
                val file = File(cacheDir, fileName)
                FileInputStream(file)
            }
            val inputStreamReader = InputStreamReader(fileInputStream, Charset.forName("UTF-8"))
            val lines: MutableList<String?> = ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()
            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }
            val readText = TextUtils.join("/n", lines)
            Toast.makeText(
                this,
                "Read from file %s successful",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Read from file %s failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveInternalFile(data: String) {
        val fileName = "pdp_internal.txt"
        try {
            val fileOutputStream: FileOutputStream
            fileOutputStream = if (isPersistent) {
                openFileOutput(fileName, MODE_PRIVATE)
            }else {
                val file = File(cacheDir, fileName)
                FileOutputStream(file)
            }
            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Toast.makeText(
                this,
                "Write to %s successful",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Write to %s failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("LongLogTag")
    private fun createInternalFIle() {
        val fileName = "pdp_internal.txt"
        val file: File
        file = if (isPersistent) {
            File(filesDir, fileName)
        } else {
            File(cacheDir, fileName)
        }
        if (!file.exists()){
            try {
                file.createNewFile()
                Toast.makeText(
                    this,
                    "File %s has been created",
                    Toast.LENGTH_SHORT
                ).show()
            }catch (e:Exception){
                Toast.makeText(
                    this,
                    "File %s creation failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }else{
            Toast.makeText(
                this,
                "File %s already exists",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun checkStoragePaths() {
        val internal_m1 = getDir("custom", 0)
        val internal_m2 = filesDir

        val external_m1 = getExternalFilesDir(null)
        val external_m2 = externalCacheDir
        val external_m3 = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        Log.d("StorageActivity", internal_m1.absolutePath)
        Log.d("StorageActivity", internal_m2.absolutePath)
        Log.d("StorageActivity", external_m1!!.absolutePath)
        Log.d("StorageActivity", external_m2!!.absolutePath)
        Log.d("StorageActivity", external_m3!!.absolutePath)
    }

    private fun requestPermissions() {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val minSDK29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSDK29

        val permissionsToRequest = mutableListOf<String>()
        if (!readPermissionGranted)
            permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!writePermissionGranted)
            permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionsToRequest.isNotEmpty())
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]
                ?: readPermissionGranted
            writePermissionGranted = permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE]
                ?: writePermissionGranted
            if (readPermissionGranted) Toast.makeText(
                this,
                "ReadExternalStorage",
                Toast.LENGTH_SHORT
            ).show()
            if (writePermissionGranted) Toast.makeText(
                this,
                "WriteExternalStorage",
                Toast.LENGTH_SHORT
            ).show()
        }
}



