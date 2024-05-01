package com.dicoding.asclepius.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri


fun toBitmap(context: Context, image: Uri): Bitmap {
    val contentResolver = context.contentResolver

    val inputStream = contentResolver.openInputStream(image)
    val bitmap = BitmapFactory.decodeStream(inputStream)

    inputStream?.close()
    return bitmap
}
