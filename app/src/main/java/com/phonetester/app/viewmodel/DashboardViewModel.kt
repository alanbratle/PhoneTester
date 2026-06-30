package com.phonetester.app.viewmodel

import androidx.lifecycle.ViewModel
import com.phonetester.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DashboardViewModel : ViewModel() {

    private val _results = MutableStateFlow(
        TestCategory.all.associateWith { TestResult(it) }
    )
    val results: StateFlow<Map<TestCategory, TestResult>> = _results.asStateFlow()

    private val _isRunningAll = MutableStateFlow(false)
    val isRunningAll: StateFlow<Boolean> = _isRunningAll.asStateFlow()

    fun updateResult(category: TestCategory, result: TestResult) {
        _results.update { it + (category to result) }
    }

    fun resetResults() {
        _results.value = TestCategory.all.associateWith { TestResult(it) }
    }

    fun setRunningAll(running: Boolean) {
        _isRunningAll.value = running
    }

    val passedCount: Int
        get() = _results.value.values.count { it.status == TestStatus.PASSED }

    val failedCount: Int
        get() = _results.value.values.count { it.status == TestStatus.FAILED }

    val skippedCount: Int
        get() = _results.value.values.count { it.status == TestStatus.SKIPPED || it.status == TestStatus.PENDING }

    val totalTests: Int
        get() = TestCategory.all.size

    val progress: Float
        get() = if (TestCategory.all.isEmpty()) 0f
        else _results.value.values.count {
            it.status == TestStatus.PASSED || it.status == TestStatus.FAILED || it.status == TestStatus.SKIPPED
        }.toFloat() / TestCategory.all.size
}