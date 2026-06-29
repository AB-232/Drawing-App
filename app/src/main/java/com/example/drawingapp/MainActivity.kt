package com.example.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class MainActivity : AppCompatActivity() {

    // Main drawing area
    private lateinit var drawingView: DrawingView

    // Color buttons
    private lateinit var purpleButton: ImageButton
    private lateinit var redButton: ImageButton
    private lateinit var pickedColorButton: ImageButton
    private lateinit var blueButton: ImageButton
    private lateinit var greenButton: ImageButton

    // Action buttons
    private lateinit var undoButton: ImageButton
    private lateinit var brushSizeButton: ImageButton
    private lateinit var colorPickerButton: ImageButton
    private lateinit var galleryButton: ImageButton
    private lateinit var saveButton: ImageButton

    // Stores selected custom color
    private var pickedColor = Color.WHITE

    // Handle gallery image selection result
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            findViewById<ImageView>(R.id.gallery_image).setImageURI(result.data?.data)
        }

    // Handle storage permission results
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted && permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission $permissionName granted",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                } else if (isGranted && permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission $permissionName granted",
                        Toast.LENGTH_SHORT
                    )
                        .show()

                    CoroutineScope(IO).launch {
                        saveImage(convertLayoutToBitmap(findViewById(R.id.drawing_part)))

                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission denied",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        drawingView = findViewById(R.id.drawing_view)

        purpleButton = findViewById(R.id.purple_button)
        redButton = findViewById(R.id.red_button)
        pickedColorButton = findViewById(R.id.picked_color_button)
        blueButton = findViewById(R.id.blue_button)
        greenButton = findViewById(R.id.green_button)

        purpleButton.background = createDynamicButtonBg(getColor(R.color.my_purple), false)
        redButton.background = createDynamicButtonBg(getColor(R.color.my_red), false)
        pickedColorButton.background = createDynamicButtonBg(pickedColor, false)
        blueButton.background = createDynamicButtonBg(getColor(R.color.my_blue), false)
        greenButton.background = createDynamicButtonBg(getColor(R.color.my_green), false)

        undoButton = findViewById(R.id.undo_button)
        brushSizeButton = findViewById(R.id.brush_size_button)
        colorPickerButton = findViewById(R.id.color_picker_button)
        galleryButton = findViewById(R.id.gallery_button)
        saveButton = findViewById(R.id.save_button)

        // Handle color selection
        purpleButton.setOnClickListener {
            resetButtonsBackground()
            purpleButton.background = createDynamicButtonBg(getColor(R.color.my_purple), true)
            drawingView.color = getColor(R.color.my_purple)
        }

        redButton.setOnClickListener {
            resetButtonsBackground()
            redButton.background = createDynamicButtonBg(getColor(R.color.my_red), true)
            drawingView.color = getColor(R.color.my_red)
        }

        pickedColorButton.setOnClickListener {
            resetButtonsBackground()
            drawingView.color = pickedColor
            pickedColorButton.background = createDynamicButtonBg(pickedColor, true)
        }

        blueButton.setOnClickListener {
            resetButtonsBackground()
            blueButton.background = createDynamicButtonBg(getColor(R.color.my_blue), true)
            drawingView.color = getColor(R.color.my_blue)
        }

        greenButton.setOnClickListener {
            resetButtonsBackground()
            greenButton.background = createDynamicButtonBg(getColor(R.color.my_green), true)
            drawingView.color = getColor(R.color.my_green)
        }

        undoButton.setOnClickListener { drawingView.undoPath() }
        brushSizeButton.setOnClickListener { showChangeBrushSizeDialog() }
        colorPickerButton.setOnClickListener { showColorPickerDialog() }

        // Handle gallery image selection
        galleryButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestStoragePermission()

            } else {
                val pickIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            }

        }

        // Handle image saving
        saveButton.setOnClickListener {

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestStoragePermission()
            } else {
                CoroutineScope(IO).launch {
                    saveImage(convertLayoutToBitmap(findViewById(R.id.drawing_part)))

                }
            }
        }
    }


    // Request storage permissions
    private fun requestStoragePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog()
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // Show permission explanation dialog
    private fun showRationaleDialog() {
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.setTitle("Storage Permission")
            .setMessage("We need this permission in order to access the internal storage")
            .setPositiveButton("YES") { dialog, _ ->
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
                dialog.dismiss()
            }
        dialog.create().show()
    }

    // Reset all color button states
    private fun resetButtonsBackground() {

        purpleButton.background = createDynamicButtonBg(getColor(R.color.my_purple), false)
        redButton.background = createDynamicButtonBg(getColor(R.color.my_red), false)
        pickedColorButton.background = createDynamicButtonBg(pickedColor, false)
        blueButton.background = createDynamicButtonBg(getColor(R.color.my_blue), false)
        greenButton.background = createDynamicButtonBg(getColor(R.color.my_green), false)
    }

    // Open brush size dialog
    private fun showChangeBrushSizeDialog() {

        val brushDialog = Dialog(this@MainActivity)
        brushDialog.setContentView(R.layout.brush_size_dialog)

        val seekBar: SeekBar = brushDialog.findViewById(R.id.change_brush_siz_seekbar)
        val textView: TextView = brushDialog.findViewById(R.id.change_brush_size_textview)

        textView.text = drawingView.initBrushSizeForShow.toString()
        seekBar.progress = drawingView.initBrushSizeForShow

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                textView.text = p1.toString()
                drawingView.changeBrushSize(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        brushDialog.show()
    }


    // Open color picker dialog
    private fun showColorPickerDialog() {

        val colorPickerDialog = AmbilWarnaDialog(
            this@MainActivity,
            pickedColor,
            object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {
                }

                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    resetButtonsBackground()
                    pickedColor = color
                    drawingView.color = pickedColor
                    pickedColorButton.background = createDynamicButtonBg(pickedColor, true)
                }
            })

        colorPickerDialog.show()
    }

    // Convert layout into bitmap
    private fun convertLayoutToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    // Save bitmap as image file
    private suspend fun saveImage(bitmap: Bitmap) {

        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val myDir = File(root, "saved_images")
        myDir.mkdirs()

        val generator = Random()
        var number = 1000000
        number = generator.nextInt(number)
        val outPutFile = File(myDir, "Image-$number.jpg")

        if (outPutFile.exists()) {
            outPutFile.delete()
        } else {
            try {
                val out = FileOutputStream(outPutFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.stackTrace

            }
        }
        withContext(Main) {
            Toast.makeText(
                this@MainActivity,
                "${outPutFile.absolutePath} saved !",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Create dynamic button background
    private fun createDynamicButtonBg(buttonColor: Int, isChecked: Boolean): GradientDrawable {

        val myDrawable = GradientDrawable()

        myDrawable.shape = GradientDrawable.RECTANGLE
        myDrawable.setColor(buttonColor)

        val scale = resources.displayMetrics.density

        myDrawable.cornerRadius = (scale * 7)
        if (isChecked) myDrawable.setStroke(
            (1.5 * scale).toInt(),
            Color.BLACK
        ) else myDrawable.setStroke((.5 * scale).toInt(), Color.GRAY)

        return myDrawable
    }
}