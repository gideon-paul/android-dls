package com.zoomcar.uikit.driverscore

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.zoomcar.util.getNullCheck
import com.zoomcar.zoomdls.R

/**
 * Originally authored by Shishir.
 * Modified by Gideon Paul.
 */
class PerformanceMeter @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: ZMeterUIModel? = null

    private var path: Path = Path()
    private val pointerDrawable = ContextCompat.getDrawable(context, R.drawable.ic_chevron_down)

    private val marginTopBottomForMeter =
        context.resources.getDimension(R.dimen.meter_top_bottom_margin)
    private val marginTopForPointer =
        context.resources.getDimension(R.dimen.meter_top_margin_for_pointer)
    private val pointerSize = context.resources.getDimensionPixelSize(R.dimen.meter_pointer_size)
    private val barMeterHeight = context.resources.getDimension(R.dimen.bar_meter_height)
    private val cornerRadius = context.resources.getDimension(R.dimen.meter_corner_radius)

    private val cornerEffect = CornerPathEffect(cornerRadius)

    // Red paint for bad category
    private val redPaint =
        Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.fire_red_06)
            style = Paint.Style.FILL
            //added for curved edges
            pathEffect = cornerEffect;
        }

    // Orange paint for average category
    private val orangePaint =
        Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.sunrise_yellow_04)
            style = Paint.Style.FILL
        }

    // Green paint for good category
    private val greenPaint =
        Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.ever_green_06)
            style = Paint.Style.FILL
            //added for curved edges
            pathEffect = cornerEffect;
        }

    // Paint for text inside the colored bar
    private val textPaint =
        Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.white)
            textAlign = Paint.Align.CENTER
            textSize = context.resources.getDimension(R.dimen.performance_meter_label_text_size)
        }

    // Paint for text for middle colored bars bottom label as text alignment is needed center
    private val labelPaint =
        Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.phantom_grey_04)
            textAlign = Paint.Align.CENTER
            textSize = context.resources.getDimension(R.dimen.performance_meter_label_text_size)
        }

    fun setData(data: ZMeterUIModel?) {
        data?.let {
            this.data = data
            this.isVisible = data.items.getNullCheck()
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lp = layoutParams.apply {
            height = context.resources.getDimensionPixelSize(R.dimen.performance_meter_height)
        }
        layoutParams = lp
        setWillNotDraw(false)
    }

    private fun getPaintForCategory(category: String?): Paint {
        return when (DriverScoreCategoryType.fromType(category)) {
            DriverScoreCategoryType.GOOD -> greenPaint
            DriverScoreCategoryType.AVERAGE -> orangePaint
            else -> redPaint
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { canvas ->
            data?.items?.mapIndexed { index, item ->
                val top = marginTopBottomForMeter
                val bottom = barMeterHeight + marginTopBottomForMeter
                val left = (item.low ?: 0) * 1.0f / 100.0f * measuredWidth.toFloat()
                val right = (item.high ?: 0) * 1.0f / 100.0f * measuredWidth.toFloat()
                path.reset()

                when (index) {
                    0 -> {
                        //path drawn considering curved edges
                        path.moveTo(right, top)
                        path.lineTo(left, top)
                        path.lineTo(left, bottom)
                        path.lineTo(right, bottom)
                    }
                    (data?.items?.size ?: 0) - 1 -> {
                        //path drawn considering curved edges
                        path.moveTo(left, top)
                        path.lineTo(right, top)
                        path.lineTo(right, bottom)
                        path.lineTo(left, bottom)
                    }
                    else -> {
                        path.moveTo(left, top)
                        path.lineTo(right, top)
                        path.lineTo(right, bottom)
                        path.lineTo(left, bottom)
                    }
                }

                val paint = getPaintForCategory(item.category)
                canvas.drawPath(path, paint)

                //add labels inside bars
                var xPos = (left + right) / 2
                var yPos = ((top + bottom) / 2) - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(item.text ?: "", xPos, yPos, textPaint)

                //add labels below bar
                yPos = bottom + barMeterHeight * 3 / 5
                if (index == 0) {
                    xPos = left + 2
                    canvas.drawText(item.low.toString(), xPos, yPos, labelPaint.apply {
                        textAlign = Paint.Align.LEFT
                    })
                }
                if (index == (data?.items?.size ?: 0) - 1) {
                    xPos = right - 2
                    canvas.drawText(item.high.toString(), xPos, yPos, labelPaint.apply {
                        textAlign = Paint.Align.RIGHT
                    })
                } else {
                    xPos = right
                    canvas.drawText(item.high.toString(), xPos, yPos, labelPaint.apply {
                        textAlign = Paint.Align.CENTER
                    })
                }
            }

            //add pointer for score
            val pointerX = width * ((data?.score ?: 0) * 1.0f / 100.0f) - pointerSize / 2
            val pointerY = marginTopForPointer
            canvas.translate(pointerX, pointerY)
            pointerDrawable?.apply {
                setBounds(0, 0, pointerSize, pointerSize)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setTint(getDriverScoreColor(context, data?.category))
                }
                draw(canvas)
            }
            super.onDraw(canvas)
        }
    }

    /**
     * @param categoryType Enum value [DriverScoreCategoryType]
     * @return color int resource
     */
    @ColorInt
    private fun getDriverScoreColor(context: Context, categoryType: DriverScoreCategoryType?): Int {
        categoryType?.let {
            return when (categoryType) {
                DriverScoreCategoryType.GOOD -> ContextCompat.getColor(
                    context,
                    R.color.ever_green_06
                )
                DriverScoreCategoryType.AVERAGE -> ContextCompat.getColor(
                    context,
                    R.color.sunrise_yellow_04
                )
                DriverScoreCategoryType.BAD -> ContextCompat.getColor(context, R.color.fire_red_06)
                DriverScoreCategoryType.UNKNOWN -> ContextCompat.getColor(
                    context,
                    R.color.phantom_grey_08
                )
            }
        } ?: return ContextCompat.getColor(context, R.color.phantom_grey_08)
    }
}

// Value objects.
data class RankScaleVO(
    var low: Int? = null,
    var high: Int? = null,
    var category: String? = null,
    var text: String? = null,
)

data class ZMeterUIModel(
    val items: List<RankScaleVO>? = null,
    val score: Int,
    val category: DriverScoreCategoryType,
)
