package com.son.lecture_project

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.son.lecture_project.data.api.ModelApiService
import com.son.lecture_project.data.api.RecordApiService
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.api.TicketApiService
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.ApiResponseDetails
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.MeasureResponse
import com.son.lecture_project.data.model.Ticket
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.ui.home.HomeViewModel
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.records.RecordsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
class SystemTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // ViewModels
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recordsViewModel: RecordsViewModel

    // APIs
    private lateinit var mockTicketApi: TicketApiService
    private lateinit var mockModelApi: ModelApiService
    private lateinit var mockRecordApi: RecordApiService

    // Android Mocks
    private lateinit var mockApplication: Application
    private lateinit var mockSharedPreferences: SharedPreferences

    private val testDispatcher = StandardTestDispatcher()

    // 시나리오 가정
    private val TOTAL_STUDENTS_HOME = 6
    private val TOTAL_STUDENTS_RECORD = 30
    private val TARGET_SUBJECT = "알고리즘"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // [Log Mocking]
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // 1. Android Mocks
        mockApplication = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        
        // Context.MODE_PRIVATE가 0이므로 any()로 처리하거나 명시
        every { mockApplication.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.getString(any(), any()) } returns "test_access_token"
        
        // 2. API Mocks
        mockTicketApi = mockk(relaxed = true)
        mockModelApi = mockk(relaxed = true)
        mockRecordApi = mockk(relaxed = true)

        // 3. RetrofitClient Mocking
        mockkObject(RetrofitClient)
        every { RetrofitClient.ticketApiService } returns mockTicketApi
        every { RetrofitClient.modelApiService } returns mockModelApi
        every { RetrofitClient.recordApiService } returns mockRecordApi
        
        // 4. TokenManager Mocking
        TokenManager.init(mockApplication) 
        mockkObject(TokenManager)
        every { TokenManager.getToken() } returns "test_access_token"

        // 5. ViewModels 생성
        homeViewModel = HomeViewModel(mockApplication)
        recordsViewModel = RecordsViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `1_인원_측정_시나리오_테스트`() = runTest {
        println("==================================================")
        println("[시스템 테스트 1] 인원 측정 기능 (HomeViewModel)")
        println("==================================================\n")

        val mockTicket = Ticket("virtual_ticket_1", "2024-11-26", "valid", "client_1")
        coEvery { mockTicketApi.createTicket(any()) } returns Response.success(mockTicket)

        val resStart = MeasureResponse(5, ApiResponseDetails("success", "id1"))
        val resEnd = MeasureResponse(4, ApiResponseDetails("success", "id2"))
        coEvery { mockModelApi.measure(any()) } returnsMany listOf(Response.success(resStart), Response.success(resEnd))

        println("Step 1: 측정 시작")
        homeViewModel.issueTicketAndMeasure(isStart = true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val startResult = homeViewModel.measurementResult.value?.peekContent()
        assertTrue(startResult is Result.Success)
        assertEquals(5, (startResult as Result.Success).data)
        println("   -> 측정 결과: 5명 (성공)")

        println("Step 2: 측정 종료")
        homeViewModel.issueTicketAndMeasure(isStart = false)
        testDispatcher.scheduler.advanceUntilIdle()

        val endResult = homeViewModel.measurementResult.value?.peekContent()
        assertTrue(endResult is Result.Success)
        assertEquals(4, (endResult as Result.Success).data)
        println("   -> 측정 결과: 4명 (성공)")

        println("Step 3: 비교 확인")
        val comparison = homeViewModel.comparisonResult.value?.peekContent()
        assertEquals(5, comparison!!.startCount)
        assertEquals(4, comparison.endCount)
        println("   -> 비교: 5명 -> 4명 (성공)")
        println("[테스트 1 완료]\n")
    }

    @Test
    fun `2_인원_조회_기능_테스트`() = runTest {
        println("==================================================")
        println("[시스템 테스트 2] 인원 조회 및 통계 기능 (RecordsViewModel)")
        println("가정: 과목 '$TARGET_SUBJECT' / 총 정원 ${TOTAL_STUDENTS_RECORD}명")
        println("==================================================\n")

        // [GIVEN 1] 가상 API 응답 설정
        val mockRecordList = listOf(
            Upload(
                id = 101,
                originalName = TARGET_SUBJECT,
                storedName = "stored_file.jpg",
                absPath = "/path/to/file",
                peopleCount = 25, // 출석 인원
                uploadedAt = "2024-11-27T10:00:00",
                clientId = "client_1"
            )
        )
        coEvery { mockRecordApi.getRecords(any()) } returns Response.success(mockRecordList)
        
        // [GIVEN 2] 총 인원 정보 주입 (Reflection 사용 - 강제 주입)
        // SharedPreferences Mocking이 불안정하여 직접 주입 방식 사용
        val schedules = listOf(
            ClassSchedule(
                id = 1,
                name = TARGET_SUBJECT,
                day = "Mon",
                startTime = "09:00",
                endTime = "11:00",
                classroom = "Room A",
                color = "#FF0000",
                totalStudents = TOTAL_STUDENTS_RECORD
            )
        )
        
        // Private field 'cachedSchedules'에 접근하여 데이터 주입
        try {
            val field: Field = RecordsViewModel::class.java.getDeclaredField("cachedSchedules")
            field.isAccessible = true
            field.set(recordsViewModel, schedules)
            println("   -> [System] 강제로 시간표 데이터 주입 완료")
        } catch (e: Exception) {
            fail("Reflection을 통한 데이터 주입 실패: ${e.message}")
        }

        // [WHEN 1] 과목 정보 로드 (여기서는 주입된 데이터를 바탕으로 동작 확인)
        // loadSubjectList는 SharedPreferences를 다시 읽어버리므로, 
        // 이미 주입한 cachedSchedules를 사용하는 메서드(getSubjectTotalCountMap)를 바로 호출하여 검증
        
        println("Step 1: 로컬 시간표 데이터 확인")
        
        // 총 인원 매핑 확인
        val countMap = recordsViewModel.getSubjectTotalCountMap()
        val totalStudents = countMap[TARGET_SUBJECT] ?: 0
        
        println("   -> 과목 '$TARGET_SUBJECT'의 총 정원 로드: ${totalStudents}명")
        assertEquals("총 정원은 ${TOTAL_STUDENTS_RECORD}명이어야 합니다.", TOTAL_STUDENTS_RECORD, totalStudents)


        // [WHEN 2] 기록 조회 요청
        println("\nStep 2: 서버에 기록 조회 요청")
        recordsViewModel.loadRecords()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val recordsResult = recordsViewModel.records.value
        
        if (recordsResult is Result.Error) {
            fail("기록 조회 실패: ${recordsResult.exception.message}")
        }
        
        assertTrue("조회가 성공해야 합니다.", recordsResult is Result.Success)
        val records = (recordsResult as Result.Success).data
        
        println("   -> 조회된 기록 수: ${records.size}개")
        assertEquals(1, records.size)
        
        // [THEN] 결과 검증 (UI에 표시될 데이터 계산)
        val record = records[0]
        val measuredCount = record.peopleCount
        val absentCount = totalStudents - measuredCount
        
        println("\nStep 3: 조회 결과 및 통계 검증")
        println("   [UI 예상 출력 결과]")
        println("   - 과목이름: ${record.originalName}")
        println("   - 총인원수: ${totalStudents}명")
        println("   - 측정인원(출석): ${measuredCount}명")
        println("   - 결석수: ${absentCount}명")
        
        assertEquals("과목명이 일치해야 합니다.", TARGET_SUBJECT, record.originalName)
        assertEquals("측정(출석) 인원이 일치해야 합니다.", 25, measuredCount)
        assertEquals("결석 수가 정확히 계산되어야 합니다.", 5, absentCount) // 30 - 25 = 5
        
        println("\n==================================================")
        println("[테스트 2 결과] 인원 조회 및 통계 검증 완료: 성공")
        println("==================================================")
    }
}
