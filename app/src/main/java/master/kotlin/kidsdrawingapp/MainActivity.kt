package master.kotlin.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val drawingView = findViewById<DrawingView>(R.id.drawing_view)
        drawingView.setSizeForBrush(20.toFloat())

//        val llPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
//        mImageButtonCurrentPaint == llPaintColors[1] as ImageButton
        mImageButtonCurrentPaint == ll_paint_colors.getChildAt(1) as ImageButton
        mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )
        val ibBrush = findViewById<ImageButton>(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibGallery = findViewById<ImageButton>(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            if(isReadStorageAllowed()){
                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)
            }else{
                requestStoragePermission()
            }
        }

        val ibUndo = findViewById<ImageButton>(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView.onClickUndo()
        }

        val ibSave = findViewById<ImageButton>(R.id.ib_save)
        ibSave.setOnClickListener {
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }else{
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY){
                try{
                    if(data!!.data != null){
                        val ivBackground = findViewById<ImageView>(R.id.iv_background)
                        ivBackground.visibility = View.VISIBLE
                        ivBackground.setImageURI(data.data)
                    }else{
                        Toast.makeText(this@MainActivity,"Error in parsing the image or its corrupted ",Toast.LENGTH_SHORT).show()
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity,"Oops! ",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
//        val drawingView = findViewById<DrawingView>(R.id.drawing_view)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
//        val ibSmallBrush = findViewById<ImageButton>(R.id.ib_small_brush)
//        val ibMediumBrush = findViewById<ImageButton>(R.id.ib_large_brush)
//        val ibLargeBrush = findViewById<ImageButton>(R.id.ib_medium_brush)
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawing_view!!.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawing_view!!.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawing_view!!.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    // 이미지 버튼 클릭
    fun paintClicked(view:View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
//            val drawingView = findViewById<DrawingView>(R.id.drawing_view)
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this, "Need permission to add a Background", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this@MainActivity,
                "Permission granted now you can read the storage",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(
                        this@MainActivity,
                        "you just denied!",
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    // 127
    private fun getBitmapFromView(view:View):Bitmap{
        val returnedBitmap =
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
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

    private inner class BitmapAsyncTask(val mBitmap:Bitmap):AsyncTask<Any, Void, String>(){

        private lateinit var mProgressDialog:Dialog
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if(!result!!.isEmpty()){
                Toast.makeText(
                        this@MainActivity,
                        "File saved successfully : $result",
                        Toast.LENGTH_LONG
                ).show()
            }else{
                Toast.makeText(
                        this@MainActivity,
                        "Something went wrong while saving the file",
                        Toast.LENGTH_LONG
                ).show()
            }
            // 130
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"

                startActivity(Intent.createChooser(shareIntent,"Share"))
            }

        }

        // 128
        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(
                            Bitmap.CompressFormat.PNG,90, bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            +File.separator+"kidDrawingApp_"
                            +System.currentTimeMillis()/1000+".png")
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }

            return result
        }

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        // 129
        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }
        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }
    }

    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}