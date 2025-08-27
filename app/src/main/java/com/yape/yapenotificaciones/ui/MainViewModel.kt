package com.yape.yapenotificaciones.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yape.yapenotificaciones.data.YapeRepository
import com.yape.yapenotificaciones.data.Yapeo
import com.yape.yapenotificaciones.util.startEndOfDay
import com.yape.yapenotificaciones.util.startEndOfMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = YapeRepository(app)

    val yapeos = repo.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun borrarTodo(onDone: () -> Unit = {}) = viewModelScope.launch {
        repo.clearAll()
        onDone()
    }

    suspend fun getDayItems(day: LocalDate): List<Yapeo> {
        val (s, e) = startEndOfDay(day)
        return repo.getBetween(s, e, onlyReceived = true)
    }

    suspend fun getMonthItems(ym: YearMonth): List<Yapeo> {
        val (s, e) = startEndOfMonth(ym)
        return repo.getBetween(s, e, onlyReceived = true)
    }

    // Debug helper: inserta un ejemplo sin notificación real
    fun insertFake(nowMillis: Long, rawText: String, pkg: String = "pe.bcp.yape") = viewModelScope.launch {
        repo.insertParsed(pkg, nowMillis, rawText)
    }
}
