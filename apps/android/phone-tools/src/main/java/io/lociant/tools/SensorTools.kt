package io.lociant.tools

import android.content.Context
import io.lociant.core.tools.ToolDefinition
import io.lociant.core.tools.ToolPolicy
import io.lociant.core.tools.ToolProvider
import io.lociant.core.tools.arrayParam
import io.lociant.core.tools.intParam
import io.lociant.core.tools.stringParam
import io.lociant.core.tools.tool
import io.lociant.tools.runtime.SensorMonitor
import org.json.JSONObject

class SensorTools(
    private val context: Context,
) : ToolProvider {
    init {
        SensorMonitor.attach(context.applicationContext)
    }

    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "sensor_status",
            description = "List the Android phone's available sensors and return latest aggregated summaries for any active continuous monitoring.",
            policy = ToolPolicy(requiresActivity = true),
        ) { SensorMonitor.status() },
        tool(
            name = "sensor_read",
            description = "Sample a phone sensor once and return an aggregated summary (mean, variance, min, max per axis). Types: accelerometer, gyroscope, magnetic, gravity, linear_acceleration, rotation, light, proximity, pressure, temperature, humidity, step_counter, and more.",
            properties = JSONObject()
                .put("type", stringParam("Sensor type name (e.g. accelerometer, gyroscope, light, proximity, pressure, magnetic) or numeric type code."))
                .put("samples", intParam("Number of samples to collect. Default 5."))
                .put("intervalMs", intParam("Delay between samples in milliseconds. Default 100.")),
            policy = ToolPolicy(requiresActivity = true),
        ) { args -> SensorMonitor.read(args) },
        tool(
            name = "sensor_start",
            description = "Start continuous monitoring of one or more phone sensors. Read the latest summaries with sensor_status and stop with sensor_stop.",
            properties = JSONObject()
                .put("types", arrayParam("Sensor type names to monitor continuously.", stringParam()))
                .put("intervalMs", intParam("Sampling interval in milliseconds. Default 200.")),
            policy = ToolPolicy(requiresActivity = true, sideEffect = true, openWorld = true),
        ) { args -> SensorMonitor.start(args) },
        tool(
            name = "sensor_stop",
            description = "Stop continuous sensor monitoring.",
            policy = ToolPolicy(requiresActivity = true, sideEffect = true, openWorld = true),
        ) { SensorMonitor.stop() },
    )
}
