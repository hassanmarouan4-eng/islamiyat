package org.dorossi.app.ui.platform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dorossi.app.data.platform.HostResolver

/** Address to open, and a counter that changes on every new attempt (retry). */
data class PlatformTarget(val base: String, val attempt: Int)

class PlatformViewModel(private val resolver: HostResolver) : ViewModel() {

    private val _target = MutableStateFlow<PlatformTarget?>(null)

    /** Null while the address is being chosen. */
    val target: StateFlow<PlatformTarget?> = _target.asStateFlow()

    private var attempts = 0
    private var job: Job? = null

    init {
        resolve()
    }

    /** Chooses the address (again) and asks the screen to open it. */
    fun resolve() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val base = resolver.resolve()
            attempts += 1
            _target.value = PlatformTarget(base, attempts)
        }
    }
}
