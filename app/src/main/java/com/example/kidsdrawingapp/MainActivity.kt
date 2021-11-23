package com.example.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode == RESULT_OK && result.data!=null){
            val imageBackground : ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(result.data?.data)

        }
    }
    /** create an ActivityResultLauncher with MultiplePermissions since we are requesting
     * both read and write
     */
    private  val externalStorageReadResultLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(
                        this,
                        "Permission granted for read external storage",
                        Toast.LENGTH_LONG
                    ).show()
                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                }else { //check the permission name and perform the specific operation
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Oops you have juste denied the permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initiate the drawingView
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        // We find the color that we want from the xml file.
        var linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        // we can use the xml file as un array. Once chosen and found we convert the index into ImageButton
        // and we set it as pressed. We find it like this because the ImageButton doesn't have un id
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton //[1] is black
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)// the default color is black so the black button must be pressed on the start
        )
        val ibBrush: ImageButton =findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener(){
            showBrushSizeChooserDialog()
        }

        val ibGallery: ImageButton= findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener(){
            requestStoragePermission()

        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener(){
        drawingView?.onClickUndo()
        }
        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener(){
            drawingView?.onClickRedo()
        }
        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener(){
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView:FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    val myBitmap: Bitmap = getBitmapFromView(flDrawingView)
                    saveBitmapFile(myBitmap)
                }
            }

        }
    }
    /**
     * Method is used to launch the dialog to select different brush sizes.
     */
    private fun showBrushSizeChooserDialog() {
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        })

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()
    }

    /**
     * Method is called when color is clicked from pallet_normal.
     *
     * @param view ImageButton on which click took place.
     */
    fun paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){// we check if the pressed button is not the same
            val imageButton = view as ImageButton// It is a sort of assurance because just our ImageButtons have this onclick function
            val colorTag = imageButton.tag.toString()// We make the color into a string code
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal))

            mImageButtonCurrentPaint =view // we assign the new view to this value
        }
    }

    /** Checks if we have a permission to write on the storage */
    private fun isReadStorageAllowed():Boolean{
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)
        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }
    /** Request storage permission*/
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )){
            showRationaleDialog(
                "Kids Drawing App",
                "Kids Drawing App needs to Access Your External Storage")
        }else{
            externalStorageReadResultLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }

    }
    /**
     * Create bitmap from view and returns it
     */
    private fun getBitmapFromView(view:View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888)//We create a Bitmap to return
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background // we get the background rom the view
        if(bgDrawable !=null){ // if there is a background
            bgDrawable.draw(canvas)// we draw it into the canvas
        }else{
            canvas.drawColor(Color.WHITE)// if there is no background we draw white into the canvas
        }
        view.draw(canvas)// And then we draw the canvas into the view

        return returnedBitmap // finally we return the created bitmap
    }
    // Coroutine
    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result=""
        // This will run everything in the background
        withContext(Dispatchers.IO){
            if(mBitmap !=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90,bytes)//We compress the Bitmap

                    val f = File(externalCacheDir?.absoluteFile.toString()//Our root directory url
                    + File.separator + "KidDrawingApp_" + System.currentTimeMillis()/1000 + ".png") //We create the file location

                    val fo = FileOutputStream(f)//We create a file output stream
                    fo.write(bytes.toByteArray())// we write the array into the stream
                    fo.close()//we close the stream
                    result= f.absolutePath
                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                            "File saved successfully: $result",
                            Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity,
                                "Something went wrong wle saving the file",
                                Toast.LENGTH_SHORT).show()

                        }
                    }


                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result

    }
    /**  create progress dialog dialog */
    private  fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        /*Set the screen content from layout resource.
        * The resource will be inflated, adding all top-level views to the screen*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        // Start thr dialog and display it on screen
        customProgressDialog?.show()
    }
    /**  cancel progress dialog */
    private fun cancelProgressDialog(){
        if(customProgressDialog !=null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        /*MediaScannerConnection provides a way for applications to pass a
        newly created or downloaded media file to the media scanner service.
        The media scanner service will read metadata from the file and add
        the file to the media content provider.
        The MediaScannerConnectionClient provides an interface for the
        media scanner service to return the Uri for a newly scanned file
        to the client of the MediaScannerConnection class.*/

        /*scanFile is used to scan the file when the connection is established with MediaScanner.*/
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            // This is used for sharing the image after it has being stored in the storage.
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)// A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.
            shareIntent.type = "image/png" // The MIME type of the data being handled by this intent.
            startActivity(Intent.createChooser(shareIntent,"Share"))

        }

    }




    /**
     * Show rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private  fun showRationaleDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this,)
        builder.setTitle(title).setMessage(message).setPositiveButton("cancel"){
                dialog,_->dialog.dismiss()
        }
        builder.create().show()
    }


}