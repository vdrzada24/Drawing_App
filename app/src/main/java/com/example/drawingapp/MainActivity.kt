package com.example.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? =null
    private var mImageButtonCurrentPaint : ImageButton? = null
    var customProgressDialog : Dialog? = null

    var colorPicker : ImageButton ?= null

    var mSelectedColor = Color.GRAY



    // launcher to select the particular image from gallery and set
    // it as imageview...
    val openGalleryLauncher : ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
                if(result.resultCode == RESULT_OK && result.data!=null){
                    val imageBackGround: ImageView = findViewById(R.id.iv_background)

                    imageBackGround.setImageURI(result.data?.data)
                }
            }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if(!isGranted){
                    //Toast.makeText(this,"Permission granted now you can read the storage files",Toast.LENGTH_LONG).show()

                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)


                }else{
                    if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,"Oops you just denied the permission",Toast.LENGTH_LONG).show()

                    }
                }
            }

        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)


        )
        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }

        val ib_brush: ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener{
            showBrushDialog()
        }

        val ib_Undo: ImageButton = findViewById(R.id.ib_undo)
        ib_Undo.setOnClickListener{
            drawingView?.onCLickUndo()

        }

        val ib_Save: ImageButton = findViewById(R.id.ib_save)
        ib_Save.setOnClickListener{

            showProgressDialog()

            lifecycleScope.launch {
                val flDrawingView:FrameLayout = findViewById(R.id.fl_drawing_view_container)
                saveBitmapFile(getBitmapFromView(flDrawingView))
            }



        }

        val removeBg : ImageButton = findViewById(R.id.ib_remove_bg)
        removeBg.setOnClickListener{
            // remember here the it is for the image button...
            val imageBackGround: ImageView = findViewById(R.id.iv_background)

            imageBackGround.setImageResource(R.drawable.bg)


        }



        colorPicker = findViewById(R.id.color_picker)

        colorPicker?.setOnClickListener{
            paintClicked(it)
            pickerDialog()
        }











    }

    private fun pickerDialog(){
        val colorPickerDialog = AmbilWarnaDialog(this,mSelectedColor,object : AmbilWarnaDialog.OnAmbilWarnaListener{
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {

                mSelectedColor = color
                drawingView?.setColor("#${Integer.toHexString(color)}")
                colorPicker?.setBackgroundColor(mSelectedColor)



            }

            override fun onCancel(dialog: AmbilWarnaDialog?) {

            }
        })

        colorPickerDialog.show()
    }


   

//    private fun isReadStorageAllowed(): Boolean{
//        val result = ContextCompat.checkSelfPermission(this,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        )
//
//        return result == PackageManager.PERMISSION_GRANTED
//
//    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }

    }

    private fun shareImage(result:String){

//        MediaScannerConnection.scanFile(this, arrayOf(result),null){
//            path,uri ->
//
//
//        }

        val file = File(result)

        if (!file.exists()) {
            Toast.makeText(this, "File does not exist", Toast.LENGTH_LONG).show()
            return
        }


        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, "com.example.drawingapp.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        //val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
        shareIntent.type = "image/png"
        startActivity(Intent.createChooser(shareIntent,"Share"))
    }


    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            ){
                showRationaleDialog("Drawing App","Drawing App" + " needs to Access your External Storage")

        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }

    }

    // to store the drawing we need to convert the drawing in Bitmap
    // as the drawing is a view Type so we convert the view in Bitmap...

    private fun getBitmapFromView(view:View): Bitmap {

        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height,Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap


    }

    private suspend fun saveBitmapFile(mBitmap:Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)


                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawingApp_" + System.currentTimeMillis()/1000 + ".png"

                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()

                            shareImage(result)

                        }else{

                            Toast.makeText(this@MainActivity,
                                "Something went wrong",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                }
                catch(e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }

        }
        return result



    }







    private fun showBrushDialog(){

        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")

        val extraSmallBtn : ImageButton = brushDialog.findViewById(R.id.ib_extra_small_brush)
        extraSmallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }


        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)


        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)

        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }





    fun paintClicked(view: View){

        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }


    }

    private fun showRationaleDialog(title:String,message:String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        .setMessage(message)
            .setPositiveButton("Cancel"){ dialog, _ ->
                dialog.dismiss()

            }

        builder.create().show()

    }

}


