package com.example.drawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // Stores the drawing area as a bitmap
    private lateinit var bitmap: Bitmap

    // Paint used to draw the bitmap onto the canvas
    private lateinit var canvasBitmap: Paint

    // Path that the user is currently drawing
    private lateinit var drawPath: FingerPath

    // List of paths drawn by the user
    private val paths = mutableListOf<FingerPath>()

    // Paint object used for drawing and its properties
    private lateinit var drawPaint: Paint
    var color = Color.BLACK
    var initBrushSizeForShow = 15
    private var brushSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        15F,
        resources.displayMetrics
    ) // Convert brush size from dp to pixels based on screen density

    init {

        setupDrawingView()
    }

    private fun setupDrawingView() {

        // Initialize drawing paint
        drawPaint = Paint()
        drawPaint.color = color
        drawPaint.strokeWidth = brushSize
        drawPaint.strokeCap = Paint.Cap.ROUND
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.style = Paint.Style.STROKE

        // Create paint for bitmap rendering
        canvasBitmap = Paint(Paint.DITHER_FLAG)

        // Initialize the first drawing path
        drawPath = FingerPath(color, brushSize)
    }

    // Create a bitmap matching the view dimensions
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

        super.onSizeChanged(w, h, oldw, oldh)
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        super.onTouchEvent(event)
        val x = event?.x
        val y = event?.y

        // Handle user touch events
        when (event?.action) {

            MotionEvent.ACTION_DOWN -> {
                // Clear previous path data
                drawPath.reset()

                // Assign current drawing properties to the path
                drawPath.color = color
                drawPath.brushSize = brushSize

                // Set the starting point of the path
                drawPath.moveTo(x!!, y!!)
            }

            MotionEvent.ACTION_MOVE -> {

                // Extend the path to the current touch position
                drawPath.lineTo(x!!, y!!)
            }

            MotionEvent.ACTION_UP -> {
                // Save completed path
                paths.add(drawPath)

                // Create a new path for the next drawing
                drawPath = FingerPath(color, brushSize)
            }

            // Return false for unsupported touch events
            else -> false
        }
        // Refresh the view and redraw content
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the bitmap background on the canvas
        canvas?.drawBitmap(bitmap, 0F, 0F, canvasBitmap)

        // Draw all previous paths
        for (path in paths) {
            drawPaint.color = path.color
            drawPaint.strokeWidth = path.brushSize
            canvas?.drawPath(path, drawPaint)
        }

        // Draw current path
        if (!drawPath.isEmpty) {
            drawPaint.color = drawPath.color
            drawPaint.strokeWidth = drawPath.brushSize
            canvas?.drawPath(drawPath, drawPaint)
        }
    }

    fun undoPath() {

        // Remove the last path if available and refresh the view
        if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex)
        invalidate()
    }

    fun changeBrushSize(newSize: Int) {

        // Update brush size with the new selected value

        brushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize.toFloat(),
            resources.displayMetrics
        ) // Convert brush size from dp to pixels based on screen density

        initBrushSizeForShow = newSize
        drawPaint.strokeWidth = brushSize
    }

    // Custom path class that stores drawing properties
    inner class FingerPath(var color: Int, var brushSize: Float) : Path()
}