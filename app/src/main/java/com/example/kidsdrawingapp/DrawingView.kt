package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {
    /** An variable of CustomPath inner class to use it further. */
    private var mDrawPath: CustomPath? = null
    /** An instance of the Bitmap. */
    private var mCanvasBitmap: Bitmap? = null
    /** The Paint class holds the style and color information about how to draw geometries, text and bitmaps. */
    private var mDrawPaint: Paint? = null
    /** An instance of canvas paint view. */
    private var mCanvasPaint: Paint? = null
    /** A variable for stroke/brush size to draw on the canvas. */
    private var mBrushSize: Float = 0.toFloat()
    /** Default drawing color */
    private var color = Color.BLACK
    /**
     * A variable for canvas which will be initialized later and used.
     *
     *The Canvas class holds the "draw" calls. To draw something, you need 4 basic components: A Bitmap to hold the pixels, a Canvas to host
     * the draw calls (writing into the bitmap), a drawing primitive (e.g. Rect,
     * Path, text, Bitmap), and a paint (to describe the colors and styles for the
     * drawing)
     */
    private var canvas: Canvas? = null

    /** ArrayList for Paths */
    private val mPaths = ArrayList<CustomPath>() // ArrayList for Paths
    private val mUndoPaths = ArrayList<CustomPath>() // Array with Undo

    init {
        setUpDrawing()
    }

    fun onClickUndo(){
        if(mPaths.size > 0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }
    fun onClickRedo(){
        if(mUndoPaths.size > 0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }

    /**
     * This method initializes the attributes of the
     * ViewForDrawing class.
     */
    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE // This is to draw a STROKE style
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND // This is for store join
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND // This is for stroke Cap
        mCanvasPaint = Paint(Paint.DITHER_FLAG) // Paint flag that enables dithering when blitting.
        //mBrushSize = 20.toFloat()
    }

    /** When the view is displayed */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }
    /**
     * This method is called when a stroke is drawn on the canvas
     * as a part of the painting.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        /**
         * Draw the specified bitmap, with its top/left corner at (x,y), using the specified paint,
         * transformed by the current matrix.
         *
         *If the bitmap and canvas have different densities, this function will take care of
         * automatically scaling the bitmap to draw at the same density as the canvas.
         *
         * @param bitmap The bitmap to be drawn
         * @param left The position of the left side of the bitmap being drawn
         * @param top The position of the top side of the bitmap being drawn
         * @param paint The paint used to draw the bitmap (may be null)
         */
        mCanvasBitmap?.let{
            canvas.drawBitmap(it,0f,0f, mCanvasPaint)
        }
        for(path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    /**
     * This method acts as an event listener when a touch
     * event is detected on the device.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset() // Clear any lines and curves from the path, making it empty.
                if(touchX !=null){
                    if(touchY != null){
                        mDrawPath!!.moveTo(touchX!!, touchY!!)// Set the beginning of the next contour to the point (x,y).
                    }
                }

            }

            MotionEvent.ACTION_MOVE -> {
                mDrawPath!!.lineTo(touchX!!, touchY!!)// Add a line from one point to the specific point(x,y)
            }

            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)//Add when to stroke is drawn to canvas and added in the path arraylist
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()

        return true
    }
    /**
     * This method is called when either the brush or the eraser
     * sizes are to be changed. This method sets the brush/eraser
     * sizes to the new values depending on user selection.
     */
    fun setSizeForBrush(newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String){
        color = Color.parseColor((newColor))
        mDrawPaint!!.color = color
    }

    /** It serves as a model that holds the color and the brush thickness of the drawing strokes */
    internal inner class CustomPath(var color: Int, var brushThickness: Float): Path() {




    }
}


