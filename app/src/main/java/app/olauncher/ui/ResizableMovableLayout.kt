package app.olauncher.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.view.ViewGroup
import app.olauncher.data.Prefs
import app.olauncher.helper.WidgetSizeHelper
import app.olauncher.helper.WidgetSize
import app.olauncher.helper.WidgetPosition
import kotlin.math.max
import kotlin.math.min

class ResizableMovableLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var lastX = 0f
    private var lastY = 0f
    private var resizing = false
    private var moving = false
    private val resizeHandleSize = 60 // px - made larger for easier touch
    private var widgetId: Int = -1
    private lateinit var prefs: Prefs

    fun setWidgetId(id: Int) {
        widgetId = id
        prefs = Prefs(context)
        loadSavedSizeAndPosition()
    }

    private fun loadSavedSizeAndPosition() {
        if (widgetId == -1) return
        
        try {
            // Load saved size
            val savedSizes = WidgetSizeHelper.loadWidgetSizes(prefs.widgetSizes)
            val savedSize = savedSizes[widgetId]
            if (savedSize != null) {
                val params = layoutParams
                if (params is ViewGroup.MarginLayoutParams) {
                    params.width = savedSize.width
                    params.height = savedSize.height
                    layoutParams = params
                }
            }
            
            // Load saved position
            val savedPositions = WidgetSizeHelper.loadWidgetPositions(prefs.widgetPositions)
            val savedPosition = savedPositions[widgetId]
            if (savedPosition != null) {
                val params = layoutParams
                if (params is ViewGroup.MarginLayoutParams) {
                    params.leftMargin = savedPosition.leftMargin
                    params.topMargin = savedPosition.topMargin
                    layoutParams = params
                }
            }
        } catch (e: Exception) {
            Log.e("ResizableMovableLayout", "Error loading saved size/position: ${e.message}")
        }
    }

    private fun saveSizeAndPosition() {
        if (widgetId == -1) return
        
        try {
            val params = layoutParams
            if (params is ViewGroup.MarginLayoutParams) {
                // Save size
                val currentSizes = WidgetSizeHelper.loadWidgetSizes(prefs.widgetSizes).toMutableMap()
                currentSizes[widgetId] = WidgetSize(params.width, params.height)
                prefs.widgetSizes = WidgetSizeHelper.saveWidgetSizes(currentSizes)
                
                // Save position
                val currentPositions = WidgetSizeHelper.loadWidgetPositions(prefs.widgetPositions).toMutableMap()
                currentPositions[widgetId] = WidgetPosition(params.leftMargin, params.topMargin)
                prefs.widgetPositions = WidgetSizeHelper.saveWidgetPositions(currentPositions)
            }
        } catch (e: Exception) {
            Log.e("ResizableMovableLayout", "Error saving size/position: ${e.message}")
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Intercept touch events in the resize handle area or when moving
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = ev.rawX
                lastY = ev.rawY
                resizing = isInResizeHandle(ev.x, ev.y)
                moving = !resizing && ev.x < width - resizeHandleSize && ev.y < height - resizeHandleSize
                
                // Always intercept if we're in resize handle or moving area
                if (resizing || moving) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            val parentRect = Rect()
            (parent as? View)?.getHitRect(parentRect)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    resizing = isInResizeHandle(event.x, event.y)
                    moving = !resizing
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    
                    val currentParams = layoutParams
                    if (currentParams is ViewGroup.MarginLayoutParams) {
                        if (resizing) {
                            // Resize the widget
                            val newWidth = max(150, min(width + dx.toInt(), parentRect.width() - currentParams.leftMargin))
                            val newHeight = max(100, min(height + dy.toInt(), parentRect.height() - currentParams.topMargin))
                            currentParams.width = newWidth
                            currentParams.height = newHeight
                        } else if (moving) {
                            // Move the widget
                            val maxLeftMargin = parentRect.width() - width
                            val maxTopMargin = parentRect.height() - height
                            val newLeftMargin = min(max(0, currentParams.leftMargin + dx.toInt()), maxLeftMargin)
                            val newTopMargin = min(max(0, currentParams.topMargin + dy.toInt()), maxTopMargin)
                            currentParams.leftMargin = newLeftMargin
                            currentParams.topMargin = newTopMargin
                        }
                        layoutParams = currentParams
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resizing = false
                    moving = false
                    // Save the final size and position
                    saveSizeAndPosition()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("ResizableMovableLayout", "Error handling touch event: ${e.message}")
        }
        return super.onTouchEvent(event)
    }

    private fun isInResizeHandle(x: Float, y: Float): Boolean {
        return x > width - resizeHandleSize && y > height - resizeHandleSize
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        // Draw a visual indicator for the resize handle
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        // Draw resize handle indicator in bottom-right corner
        val handleRect = android.graphics.RectF(
            width - resizeHandleSize.toFloat(),
            height - resizeHandleSize.toFloat(),
            width.toFloat(),
            height.toFloat()
        )
        canvas.drawRect(handleRect, paint)
        
        // Draw diagonal lines to indicate resize handle
        canvas.drawLine(
            width - resizeHandleSize.toFloat(),
            height.toFloat(),
            width.toFloat(),
            height - resizeHandleSize.toFloat(),
            paint
        )
        
        // Draw delete indicator in top-right corner
        val deletePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        // Draw a small X in the top-right corner
        val deleteSize = 30f
        val deleteX = width - deleteSize - 10f
        val deleteY = 10f
        
        canvas.drawLine(deleteX, deleteY, deleteX + deleteSize, deleteY + deleteSize, deletePaint)
        canvas.drawLine(deleteX + deleteSize, deleteY, deleteX, deleteY + deleteSize, deletePaint)
        
        // Draw additional visual cues for moving
        if (moving) {
            val movePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLUE
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), movePaint)
        }
    }
} 