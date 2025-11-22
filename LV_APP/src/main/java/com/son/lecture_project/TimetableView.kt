package com.son.lecture_project

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.son.lecture_project.data.model.ClassSchedule

class TimetableView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs) {

    private var classes: List<ClassSchedule> = listOf()
    private var onClassClickListener: ((ClassSchedule) -> Unit)? = null

    private val START_HOUR = 9
    private val END_HOUR = 18
    private val DAYS = listOf("월", "화", "수", "목", "금")
    private val timeLabelWidth = dpToPx(50f)
    private val dayLabelHeight = dpToPx(30f)
    private val hourHeight = dpToPx(60f)

    private val linePaint = Paint().apply { color = Color.parseColor("#E0E0E0") }
    private val dayLabelPaint = TextPaint().apply { color = Color.BLACK; textSize = dpToPx(14f); textAlign = Paint.Align.CENTER }
    private val timeLabelPaint = TextPaint().apply { color = Color.DKGRAY; textSize = dpToPx(12f); textAlign = Paint.Align.CENTER }
    private val classBlockPaint = Paint()
    private val classTextPaint = TextPaint().apply { color = Color.WHITE; textSize = dpToPx(12f) }
    
    // 수업 블록의 화면상 좌표를 저장하기 위한 리스트
    private val classRects = mutableListOf<Pair<RectF, ClassSchedule>>()

    fun setClasses(newClasses: List<ClassSchedule>) {
        this.classes = newClasses
        requestLayout()
        invalidate()
    }

    fun setOnClassClickListener(listener: (ClassSchedule) -> Unit) {
        this.onClassClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalHeight = (dayLabelHeight + (END_HOUR - START_HOUR + 1) * hourHeight).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), totalHeight)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            classRects.forEach { (rect, classInfo) ->
                if (rect.contains(event.x, event.y)) {
                    onClassClickListener?.invoke(classInfo)
                    return true
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        classRects.clear()

        val dayColumnWidth = (width - timeLabelWidth) / DAYS.size

        // 요일 라벨
        DAYS.forEachIndexed { index, day ->
            val x = timeLabelWidth + index * dayColumnWidth + dayColumnWidth / 2
            canvas.drawText(day, x, dayLabelHeight / 2 - (dayLabelPaint.descent() + dayLabelPaint.ascent()) / 2, dayLabelPaint)
        }

        // 시간 라벨 및 가로선
        for (hour in START_HOUR..END_HOUR) {
            val y = dayLabelHeight + (hour - START_HOUR) * hourHeight
            canvas.drawText(String.format("%02d", hour), timeLabelWidth / 2, y + hourHeight / 2 - (timeLabelPaint.descent() + timeLabelPaint.ascent()) / 2, timeLabelPaint)
            canvas.drawLine(timeLabelWidth, y, width.toFloat(), y, linePaint)
        }
        val finalY = dayLabelHeight + (END_HOUR - START_HOUR + 1) * hourHeight
        canvas.drawLine(timeLabelWidth, finalY, width.toFloat(), finalY, linePaint)

        // 세로선
        for (i in 0..DAYS.size) {
            val x = timeLabelWidth + i * dayColumnWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
        }

        // 수업 블록
        classes.forEach { classInfo ->
            val dayIndex = getDayIndex(classInfo.day)
            if (dayIndex < 0) return@forEach

            val startHour = parseTime(classInfo.startTime)
            val endHour = parseTime(classInfo.endTime)
            if (startHour < 0 || endHour < 0 || startHour >= endHour) return@forEach

            val left = timeLabelWidth + dayIndex * dayColumnWidth
            val top = dayLabelHeight + (startHour - START_HOUR) * hourHeight
            val right = left + dayColumnWidth
            val bottom = dayLabelHeight + (endHour - START_HOUR) * hourHeight

            val rect = RectF(left + 2, top + 2, right - 2, bottom - 2)
            classBlockPaint.color = Color.parseColor(classInfo.color)
            canvas.drawRect(rect, classBlockPaint)
            
            // 클릭 감지를 위해 좌표와 정보 저장
            classRects.add(rect to classInfo)

            // 텍스트
            val text = "${classInfo.name}\n${classInfo.startTime}"
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, classTextPaint, (rect.width() - dpToPx(8f)).toInt()).build()
            canvas.save()
            canvas.translate(rect.left + (rect.width() - staticLayout.width) / 2, rect.top + (rect.height() - staticLayout.height) / 2)
            staticLayout.draw(canvas)
            canvas.restore()
        }
    }

    private fun getDayIndex(day: String): Int = when { day.contains("월") -> 0; day.contains("화") -> 1; day.contains("수") -> 2; day.contains("목") -> 3; day.contains("금") -> 4; else -> -1 }
    private fun parseTime(timeStr: String): Float = try { val p = timeStr.split(":"); p[0].toInt() + (if (p.size > 1) p[1].toInt() else 0) / 60f } catch (e: Exception) { -1f }
    private fun dpToPx(dp: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
}
