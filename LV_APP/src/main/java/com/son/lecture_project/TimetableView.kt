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
import androidx.core.content.ContextCompat
import com.son.lecture_project.data.model.ClassSchedule

/**
 * 주간 시간표를 표시하는 커스텀 뷰.
 *
 * [기능]
 * 1. 시간표 그리드(Grid) 그리기
 * 2. 수업 블록(Class Block) 배치 및 그리기
 * 3. 터치 이벤트 감지 및 콜백 처리
 *
 * [리팩토링 반영 사항]
 * - onDraw 메서드의 복잡도를 낮추기 위해 그리기 로직을 하위 메서드로 분리함.
 * - (KISS 원칙 적용)
 */
class TimetableView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs) {

    // --- 데이터 ---
    private var classes: List<ClassSchedule> = listOf()
    private var onClassClickListener: ((ClassSchedule) -> Unit)? = null

    // --- 상수 및 설정 ---
    private val START_HOUR = 9
    private val END_HOUR = 18
    private val DAYS = listOf("월", "화", "수", "목", "금")
    
    // --- 치수 (Dimensions) ---
    private val timeLabelWidth = dpToPx(50f)
    private val dayLabelHeight = dpToPx(30f)
    private val hourHeight = dpToPx(60f)

    // --- 페인트 객체 (Paints) ---
    private val linePaint = Paint().apply { color = Color.parseColor("#E0E0E0") }
    private val dayLabelPaint = TextPaint().apply { 
        color = ContextCompat.getColor(context, R.color.text_primary) // 리소스 컬러 사용
        textSize = dpToPx(14f)
        textAlign = Paint.Align.CENTER 
    }
    private val timeLabelPaint = TextPaint().apply { 
        color = ContextCompat.getColor(context, R.color.text_secondary) // 리소스 컬러 사용
        textSize = dpToPx(12f)
        textAlign = Paint.Align.CENTER 
    }
    private val classBlockPaint = Paint()
    private val classTextPaint = TextPaint().apply { 
        color = Color.WHITE
        textSize = dpToPx(11f) // 글자 크기 약간 줄임 (정보량 증가 대응)
    }

    // 터치 영역 감지용 리스트
    private val classRects = mutableListOf<Pair<RectF, ClassSchedule>>()

    // --- Public Methods ---

    /**
     * 시간표 데이터를 설정하고 화면을 갱신합니다.
     */
    fun setClasses(newClasses: List<ClassSchedule>) {
        this.classes = newClasses
        requestLayout()
        invalidate()
    }

    /**
     * 수업 클릭 리스너를 설정합니다.
     */
    fun setOnClassClickListener(listener: (ClassSchedule) -> Unit) {
        this.onClassClickListener = listener
    }

    // --- View Lifecycle ---

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 뷰의 전체 높이 계산: 상단 라벨 + (시간 수 * 시간당 높이)
        val totalHeight = (dayLabelHeight + (END_HOUR - START_HOUR + 1) * hourHeight).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), totalHeight)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            // 터치된 좌표가 수업 블록 내에 있는지 확인
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
        classRects.clear() // 그리기 전 터치 영역 초기화

        val dayColumnWidth = (width - timeLabelWidth) / DAYS.size

        drawDayLabels(canvas, dayColumnWidth)
        drawTimeGrid(canvas)
        drawVerticalLines(canvas, dayColumnWidth)
        drawClassBlocks(canvas, dayColumnWidth)
    }

    // --- Private Drawing Helpers (모듈화) ---

    private fun drawDayLabels(canvas: Canvas, columnWidth: Float) {
        DAYS.forEachIndexed { index, day ->
            val x = timeLabelWidth + index * columnWidth + columnWidth / 2
            val y = dayLabelHeight / 2 - (dayLabelPaint.descent() + dayLabelPaint.ascent()) / 2
            canvas.drawText(day, x, y, dayLabelPaint)
        }
    }

    private fun drawTimeGrid(canvas: Canvas) {
        // 시간 텍스트 및 가로선
        for (hour in START_HOUR..END_HOUR) {
            val y = dayLabelHeight + (hour - START_HOUR) * hourHeight
            // 텍스트 수직 중앙 정렬 계산
            val textY = y + hourHeight / 2 - (timeLabelPaint.descent() + timeLabelPaint.ascent()) / 2
            
            canvas.drawText(String.format("%02d", hour), timeLabelWidth / 2, textY, timeLabelPaint)
            canvas.drawLine(timeLabelWidth, y, width.toFloat(), y, linePaint)
        }
        
        // 마지막 하단 선
        val finalY = dayLabelHeight + (END_HOUR - START_HOUR + 1) * hourHeight
        canvas.drawLine(timeLabelWidth, finalY, width.toFloat(), finalY, linePaint)
    }

    private fun drawVerticalLines(canvas: Canvas, columnWidth: Float) {
        for (i in 0..DAYS.size) {
            val x = timeLabelWidth + i * columnWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
        }
    }

    private fun drawClassBlocks(canvas: Canvas, columnWidth: Float) {
        classes.forEach { classInfo ->
            val dayIndex = getDayIndex(classInfo.day)
            if (dayIndex < 0) return@forEach

            val startHour = parseTime(classInfo.startTime)
            val endHour = parseTime(classInfo.endTime)
            
            // 유효하지 않은 시간은 스킵
            if (startHour < 0 || endHour < 0 || startHour >= endHour) return@forEach

            // 좌표 계산
            val left = timeLabelWidth + dayIndex * columnWidth
            val top = dayLabelHeight + (startHour - START_HOUR) * hourHeight
            val right = left + columnWidth
            val bottom = dayLabelHeight + (endHour - START_HOUR) * hourHeight

            // 여백을 둔 사각형 생성
            val rect = RectF(left + 2, top + 2, right - 2, bottom - 2)
            
            // 블록 그리기
            classBlockPaint.color = Color.parseColor(classInfo.color)
            canvas.drawRect(rect, classBlockPaint)

            // 터치 감지용 리스트에 추가
            classRects.add(rect to classInfo)

            // 텍스트 그리기
            drawClassText(canvas, rect, classInfo)
        }
    }

    private fun drawClassText(canvas: Canvas, rect: RectF, classInfo: ClassSchedule) {
        // 수업명, 시작~종료 시간, 총 인원수 표시
        val totalInfo = if (classInfo.totalStudents > 0) "\n(총 ${classInfo.totalStudents}명)" else ""
        val text = "${classInfo.name}\n${classInfo.startTime}~${classInfo.endTime}$totalInfo"
        
        // 텍스트가 영역을 벗어나지 않도록 StaticLayout 사용
        val textWidth = (rect.width() - dpToPx(8f)).toInt()
        if (textWidth > 0) {
            val staticLayout = StaticLayout.Builder.obtain(
                text, 0, text.length, classTextPaint, textWidth
            )
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER) // 가운데 정렬
            .build()
            
            canvas.save()
            // 텍스트를 블록 중앙에 위치
            canvas.translate(
                rect.left + (rect.width() - staticLayout.width) / 2, 
                rect.top + (rect.height() - staticLayout.height) / 2
            )
            staticLayout.draw(canvas)
            canvas.restore()
        }
    }

    // --- Utilities ---

    private fun getDayIndex(day: String): Int {
        return when {
            day.contains("월") -> 0
            day.contains("화") -> 1
            day.contains("수") -> 2
            day.contains("목") -> 3
            day.contains("금") -> 4
            else -> -1
        }
    }

    private fun parseTime(timeStr: String): Float {
        return try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = if (parts.size > 1) parts[1].toInt() else 0
            hour + minute / 60f
        } catch (e: Exception) {
            -1f
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
        )
    }
}
