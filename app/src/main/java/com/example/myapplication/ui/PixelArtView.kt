package com.example.myapplication.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.use
import androidx.core.content.res.getColorOrThrow
import com.example.myapplication.R

class PixelArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint()
    private val cellPaint = Paint()
    private val hoverPaint = Paint()

    var cellCount: Int = 10
        set(value) { field = value; invalidate() }
    var showGrid: Boolean = true
        set(value) { field = value; invalidate() }

    var gridColor: Int = 0x000000
        set(value) { field = value; invalidate() }
    var cellColor: Int = 0xFFFFFF
        set(value) { field = value; invalidate() }

    private var pixmap = mutableListOf<Point>()
    private var hover: Point? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PixelArtView, 0, 0).use { array ->
            gridColor = array.getColorOrThrow(R.styleable.PixelArtView_gridColor)
            cellColor = array.getColorOrThrow(R.styleable.PixelArtView_cellColor)

            gridPaint.color = gridColor
            gridPaint.style = Paint.Style.STROKE
            gridPaint.strokeWidth = 1.dp()

            cellPaint.color = cellColor
            cellPaint.style = Paint.Style.FILL_AND_STROKE
            cellPaint.strokeWidth = 1f

            hoverPaint.color = cellColor
            hoverPaint.style = Paint.Style.FILL_AND_STROKE
            hoverPaint.alpha = 100

            cellCount = array.getInteger(R.styleable.PixelArtView_cellCount, 10)
            showGrid = array.getBoolean(R.styleable.PixelArtView_showGrid, true)
        }

        val padding = 16.dp().toInt()
        setPadding(padding, padding, padding, padding)
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthSize < heightSize) {
            setMeasuredDimension(widthSize, widthSize)
        } else {
            setMeasuredDimension(heightSize, heightSize)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gridSize = (width - paddingLeft - paddingRight).toFloat()
        val cellSize = gridSize / cellCount
        var horizontalX = paddingLeft + cellSize
        var verticalY = paddingTop + cellSize

        canvas.drawRect(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            gridSize + paddingRight.toFloat(),
            gridSize + paddingBottom.toFloat(),
            gridPaint,
        )

        if (showGrid) {
            gridPaint.color = gridColor
            for (i in 1 until cellCount) {
                canvas.drawLine(
                    horizontalX,
                    paddingTop.toFloat(),
                    horizontalX,
                    gridSize + paddingTop,
                    gridPaint,
                )
                canvas.drawLine(
                    paddingLeft.toFloat(),
                    verticalY,
                    gridSize + paddingLeft,
                    verticalY,
                    gridPaint,
                )
                horizontalX += cellSize
                verticalY += cellSize
            }
        }

        if (hover != null) {
            hoverPaint.color = cellColor
            hoverPaint.alpha = 100
            val (hoverX, hoverY) = hover!!
            if (hoverX >= 0 && hoverY >= 0) {
                val pixelX = hoverX * cellSize
                val pixelY = hoverY * cellSize
                canvas.drawRect(
                    paddingLeft + pixelX,
                    paddingTop + pixelY,
                    paddingRight + pixelX + cellSize,
                    paddingBottom + pixelY + cellSize,
                    hoverPaint,
                )
            }
        }

        for ((x, y, color) in pixmap) {
            cellPaint.color = color
            val pixelX = x * cellSize
            val pixelY = y * cellSize
            canvas.drawRect(
                paddingLeft + pixelX,
                paddingTop + pixelY,
                paddingRight + pixelX + cellSize,
                paddingBottom + pixelY + cellSize,
                cellPaint,
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> hover(event.x, event.y)
            MotionEvent.ACTION_MOVE -> hover(event.x, event.y)
            MotionEvent.ACTION_UP -> place(event.x, event.y)
            MotionEvent.ACTION_CANCEL -> {
                hover = null
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).apply {
            this.pixmap = this@PixelArtView.pixmap
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            this.pixmap = state.pixmap
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun place(clickX: Float, clickY: Float) {
        checkBounds(
            x = clickX,
            y = clickY,
            isInBounds = { x, y ->
                val point = Point(x, y, cellColor)
                val existing = pixmap.find { it.x == point.x && it.y == point.y }
                if (existing == null) {
                    pixmap.add(point)
                } else {
                    pixmap.remove(existing)
                }
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                hover = null
                invalidate()
            },
            isOutBounds = {
                hover = null
                invalidate()
            }
        )
    }

    private fun hover(hoverX: Float, hoverY: Float) {
        checkBounds(
            x = hoverX,
            y = hoverY,
            isInBounds = { x, y ->
                hover = Point(x, y, cellColor)
                invalidate()
            },
            isOutBounds = {
                hover = null
                invalidate()
            }
        )
    }

    private fun checkBounds(
        x: Float,
        y: Float,
        isInBounds: (x: Int, y: Int) -> Unit,
        isOutBounds: () -> Unit,
    ) {
        val gridSize = (width - paddingLeft - paddingRight).toFloat()
        val cellSize = gridSize / cellCount

        var horizontalPoint = paddingLeft.toFloat()
        var verticalPoint = paddingTop.toFloat()
        var placeX = -1
        var placeY = -1

        for (i in 0..cellCount) {
            if (x <= horizontalPoint && x >= horizontalPoint - cellSize) {
                placeX = i - 1
            }
            if (y <= verticalPoint  && y >= verticalPoint - cellSize) {
                placeY = i - 1
            }
            horizontalPoint += cellSize
            verticalPoint += cellSize
        }
        if (placeX >= 0 &&
            placeY >= 0 &&
            placeX < cellCount &&
            placeY < cellCount
        ) {
            isInBounds(placeX, placeY)
        } else {
            isOutBounds()
        }
    }

    private fun Int.dp(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        )
    }

    private class SavedState : BaseSavedState {

        var pixmap = mutableListOf<Point>()

        constructor(superState: Parcelable?) : super(superState)
        constructor(source: Parcel?) : super(source) {
            source?.run {
                val size = source.readInt()
                for (i in 0..size) {
                    val point = Point(
                        x = source.readInt(),
                        y = source.readInt(),
                        color = source.readInt(),
                    )
                    pixmap.add(point)
                }
            }
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(pixmap.size)
            for ((x, y, color) in pixmap) {
                out.writeInt(x)
                out.writeInt(y)
                out.writeInt(color)
            }
        }

        companion object {

            @JvmField
            @Suppress("unused")
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel?) = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}