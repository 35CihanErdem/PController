package com.cihan.pccontroller.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Trackpad yüzeyi.
 * - 1 parmak sürükle → mouse move
 * - 1 parmak kısa dokunuş → sol tık
 * - 2 parmak kısa dokunuş → sağ tık
 * - 2 parmak dikey sürükle → scroll
 */
class TrackpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onMove(dx: Int, dy: Int)
        fun onScroll(wheel: Int)
        fun onLeftClick()
        fun onRightClick()
    }

    var listener: Listener? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val tapTimeout = ViewConfiguration.getTapTimeout().toLong()
    private val density = resources.displayMetrics.density

    /** Piksel → HID relative birim */
    private val moveSensitivity = 1.8f
    private val scrollSensitivity = 0.18f

    private var accumX = 0f
    private var accumY = 0f
    private var scrollAccum = 0f

    private var primaryId = MotionEvent.INVALID_POINTER_ID
    private var secondaryId = MotionEvent.INVALID_POINTER_ID
    private var lastX = 0f
    private var lastY = 0f
    private var lastScrollY = 0f
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var movedBeyondSlop = false
    private var multiTouch = false
    private var scrolled = false

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x221E4FD6
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = 0x662F6BFF
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x998BA3C0.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 14f * density
    }

    override fun onDraw(canvas: Canvas) {
        val r = 18f * density
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, fillPaint)
        canvas.drawRoundRect(
            strokePaint.strokeWidth / 2,
            strokePaint.strokeWidth / 2,
            width - strokePaint.strokeWidth / 2,
            height - strokePaint.strokeWidth / 2,
            r,
            r,
            strokePaint
        )
        canvas.drawText("TRACKPAD", width / 2f, height / 2f, hintPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                primaryId = event.getPointerId(0)
                secondaryId = MotionEvent.INVALID_POINTER_ID
                lastX = event.x
                lastY = event.y
                downX = event.x
                downY = event.y
                downTime = event.eventTime
                movedBeyondSlop = false
                multiTouch = false
                scrolled = false
                accumX = 0f
                accumY = 0f
                scrollAccum = 0f
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    multiTouch = true
                    secondaryId = event.getPointerId(event.actionIndex)
                    val i0 = event.findPointerIndex(primaryId).takeIf { it >= 0 } ?: 0
                    val i1 = event.findPointerIndex(secondaryId).takeIf { it >= 0 }
                        ?: event.actionIndex
                    lastScrollY = (event.getY(i0) + event.getY(i1)) / 2f
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    multiTouch = true
                    val i0 = event.findPointerIndex(primaryId).takeIf { it >= 0 } ?: 0
                    val i1 = (0 until event.pointerCount).firstOrNull { it != i0 } ?: return true
                    val midY = (event.getY(i0) + event.getY(i1)) / 2f
                    val dy = midY - lastScrollY
                    lastScrollY = midY
                    if (abs(dy) > 1f) {
                        scrolled = true
                        movedBeyondSlop = true
                        scrollAccum += -dy * scrollSensitivity
                        val wheel = scrollAccum.toInt()
                        if (wheel != 0) {
                            scrollAccum -= wheel
                            listener?.onScroll(wheel)
                        }
                    }
                } else if (primaryId != MotionEvent.INVALID_POINTER_ID) {
                    val idx = event.findPointerIndex(primaryId)
                    if (idx < 0) return true
                    val x = event.getX(idx)
                    val y = event.getY(idx)
                    val dx = x - lastX
                    val dy = y - lastY
                    lastX = x
                    lastY = y
                    if (!movedBeyondSlop) {
                        val total = sqrt(
                            (x - downX) * (x - downX) + (y - downY) * (y - downY)
                        )
                        if (total > touchSlop) movedBeyondSlop = true
                    }
                    if (movedBeyondSlop && !multiTouch) {
                        emitMove(dx, dy)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == secondaryId) {
                    secondaryId = MotionEvent.INVALID_POINTER_ID
                }
                if (id == primaryId && event.pointerCount > 1) {
                    // Primary kalktı — kalanı primary yap
                    val other = (0 until event.pointerCount)
                        .map { event.getPointerId(it) }
                        .firstOrNull { it != id }
                    if (other != null) {
                        primaryId = other
                        val idx = event.findPointerIndex(other)
                        if (idx >= 0) {
                            lastX = event.getX(idx)
                            lastY = event.getY(idx)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val duration = event.eventTime - downTime
                val isTap = !movedBeyondSlop && duration <= tapTimeout + 80
                if (event.actionMasked == MotionEvent.ACTION_UP && isTap) {
                    if (multiTouch && !scrolled) {
                        listener?.onRightClick()
                    } else if (!multiTouch) {
                        listener?.onLeftClick()
                    }
                }
                primaryId = MotionEvent.INVALID_POINTER_ID
                secondaryId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun emitMove(dxPx: Float, dyPx: Float) {
        accumX += dxPx * moveSensitivity
        accumY += dyPx * moveSensitivity
        val ix = accumX.toInt()
        val iy = accumY.toInt()
        if (ix != 0 || iy != 0) {
            accumX -= ix
            accumY -= iy
            listener?.onMove(ix, iy)
        }
    }
}
