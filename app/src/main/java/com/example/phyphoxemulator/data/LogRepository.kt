package com.example.phyphoxemulator.data

import com.example.phyphoxemulator.domain.LogEntry
import com.example.phyphoxemulator.domain.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LogRepository {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun add(level: LogLevel, message: String) {
        _entries.value = (_entries.value + LogEntry(level = level, message = message)).takeLast(500)
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
