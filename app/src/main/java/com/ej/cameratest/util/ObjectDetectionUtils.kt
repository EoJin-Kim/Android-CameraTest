package com.ej.cameratest.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF

object ObjectDetectionUtils {

    // 이미지에서 탐지된 객체의 영역을 잘라내는 메서드
    fun cropDetectedObject(inputImage: Bitmap, boundingBox: RectF): Bitmap {
        val left = Math.max(boundingBox.left.toInt(), 0)
        val top = Math.max(boundingBox.top.toInt(), 0)
        val right = Math.min(boundingBox.right.toInt(), inputImage.width)
        val bottom = Math.min(boundingBox.bottom.toInt(), inputImage.height)

        val width = right - left
        val height = bottom - top

        // 객체 영역을 잘라낸 비트맵 생성
        val croppedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedBitmap)
        canvas.drawBitmap(inputImage, -left.toFloat(), -top.toFloat(), null)

        return croppedBitmap
    }
}