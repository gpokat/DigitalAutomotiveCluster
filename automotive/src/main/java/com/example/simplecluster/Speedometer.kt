package com.example.simplecluster

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin


class Speedometer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    //TODO need to add a normalization for temp/rpm ranges based on MAX/MIN vehicle values
    private val batteryGaugeWidth = 20F
    private val batteryGaugeSizeXY = 200F
    private val batteryGaugeNTotalCells = 9 //[0-9] cells. Total is 10 i.e. 10%
    private val sweepCellAngle = 25F //[0-77] degrees
    private var batteryGaugeNInactiveCells = 3
    private var mCurrentSpeed = 25
    private var mCurrentRpm = 35F
    private var mCurrentEngineOilTemp = 15F
    private var mBatteryCapacity = 100F;
    private var speedTextBounds = Rect()
    private val metricLabel = "MPH"
    private val metricLabelGap = 10F
    private val centerX get() = width / 2F
    private val centerY get() = height / 2F
    private val innerBoxShift = 25F
    private val innerBoxFlaskShift = 19F
    private val innerBoxFlask2Shift = 31F
    private val innerBoxFlaskLabelsTShift = 37F
    private val innerBoxFlaskLabelsRShift = 44F
    private val startTickGap = 8F
    private val endTickGap = 39F
    private val degToRad = PI.toFloat()/180F
    private val startDegAngle = 140F
    private val endDegAngle = 399F
    private val flaskDegSwipeAngle = 77F
    private val durationOne = 1000L //msec
    private val durationOneAndHalf = 1500L
    private val durationTwo = 2000L
    private val leftBox get() = centerX-batteryGaugeSizeXY-batteryGaugeWidth/2
    private val topBox get() = centerY-batteryGaugeSizeXY-batteryGaugeWidth/2
    private val rightBox get() = centerX+batteryGaugeSizeXY+batteryGaugeWidth/2
    private val bottomBox get() = centerY+batteryGaugeSizeXY+batteryGaugeWidth/2
    private val speedometerBox get() = RectF(leftBox,topBox,rightBox,bottomBox)
    private val innerBox get() = RectF(leftBox+innerBoxShift,topBox+innerBoxShift,rightBox-innerBoxShift,bottomBox-innerBoxShift)
    private val innerBoxFlask get() = RectF(leftBox+innerBoxFlaskShift,topBox+innerBoxFlaskShift,rightBox-innerBoxFlaskShift,bottomBox-innerBoxFlaskShift)
    private val innerBoxFlask2 get() = RectF(leftBox+innerBoxFlask2Shift,topBox+innerBoxFlask2Shift,rightBox-innerBoxFlask2Shift,bottomBox-innerBoxFlask2Shift)
    private val innerBoxFlaskLabelsTemp get() = RectF(leftBox+innerBoxFlaskLabelsTShift,topBox+innerBoxFlaskLabelsTShift,rightBox-innerBoxFlaskLabelsTShift,bottomBox-innerBoxFlaskLabelsTShift)
    private val innerBoxFlaskLabelsRpm get() = RectF(leftBox+innerBoxFlaskLabelsRShift,topBox+innerBoxFlaskLabelsRShift,rightBox-innerBoxFlaskLabelsRShift,bottomBox-innerBoxFlaskLabelsRShift)

    private var metricLabelBounds = Rect()

    // Animators
    private val animatorRpm = ValueAnimator.ofFloat().apply {
        interpolator = LinearOutSlowInInterpolator()
        duration = durationOneAndHalf
    }

    private val animatorOilTemp = ValueAnimator.ofFloat().apply {
        interpolator = LinearOutSlowInInterpolator()
        duration = durationOneAndHalf
    }

    private val animatorSpeed = ValueAnimator.ofInt().apply {
        interpolator = AccelerateDecelerateInterpolator()
        duration = durationTwo
    }

    private val animatorBattery = ValueAnimator.ofInt().apply {
        interpolator = LinearInterpolator()
        duration = durationOne
    }

    private val mArcTextRpm get() = Path().apply {
        addArc(innerBoxFlaskLabelsRpm, startDegAngle+1, flaskDegSwipeAngle)
    }
    private val mArcTextTemp get() = Path().apply {
        addArc(innerBoxFlaskLabelsTemp, endDegAngle-1, -flaskDegSwipeAngle)
    }

    private fun getStartTicksXY(deg: Float, isStart: Boolean) : PointF =
        if(isStart) {
            PointF(innerBoxFlask.centerX()-cos(deg*degToRad)*(batteryGaugeSizeXY-startTickGap),
            innerBoxFlask.centerY()+sin(deg*degToRad)*(batteryGaugeSizeXY-startTickGap))
        } else {
            PointF(innerBoxFlask.centerX()-cos(deg*degToRad)*(batteryGaugeSizeXY-endTickGap),
                innerBoxFlask.centerY()+sin(deg*degToRad)*(batteryGaugeSizeXY-endTickGap))
        }

    private val paintBatteryGaugeActive = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#62c1e0")
        strokeWidth = batteryGaugeWidth
        strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        setShadowLayer(10F,0F,0F, Color.parseColor("#62c1e0"))

    }

    private val paintBatteryGaugeInactive = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#aaafb4")
        strokeWidth = batteryGaugeWidth
        strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        setShadowLayer(10F,0F,0F, Color.parseColor("#aaafb4"))
    }

    private fun drawBatteryGauge(canvas: Canvas) {
        for (i in 0..batteryGaugeNTotalCells) {
            var painter = if (i - 1 < batteryGaugeNTotalCells - batteryGaugeNInactiveCells) {
                paintBatteryGaugeActive
            } else {
                paintBatteryGaugeInactive
            }
            canvas.drawArc(
                speedometerBox,
                startDegAngle + (sweepCellAngle + 1) * i,
                sweepCellAngle,
                false,
                painter
            ) //start angle 140F+i*25F+(i*1.0F)
        }
    }

    private val paintCurrentSpeed = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
        //strokeWidth = 5F
        //strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        textSize = 100F
        //typeface =  //TODO
        setShadowLayer(5F,0F,0F, Color.WHITE)
    }

    private val paintMetricLabel = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#62c1e0")
        strokeWidth = 5F
        strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        textSize = 20F
        //typeface =  //TODO
        setShadowLayer(2F,0F,0F, Color.parseColor("#62c1e0"))
    }

    private val paintRpmGaugeText = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#8e7cc3")
        strokeWidth = 1F//batteryGaugeWidth
        alpha = 255 //[0-255]
        //setShadowLayer(10F,0F,0F, Color.parseColor("#8e7cc3"))
    }
    private val paintTempGaugeText = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#ff7b7b")
        strokeWidth = 1F//batteryGaugeWidth
        alpha = 255 //[0-255]
        //setShadowLayer(10F,0F,0F, Color.parseColor("#8e7cc3"))
    }
    private fun drawSpeedText(canvas: Canvas) {
        var speed = mCurrentSpeed.toString()
        paintCurrentSpeed.getTextBounds(speed, 0, speed.length, speedTextBounds)
        canvas.drawText(speed, speedometerBox.centerX() - speedTextBounds.exactCenterX(), speedometerBox.centerY() - speedTextBounds.exactCenterY() - 25F, paintCurrentSpeed)

        paintMetricLabel.getTextBounds(metricLabel, 0, metricLabel.length, metricLabelBounds)
        canvas.drawText(metricLabel, speedometerBox.centerX() - metricLabelBounds.exactCenterX(), speedometerBox.centerY() - metricLabelBounds.exactCenterY() + paintCurrentSpeed.textSize/2 + metricLabelGap -25F, paintMetricLabel)

        canvas.drawTextOnPath("Rpm  x1000", mArcTextRpm,0F,0F,paintRpmGaugeText)
        canvas.drawTextOnPath("Temp  F", mArcTextTemp,0F,0F,paintTempGaugeText)
    }

    private val paintRpmGaugeActive = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#8e7cc3")
        strokeWidth = 11F//batteryGaugeWidth
        strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        setShadowLayer(10F,0F,0F, Color.parseColor("#8e7cc3"))
    }

    private val paintRpmGaugeFlask = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#8e7cc3")
        strokeWidth = 2F//batteryGaugeWidth
        strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        //setShadowLayer(10F,0F,0F, Color.parseColor("#8e7cc3"))
    }

    private fun drawRpmGauge(canvas: Canvas) {
        canvas.drawArc(
                innerBox,
                startDegAngle,//140F + (5 + 1) * i,
                mCurrentRpm,//129F
                false,
                paintRpmGaugeActive
        )
        canvas.drawArc(
            innerBoxFlask,
            startDegAngle,
            flaskDegSwipeAngle,
            false,
            paintRpmGaugeFlask
        )
        canvas.drawArc(
            innerBoxFlask2,
            startDegAngle,
            flaskDegSwipeAngle,
            false,
            paintRpmGaugeFlask
        )
    }
    private val paintTempGaugeActive = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#ff7b7b")
        strokeWidth = 11F//batteryGaugeWidth
        strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        setShadowLayer(10F,0F,0F, Color.parseColor("#ff7b7b"))
    }
    private val paintTempGaugeFlask = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#ff7b7b")
        strokeWidth = 2F//batteryGaugeWidth
        strokeCap = Paint.Cap.BUTT
        alpha = 255 //[0-255]
        //setShadowLayer(10F,0F,0F, Color.parseColor("#8e7cc3"))
    }
    private fun drawTempGauge(canvas: Canvas) {
        canvas.drawArc(
            innerBox,
            endDegAngle,
            -mCurrentEngineOilTemp,
            false,
            paintTempGaugeActive
        )
        canvas.drawArc(
            innerBoxFlask,
            endDegAngle,
            -flaskDegSwipeAngle,
            false,
            paintTempGaugeFlask
        )
        canvas.drawArc(
            innerBoxFlask2,
            endDegAngle,
            -flaskDegSwipeAngle,
            false,
            paintTempGaugeFlask
        )
    }
    private fun drawBorders(canvas: Canvas) {
        canvas.drawLine(getStartTicksXY(40F, true).x,getStartTicksXY(40F, true).y,getStartTicksXY(40F, false).x,getStartTicksXY(40F, false).y,paintRpmGaugeFlask)
        canvas.drawLine(getStartTicksXY(-37F, true).x,getStartTicksXY(-37F, true).y,getStartTicksXY(-37F, false).x,getStartTicksXY(-37F, false).y,paintRpmGaugeFlask)

        canvas.drawLine(getStartTicksXY(141F, true).x,getStartTicksXY(141F, true).y,getStartTicksXY(141F, false).x,getStartTicksXY(141F, false).y,paintTempGaugeFlask)
        canvas.drawLine(getStartTicksXY(218F, true).x,getStartTicksXY(218F, true).y,getStartTicksXY(218F, false).x,getStartTicksXY(218F, false).y,paintTempGaugeFlask)
    }
        override fun onDraw(canvas: Canvas) {
            drawSpeedText(canvas)
            drawBatteryGauge(canvas)
            drawRpmGauge(canvas)
            drawTempGauge(canvas)
            drawBorders(canvas)
        }

    fun setSpeed(currentSpeed: Int) {
        if (currentSpeed in 0..400) {
            animatorSpeed.apply {
                setIntValues(mCurrentSpeed, currentSpeed)
                addUpdateListener {
                    mCurrentSpeed = it.animatedValue as Int
                    postInvalidate()
                }
                start()
            }
        }
    }

    fun setBatteryLevel(currentBatteryLevel: Float) {
        var nInactiveCellsCurrent = (10 - round(currentBatteryLevel/mBatteryCapacity * 10)).toInt()
        animatorBattery.apply {
            setIntValues(batteryGaugeNInactiveCells, nInactiveCellsCurrent)
            addUpdateListener {
                batteryGaugeNInactiveCells = it.animatedValue as Int
                postInvalidate()
            }
            start()
        }
    }

   fun setBatteryCapacity(currentBatteryCapacity: Float) {
       mBatteryCapacity = currentBatteryCapacity
       postInvalidate()
   }

    fun setEngineRpm(currentEngineRpm: Float) {
        if (currentEngineRpm in 0.0..flaskDegSwipeAngle.toDouble()) {
            animatorRpm.apply {
                setFloatValues(mCurrentRpm, currentEngineRpm)
                addUpdateListener {
                    mCurrentRpm = it.animatedValue as Float
                    postInvalidate()
                }
                start()
            }
        }
    }

    fun setEngineOilTemp(currentEngineOilTemp: Float) {
        if (currentEngineOilTemp in 0.0..flaskDegSwipeAngle.toDouble()) {
            animatorOilTemp.apply {
                setFloatValues(mCurrentEngineOilTemp, currentEngineOilTemp)
                addUpdateListener {
                    mCurrentEngineOilTemp = it.animatedValue as Float
                    postInvalidate()
                }
                start()
            }
        }
    }
}
