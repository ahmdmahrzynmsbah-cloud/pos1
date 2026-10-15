package com.example.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

fun saveImageToInternalStorage(context: Context, uri: Uri): String {
  try {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return uri.toString()
    val fileName = "img_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)
    val outputStream = FileOutputStream(file)
    
    val buffer = ByteArray(4 * 1024)
    var read: Int
    while (inputStream.read(buffer).also { read = it } != -1) {
      outputStream.write(buffer, 0, read)
    }
    
    outputStream.flush()
    outputStream.close()
    inputStream.close()
    
    return Uri.fromFile(file).toString()
  } catch (e: Exception) {
    e.printStackTrace()
    return uri.toString() // Fallback to original URI if it fails
  }
}
