package com.example.dazzlelauncher

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.*
import android.os.*
import android.telephony.TelephonyManager
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat

class PostureAlertService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private lateinit var windowManager: WindowManager
    private lateinit var powerManager: PowerManager
    private lateinit var telephonyManager: TelephonyManager

    private var bannerView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isBadAngle = false
    private var badAngleStartTime: Long = 0
    private var lastAlertTime: Long = 0
    
    // Settings
    private var enabled = true
    private var thresholdAngle = 45f
    private var durationThreshold = 30000L
    private val alertCooldown = 120000L
    private var quietHoursStart = "23:00"
    private var quietHoursEnd = "07:00"

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> registerSensor()
                Intent.ACTION_SCREEN_OFF -> unregisterSensor()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        loadSettings()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1, 
                createNotification(), 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, createNotification())
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        if (powerManager.isInteractive) {
            registerSensor()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        enabled = prefs.getBoolean("posture_alert_enabled", false)
        thresholdAngle = prefs.getInt("posture_sensitivity", 45).toFloat()
        durationThreshold = prefs.getInt("posture_duration", 30).toLong() * 1000
        quietHoursStart = prefs.getString("posture_quiet_start", "23:00") ?: "23:00"
        quietHoursEnd = prefs.getString("posture_quiet_end", "07:00") ?: "07:00"
    }

    private fun registerSensor() {
        if (enabled && rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterSensor() {
        sensorManager.unregisterListener(this)
        resetTimer()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val pitchRadians = orientation[1]
            val pitchDegrees = Math.toDegrees((-pitchRadians).toDouble()).toFloat()

            checkPosture(pitchDegrees)
        }
    }

    private fun checkPosture(pitchDegrees: Float) {
        if (!isInQuietHours() && !isDuringCall() && !isFullScreen()) {
            if (pitchDegrees < thresholdAngle) {
                if (!isBadAngle) {
                    isBadAngle = true
                    badAngleStartTime = System.currentTimeMillis()
                } else {
                    val duration = System.currentTimeMillis() - badAngleStartTime
                    if (duration >= durationThreshold) {
                        if (System.currentTimeMillis() - lastAlertTime >= alertCooldown) {
                            showAlert()
                            lastAlertTime = System.currentTimeMillis()
                        }
                    }
                }
            } else {
                resetTimer()
            }
        } else {
            resetTimer()
        }
    }

    private fun resetTimer() {
        isBadAngle = false
        badAngleStartTime = 0
    }

    private fun isInQuietHours(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentTimeInMins = currentHour * 60 + currentMinute

        val startParts = quietHoursStart.split(":")
        val startMins = startParts[0].toInt() * 60 + startParts[1].toInt()
        
        val endParts = quietHoursEnd.split(":")
        val endMins = endParts[0].toInt() * 60 + endParts[1].toInt()

        return if (startMins < endMins) {
            currentTimeInMins in startMins..endMins
        } else {
            currentTimeInMins >= startMins || currentTimeInMins <= endMins
        }
    }

    private fun isDuringCall(): Boolean {
        return try {
            @Suppress("DEPRECATION")
            telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
        } catch (e: Exception) {
            false
        }
    }

    private fun isFullScreen(): Boolean {
        return false 
    }

    private fun showAlert() {
        if (bannerView != null) return

        handler.post {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
            }

            val view = TextView(this).apply {
                text = getString(R.string.posture_banner_text)
                setBackgroundColor(0xCC000000.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(40, 80, 40, 80)
                gravity = Gravity.CENTER
            }
            
            bannerView = view
            try {
                windowManager.addView(view, params)
                vibrate()
                handler.postDelayed({
                    removeAlert()
                }, 4000)
            } catch (e: Exception) {
                bannerView = null
            }
        }
    }

    private fun removeAlert() {
        bannerView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore
            }
            bannerView = null
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "posture_service",
                getString(R.string.posture_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "posture_service")
            .setContentTitle(getString(R.string.posture_service_notification_title))
            .setContentText(getString(R.string.posture_service_notification_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterSensor()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        removeAlert()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_SETTINGS") {
            loadSettings()
            unregisterSensor()
            if (powerManager.isInteractive) {
                registerSensor()
            }
        }
        return START_STICKY
    }
}
