package io.lociant.tools.runtime

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * In-process sensor monitoring for the Lociant tool layer.
 *
 * Raw high-frequency readings are meaningless to an LLM, so this monitor always
 * exposes *aggregated summaries* (mean / variance / min / max per axis) instead of
 * raw streams. `sensor_read` performs a bounded one-shot sample; `sensor_start`
 * keeps a small rolling window per active sensor that `sensor_status` can read.
 */
object SensorMonitor {
    private const val TAG = "LociantSensors"
    private const val MAX_BUFFER = 32
    private const val MAX_READ_MS = 8000L

    @Volatile private var sensorManager: SensorManager? = null
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private val active = LinkedHashMap<Int, SensorBuffer>()
    private val activeIntervalMs = LinkedHashMap<Int, Int>()

    @Synchronized
    fun attach(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }
        if (sensorThread == null) {
            sensorThread = HandlerThread("lociant-sensors").apply { start() }
            sensorHandler = Handler(sensorThread!!.looper)
        }
    }

    fun read(args: JSONObject): JSONObject {
        val manager = sensorManager ?: return error("sensor_unavailable", "SensorManager is unavailable")
        val typeName = args.optString("type").trim().lowercase()
        val typeCode = typeName.toIntOrNull() ?: nameToCode(typeName)
            ?: return error("sensor_unknown_type", "Unknown sensor type: ${typeName.ifBlank { "<empty>" }}")
        val sensor = manager.getDefaultSensor(typeCode)
            ?: return error("sensor_not_found", "No ${typeName.ifBlank { sensorName(typeCode) }} sensor on this device")
        val requested = args.optInt("samples", 5).coerceIn(1, 200)
        val intervalMs = args.optInt("intervalMs", 100).coerceIn(10, 10000)
        val buffer = SensorBuffer(typeCode)
        val registered = manager.registerListener(buffer, sensor, intervalMs * 1000, sensorHandler)
        if (!registered) return error("sensor_busy", "Failed to register ${sensorName(typeCode)} sensor listener")
        val started = System.currentTimeMillis()
        try {
            val deadline = started + (intervalMs.toLong() * requested).coerceIn(300L, MAX_READ_MS)
            while (buffer.count() < requested && System.currentTimeMillis() < deadline) {
                Thread.sleep(5)
            }
        } finally {
            manager.unregisterListener(buffer)
        }
        val collected = buffer.snapshot()
        return JSONObject()
            .put("ok", collected.isNotEmpty())
            .put("type", sensorName(typeCode))
            .put("typeCode", typeCode)
            .put("samplesRequested", requested)
            .put("samplesCollected", collected.size)
            .put("intervalMs", intervalMs)
            .put("elapsedMs", System.currentTimeMillis() - started)
            .put("summary", summarize(sensor, collected))
            .put("sensor", sensorJson(sensor))
    }

    fun start(args: JSONObject): JSONObject {
        val manager = sensorManager ?: return error("sensor_unavailable", "SensorManager is unavailable")
        val requested = args.optJSONArray("types") ?: JSONArray().put("accelerometer")
        val intervalMs = args.optInt("intervalMs", 200).coerceIn(50, 60000)
        val started = ArrayList<String>()
        for (index in 0 until requested.length()) {
            val name = requested.optString(index).trim().lowercase()
            val code = nameToCode(name) ?: continue
            val sensor = manager.getDefaultSensor(code) ?: continue
            if (synchronized(active) { active.containsKey(code) }) continue
            val buffer = SensorBuffer(code)
            val registered = manager.registerListener(buffer, sensor, intervalMs * 1000, sensorHandler)
            if (registered) {
                synchronized(active) {
                    if (active.containsKey(code)) {
                        manager.unregisterListener(buffer)
                    } else {
                        active[code] = buffer
                        activeIntervalMs[code] = intervalMs
                        started.add(sensorName(code))
                    }
                }
            }
        }
        return JSONObject()
            .put("ok", started.isNotEmpty())
            .put("started", JSONArray(started))
            .put("active", JSONArray(active.keys.map { sensorName(it) }))
            .put("intervalMs", intervalMs)
    }

    fun stop(): JSONObject {
        val manager = sensorManager ?: return error("sensor_unavailable", "SensorManager is unavailable")
        val stopped = ArrayList<String>()
        synchronized(active) {
            active.keys.forEach { code ->
                manager.unregisterListener(active.remove(code))
                activeIntervalMs.remove(code)
                stopped.add(sensorName(code))
            }
        }
        return JSONObject()
            .put("ok", true)
            .put("stopped", JSONArray(stopped))
            .put("active", 0)
    }

    fun status(): JSONObject {
        val manager = sensorManager ?: return error("sensor_unavailable", "SensorManager is unavailable")
        val sensors = JSONArray()
        manager.getSensorList(Sensor.TYPE_ALL).forEach { sensors.put(sensorJson(it)) }
        val monitoring = JSONArray()
        synchronized(active) {
            active.forEach { (code, buffer) ->
                val sensor = manager.getDefaultSensor(code) ?: return@forEach
                monitoring.put(JSONObject()
                    .put("type", sensorName(code))
                    .put("typeCode", code)
                    .put("intervalMs", activeIntervalMs[code] ?: 0)
                    .put("summary", summarize(sensor, buffer.snapshot())))
            }
        }
        return JSONObject()
            .put("ok", true)
            .put("count", sensors.length())
            .put("sensors", sensors)
            .put("monitoring", JSONObject()
                .put("active", active.isNotEmpty())
                .put("sensors", monitoring))
    }

    // ---- helpers ----

    private class SensorBuffer(private val typeCode: Int) : SensorEventListener {
        private val events = ArrayDeque<FloatArray>()

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != typeCode) return
            synchronized(events) {
                events.addLast(event.values.copyOf())
                while (events.size > MAX_BUFFER) events.removeFirst()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        fun count(): Int = synchronized(events) { events.size }

        fun snapshot(): List<FloatArray> = synchronized(events) { events.toList() }
    }

    private fun summarize(sensor: Sensor, values: List<FloatArray>): JSONObject {
        val summary = JSONObject()
        if (values.isEmpty()) return summary
        // Events may carry a variable number of axes (fusion sensors can report more
        // than 4 components), so derive the axis count from the shortest event and
        // guard every read.
        val axes = values.minOf { it.size }.coerceAtLeast(1)
        for (axis in 0 until axes) {
            val name = if (axes <= 1) "value" else if (axis < AXIS_NAMES.size) AXIS_NAMES[axis] else "axis$axis"
            val nums = values.mapNotNull { it.getOrNull(axis)?.toDouble() }
            if (nums.isEmpty()) continue
            val mean = nums.average()
            val variance = nums.sumOf { (it - mean) * (it - mean) } / nums.size
            summary.put(name, JSONObject()
                .put("n", nums.size)
                .put("mean", round4(mean))
                .put("variance", round4(variance))
                .put("min", round4(nums.min()))
                .put("max", round4(nums.max())))
        }
        return summary
    }

    private fun sensorJson(sensor: Sensor): JSONObject = JSONObject()
        .put("type", sensorName(sensor.type))
        .put("typeCode", sensor.type)
        .put("name", sensor.name)
        .put("vendor", sensor.vendor)
        .put("version", sensor.version)
        .put("power", round4(sensor.power.toDouble()))
        .put("resolution", round4(sensor.resolution.toDouble()))
        .put("maxRange", round4(sensor.maximumRange.toDouble()))
        .put("minDelayUs", sensor.minDelay)

    private fun sensorName(typeCode: Int): String =
        TYPE_NAMES[typeCode] ?: "type_$typeCode"

    private fun nameToCode(name: String): Int? = NAME_TO_TYPE[name]

    private fun round4(value: Double): Double = (value * 10000).roundToInt() / 10000.0

    private fun error(code: String, message: String): JSONObject = JSONObject()
        .put("ok", false)
        .put("code", code)
        .put("message", message)

    private val AXIS_NAMES = arrayOf("x", "y", "z", "w")

    private val TYPE_NAMES = mapOf(
        Sensor.TYPE_ACCELEROMETER to "accelerometer",
        Sensor.TYPE_MAGNETIC_FIELD to "magnetic",
        Sensor.TYPE_GYROSCOPE to "gyroscope",
        Sensor.TYPE_LIGHT to "light",
        Sensor.TYPE_PRESSURE to "pressure",
        Sensor.TYPE_PROXIMITY to "proximity",
        Sensor.TYPE_GRAVITY to "gravity",
        Sensor.TYPE_LINEAR_ACCELERATION to "linear_acceleration",
        Sensor.TYPE_ROTATION_VECTOR to "rotation",
        Sensor.TYPE_RELATIVE_HUMIDITY to "humidity",
        Sensor.TYPE_AMBIENT_TEMPERATURE to "temperature",
        Sensor.TYPE_STEP_COUNTER to "step_counter",
        Sensor.TYPE_STEP_DETECTOR to "step_detector",
        Sensor.TYPE_GAME_ROTATION_VECTOR to "game_rotation",
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to "geomagnetic_rotation",
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED to "accelerometer_uncalibrated",
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED to "gyroscope_uncalibrated",
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED to "magnetic_uncalibrated",
    )

    private val NAME_TO_TYPE = TYPE_NAMES.entries.associate { (code, name) -> name to code }
}
