package com.example.samplesavebitmapproject

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    // 뷰를 비트맵으로 변환 후 이미지로 저장
    fun viewToBitmap(view: View, mode: String) {

        // 뷰를 비트맵으로 변환
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 안드로이드가 Q버전(API 29) 이상일 땐 그냥 실행
            saveImageOnAboveAndroidQ(bitmap)
        } else { // Q버전 이하일 경우, 저장소 권한을 얻어옴
            val writePermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (writePermission == PackageManager.PERMISSION_GRANTED) {
                saveImageOnUnderAndroidQ(bitmap)
            } else {
                val requestExternalStorageCode = 1

                val permissionStorage = arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

                ActivityCompat.requestPermissions(this, permissionStorage, requestExternalStorageCode)
            }
        }
    }

    // 안드로이드Q 이상에서 비트맵을 이미지로 저장
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveImageOnAboveAndroidQ(bitmap: Bitmap) {
        val fileName = "${System.currentTimeMillis()}_darwinT.jpeg"

        val contentValues = ContentValues()
        contentValues.apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/darwinT")
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val url = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if(url!=null) {
                val image = contentResolver.openFileDescriptor(url, "w", null)

                if(image!=null) {
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    // 비트맵을 FileOutputStream을 통해 compress 함
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 저장소 독점 해제
                    contentResolver.update(url, contentValues, null, null)
                }
            }


        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 안드로이드Q 이하에서 비트맵을 이미지로 저장
    fun saveImageOnUnderAndroidQ(bitmap: Bitmap) {
        val fileName = "${System.currentTimeMillis()}_darwinT.jpeg"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/darwinT"
        val dir = File(path)

        if(dir.exists().not()) {
            dir.mkdirs() // 경로가 없을 경우 폴더 생성
        }

        try {
            val fileItem = File("$dir/$fileName")
            fileItem.createNewFile() // 0KB 파일 생성

            val fos = FileOutputStream(fileItem)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            // FileOutputStream을 통해 Bitmap 압축

            fos.close()

            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}