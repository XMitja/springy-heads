package com.flipkart.chatheads.container

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager.LayoutParams.*
import com.flipkart.chatheads.ChatHead
import com.flipkart.chatheads.ChatHeadManager
import com.flipkart.chatheads.arrangement.ChatHeadArrangement
import com.flipkart.chatheads.arrangement.MaximizedArrangement
import com.flipkart.chatheads.arrangement.MinimizedArrangement

/**
 * Created by kiran.kumar on 08/11/16.
 */

class WindowManagerContainer(context: Context) : FrameChatHeadContainer(context) {
    /**
     * A transparent view of the size of chat head which capture motion events and delegates them to the real view (frame layout)
     * This view is required since window managers will delegate the touch events to the window beneath it only if they are outside the bounds.
     * [android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL]
     */
    private var motionCaptureView: View? = null
    private val windowManager: WindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
    private var currentArrangement: ChatHeadArrangement? = null
    private var motionCaptureViewAdded: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onInitialized(manager: ChatHeadManager<*>) {
        super.onInitialized(manager)
        motionCaptureView = MotionCaptureView(context).apply{
            setOnTouchListener(MotionCapturingTouchListener())
        }
        registerReceiver(context)
    }

    private fun registerReceiver(context: Context) {
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                frameLayout?.minimize()
            }
        }, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    private fun setContainerHeight(container: View?, height: Int) {
        val layoutParams = getOrCreateLayoutParamsForContainer(container!!)
        layoutParams.height = height
        windowManager.updateViewLayout(container, layoutParams)
    }

    private fun setContainerWidth(container: View?, width: Int) {
        val layoutParams = getOrCreateLayoutParamsForContainer(container!!)
        layoutParams.width = width
        windowManager.updateViewLayout(container, layoutParams)
    }

    private fun getOrCreateLayoutParamsForContainer(container: View): WindowManager.LayoutParams {
        var layoutParams: WindowManager.LayoutParams? = container.layoutParams as WindowManager.LayoutParams
        if (layoutParams == null) {
            layoutParams = createContainerLayoutParams(false)
            container.layoutParams = layoutParams
        }
        return layoutParams
    }

    private fun setContainerX(container: View?, xPosition: Int) {
        val layoutParams = getOrCreateLayoutParamsForContainer(container!!)
        layoutParams.x = xPosition
        windowManager.updateViewLayout(container, layoutParams)
    }

    private fun getContainerX(container: View): Int {
        val layoutParams = getOrCreateLayoutParamsForContainer(container)
        return layoutParams.x
    }

    private fun setContainerY(container: View?, yPosition: Int) {
        val layoutParams = getOrCreateLayoutParamsForContainer(container!!)
        layoutParams.y = yPosition
        windowManager.updateViewLayout(container, layoutParams)
    }

    private fun getContainerY(container: View): Int {
        val layoutParams = getOrCreateLayoutParamsForContainer(container)
        return layoutParams.y
    }

    @SuppressLint("RtlHardcoded")
    private fun createContainerLayoutParams(focusable: Boolean): WindowManager.LayoutParams {
        val focusableFlag: Int = if (focusable) {
            FLAG_NOT_TOUCH_MODAL
        } else {
            FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE
        }
        @Suppress("DEPRECATION")
        val overlayFlag = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_PHONE
        else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val layoutParams = WindowManager.LayoutParams(MATCH_PARENT, MATCH_PARENT,
                overlayFlag,
                focusableFlag,
                PixelFormat.TRANSLUCENT)
        layoutParams.x = 0
        layoutParams.y = 0
        // BEGIN is different from BEGIN in addChatHead
        layoutParams.gravity = Gravity.TOP or Gravity.LEFT
        return layoutParams
    }

    override fun addContainer(container: View?, focusable: Boolean) {
        val containerLayoutParams = createContainerLayoutParams(focusable)
        addContainer(container!!, containerLayoutParams)
    }

    private fun addContainer(container: View, containerLayoutParams: WindowManager.LayoutParams) {
        container.layoutParams = containerLayoutParams
        windowManager.addView(container, containerLayoutParams)
    }

    override fun setViewX(view: View, xPosition: Int) {
        super.setViewX(view, xPosition)
        if (view is ChatHead<*>) {
            if (view.isHero && currentArrangement is MinimizedArrangement<*>) {
                setContainerX(motionCaptureView, xPosition)
                setContainerWidth(motionCaptureView, view.getMeasuredWidth())
            }
        }
    }

    override fun setViewY(view: View, yPosition: Int) {
        super.setViewY(view, yPosition)
        if (view is ChatHead<*> && currentArrangement is MinimizedArrangement<*>) {
            if (view.isHero) {
                setContainerY(motionCaptureView, yPosition)
                setContainerHeight(motionCaptureView, view.getMeasuredHeight())
            }
        }
    }

    override fun onArrangementChanged(oldArrangement: ChatHeadArrangement?, newArrangement: ChatHeadArrangement) {
        currentArrangement = newArrangement
        if (oldArrangement is MinimizedArrangement<*> && newArrangement is MaximizedArrangement<*>) {
            // about to be maximized
            var layoutParams = getOrCreateLayoutParamsForContainer(motionCaptureView!!)
            layoutParams.flags = layoutParams.flags or (FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE)
            windowManager.updateViewLayout(motionCaptureView, layoutParams)

            layoutParams = getOrCreateLayoutParamsForContainer(frameLayout)
            layoutParams.flags = layoutParams.flags and FLAG_NOT_FOCUSABLE.inv() //add focusability
            layoutParams.flags = layoutParams.flags and FLAG_NOT_TOUCHABLE.inv() //add focusability
            layoutParams.flags = layoutParams.flags or FLAG_NOT_TOUCH_MODAL

            windowManager.updateViewLayout(frameLayout, layoutParams)

            setContainerX(motionCaptureView, 0)
            setContainerY(motionCaptureView, 0)
            setContainerWidth(motionCaptureView, frameLayout.measuredWidth)
            setContainerHeight(motionCaptureView, frameLayout.measuredHeight)

        } else {
            // about to be minimized
            var layoutParams = getOrCreateLayoutParamsForContainer(motionCaptureView!!)
            layoutParams.flags = layoutParams.flags or FLAG_NOT_FOCUSABLE //remove focusability
            layoutParams.flags = layoutParams.flags and FLAG_NOT_TOUCHABLE.inv() //add touch
            layoutParams.flags = layoutParams.flags or FLAG_NOT_TOUCH_MODAL //add touch
            windowManager.updateViewLayout(motionCaptureView, layoutParams)

            layoutParams = getOrCreateLayoutParamsForContainer(frameLayout)
            layoutParams.flags = layoutParams.flags or (FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE)
            windowManager.updateViewLayout(frameLayout, layoutParams)
        }
    }

    override fun addView(view: View, layoutParams: ViewGroup.LayoutParams) {
        super.addView(view, layoutParams)
        if (!motionCaptureViewAdded && manager.chatHeads.size > 0) {
            addContainer(motionCaptureView, true)
            val motionCaptureParams = getOrCreateLayoutParamsForContainer(motionCaptureView!!)
            motionCaptureParams.width = 0
            motionCaptureParams.height = 0
            windowManager.updateViewLayout(motionCaptureView, motionCaptureParams)
            motionCaptureViewAdded = true
        }
    }

    override fun removeView(view: View) {
        super.removeView(view)
        if (manager.chatHeads.size == 0) {
            windowManager.removeViewImmediate(motionCaptureView)
            motionCaptureViewAdded = false
        }
    }

    /*
    private void removeContainer(View motionCaptureView) {
        windowManager.removeView(motionCaptureView);
    }
    */

    fun destroy() {
        windowManager.removeViewImmediate(motionCaptureView)
        windowManager.removeViewImmediate(frameLayout)
    }

    private inner class MotionCapturingTouchListener : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val offsetX = getContainerX(v).toFloat()
            val offsetY = getContainerY(v).toFloat()
            event.offsetLocation(offsetX, offsetY)
            return frameLayout?.dispatchTouchEvent(event) ?: false
        }

    }


    private inner class MotionCaptureView(context: Context) : View(context) {
        /*
        override fun onAttachedToWindow() {
            setBackgroundColor(Color.argb(100, 0, 255, 0))
            super.onAttachedToWindow()
        }
        */
    }
}
