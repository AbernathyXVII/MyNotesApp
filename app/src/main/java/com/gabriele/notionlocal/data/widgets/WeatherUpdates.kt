package com.gabriele.notionlocal.data.widgets

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * **Quando si riscarica il meteo**: solo con la barra laterale aperta (lo
 * chiede il widget, vedi `WeatherView`), e per ogni città solo se quello che
 * c'è ha più di mezz'ora (`isStale`) — o subito, con "Refresh". Una città
 * già in arrivo non si chiede due volte.
 *
 * `failed`: le città la cui ultima domanda non ha avuto risposta (niente
 * rete): il widget lo dice, e intanto mostra il meteo che aveva.
 */
object WeatherUpdates {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val inFlight = mutableSetOf<String>()

    private val failedState = mutableStateOf<Set<String>>(emptySet())
    val failed: Set<String> get() = failedState.value

    private val loadingState = mutableStateOf<Set<String>>(emptySet())
    val loading: Set<String> get() = loadingState.value

    fun refresh(widget: WeatherWidget, force: Boolean = false, now: Long = System.currentTimeMillis()) {
        widget.cities.forEach { city ->
            if (city.id in inFlight) return@forEach
            if (!force && !widget.reports[city.id].isStale(now)) return@forEach
            inFlight += city.id
            loadingState.value = loadingState.value + city.id
            scope.launch {
                val report = WeatherApi.forecast(city)
                if (report != null) {
                    WidgetStore.setWeatherReport(widget.id, city.id, report)
                    failedState.value = failedState.value - city.id
                } else {
                    failedState.value = failedState.value + city.id
                }
                inFlight -= city.id
                loadingState.value = loadingState.value - city.id
            }
        }
    }
}
