package com.flipkart.chatheads

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringListener
import com.facebook.rebound.SpringSystem
import com.flipkart.chatheads.utils.ChatHeadUtils
import com.flipkart.chatheads.utils.SpringConfigsHolder
import java.io.Serializable

/**
 * Created by kirankumar on 10/02/15.
 */
class ChatHead<T : Serializable> : androidx.appcompat.widget.AppCompatImageView, SpringListener {

    @Suppress("PropertyName")
    val CLOSE_ATTRACTION_THRESHOLD = ChatHeadUtils.dpToPx(context, 110)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    //private final float DELTA = ChatHeadUtils.dpToPx(getContext(), 10);
    private lateinit var manager: ChatHeadManager<*>
    private lateinit var springSystem: SpringSystem
    var isSticky: Boolean = false
    /*
    public void setUnreadCount(int unreadCount) {
        if (unreadCount != this.unreadCount) {
            manager.reloadDrawable(key);
        }
        this.unreadCount = unreadCount;
    }
    */

    var state: State? = null
    var key: T? = null
    private var downX = -1f
    private var downY = -1f
    private var velocityTracker: VelocityTracker? = null
    private var isDragging: Boolean = false
    private var downTranslationX: Float = 0.toFloat()
    private var downTranslationY: Float = 0.toFloat()
    private var scaleSpring: Spring? = null
    /*
    public Bundle getExtras() {
        return extras;
    }

    public void setExtras(Bundle extras) {
        this.extras = extras;
    }
    */

    var horizontalSpring: Spring? = null
        private set
    var verticalSpring: Spring? = null
        private set
    var isHero: Boolean = false
    val unreadCount: Int = 0

    constructor(context: Context) : super(context) {
        throw IllegalArgumentException("This constructor cannot be used")
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        throw IllegalArgumentException("This constructor cannot be used")
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        throw IllegalArgumentException("This constructor cannot be used")
    }

    constructor(manager: ChatHeadManager<*>, springsHolder: SpringSystem, context: Context, isSticky: Boolean) : super(context) {
        this.manager = manager
        this.springSystem = springsHolder
        this.isSticky = isSticky
        init()
    }

    private fun init() {
        val xPositionListener = object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring?) {
                super.onSpringUpdate(spring)
                manager.chatHeadContainer.setViewX(this@ChatHead, spring!!.currentValue.toInt())
            }
        }
        horizontalSpring = springSystem.createSpring()
        horizontalSpring!!.addListener(xPositionListener)
        horizontalSpring!!.addListener(this)

        val yPositionListener = object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring?) {
                super.onSpringUpdate(spring)
                manager.chatHeadContainer.setViewY(this@ChatHead, spring!!.currentValue.toInt())
            }
        }
        verticalSpring = springSystem.createSpring()
        verticalSpring!!.addListener(yPositionListener)
        verticalSpring!!.addListener(this)

        scaleSpring = springSystem.createSpring()
        scaleSpring!!.addListener(object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring?) {
                super.onSpringUpdate(spring)
                val currentValue = spring!!.currentValue
                scaleX = currentValue.toFloat()
                scaleY = currentValue.toFloat()
            }
        })
        scaleSpring!!.setCurrentValue(1.0).setAtRest()
    }

    override fun onSpringUpdate(spring: Spring) {
        if (horizontalSpring != null && verticalSpring != null) {
            val activeHorizontalSpring = horizontalSpring
            val activeVerticalSpring = verticalSpring
            if (spring !== activeHorizontalSpring && spring !== activeVerticalSpring)
                return
            val totalVelocity = Math.hypot(activeHorizontalSpring!!.velocity, activeVerticalSpring!!.velocity).toInt()
            if (manager.activeArrangement != null)
                manager.activeArrangement.onSpringUpdate(this, isDragging, manager.maxWidth, manager.maxHeight, spring, activeHorizontalSpring, activeVerticalSpring, totalVelocity)
        }
    }

    override fun onSpringAtRest(spring: Spring) {
        manager.listener?.onChatHeadAnimateEnd(this)
    }

    override fun onSpringActivate(spring: Spring) {
        manager.listener?.onChatHeadAnimateStart(this)
    }

    override fun onSpringEndStateChange(spring: Spring) {

    }

    /*
    public SpringListener getHorizontalPositionListener() {
        return xPositionListener;
    }

    public SpringListener getVerticalPositionListener() {
        return yPositionListener;
    }
    */

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        if (horizontalSpring == null || verticalSpring == null) return false
        //Chathead view will set the correct active springs on touch
        val activeHorizontalSpring = horizontalSpring
        val activeVerticalSpring = verticalSpring

        val action = event.action
        val rawX = event.rawX
        val rawY = event.rawY
        val offsetX = rawX - downX
        val offsetY = rawY - downY
        val showCloseButton = manager.activeArrangement.shouldShowCloseButton(this)
        event.offsetLocation(manager.chatHeadContainer.getViewX(this).toFloat(), manager.chatHeadContainer.getViewY(this).toFloat())
        if (action == MotionEvent.ACTION_DOWN) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            } else {
                velocityTracker!!.clear()

            }
            activeHorizontalSpring!!.springConfig = SpringConfigsHolder.NOT_DRAGGING
            activeVerticalSpring!!.springConfig = SpringConfigsHolder.NOT_DRAGGING
            state = ChatHead.State.FREE
            downX = rawX
            downY = rawY
            downTranslationX = activeHorizontalSpring.currentValue.toFloat()
            downTranslationY = activeVerticalSpring.currentValue.toFloat()
            scaleSpring!!.endValue = .9
            activeHorizontalSpring.setAtRest()
            activeVerticalSpring.setAtRest()
            velocityTracker!!.addMovement(event)


        } else if (action == MotionEvent.ACTION_MOVE) {
            if (Math.hypot(offsetX.toDouble(), offsetY.toDouble()) > touchSlop) {
                isDragging = true
                if (showCloseButton) {
                    manager.closeButton.appear()
                }
            }
            velocityTracker!!.addMovement(event)

            if (isDragging) {
                manager.closeButton.pointTo(rawX, rawY)
                if (manager.activeArrangement.canDrag(this)) {
                    val distanceCloseButtonFromHead = manager.getDistanceCloseButtonFromHead(rawX, rawY)
                    if (distanceCloseButtonFromHead < CLOSE_ATTRACTION_THRESHOLD && showCloseButton) {
                        state = ChatHead.State.CAPTURED
                        activeHorizontalSpring!!.springConfig = SpringConfigsHolder.NOT_DRAGGING
                        activeVerticalSpring!!.springConfig = SpringConfigsHolder.NOT_DRAGGING
                        val coords = manager.getChatHeadCoordsForCloseButton(this)
                        activeHorizontalSpring.endValue = coords[0].toDouble()
                        activeVerticalSpring.endValue = coords[1].toDouble()
                        manager.closeButton.onCapture()

                    } else {
                        state = ChatHead.State.FREE
                        activeHorizontalSpring!!.springConfig = SpringConfigsHolder.DRAGGING
                        activeVerticalSpring!!.springConfig = SpringConfigsHolder.DRAGGING
                        activeHorizontalSpring.currentValue = (downTranslationX + offsetX).toDouble()
                        activeVerticalSpring.currentValue = (downTranslationY + offsetY).toDouble()
                        manager.closeButton.onRelease()
                    }

                    velocityTracker!!.computeCurrentVelocity(1000)
                }

            }

        } else {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                val wasDragging = isDragging
                activeHorizontalSpring!!.springConfig = SpringConfigsHolder.DRAGGING
                activeHorizontalSpring.springConfig = SpringConfigsHolder.DRAGGING
                isDragging = false
                scaleSpring!!.endValue = 1.0
                val xVelocity = velocityTracker!!.xVelocity.toInt()
                val yVelocity = velocityTracker!!.yVelocity.toInt()
                velocityTracker!!.recycle()
                velocityTracker = null
                if (horizontalSpring != null && verticalSpring != null) {
                    manager.activeArrangement.handleTouchUp(this, xVelocity, yVelocity, activeHorizontalSpring, activeVerticalSpring, wasDragging)
                }
            }
        }

        return true
    }

    fun onRemove() {
        horizontalSpring?.setAtRest()
        horizontalSpring?.removeAllListeners()
        horizontalSpring?.destroy()
        horizontalSpring = null
        verticalSpring?.setAtRest()
        verticalSpring?.removeAllListeners()
        verticalSpring?.destroy()
        verticalSpring = null
        scaleSpring?.setAtRest()
        scaleSpring?.removeAllListeners()
        scaleSpring?.destroy()
        scaleSpring = null
    }


    enum class State {
        FREE, CAPTURED
    }
}

