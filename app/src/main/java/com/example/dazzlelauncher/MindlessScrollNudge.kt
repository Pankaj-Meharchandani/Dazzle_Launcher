package com.example.dazzlelauncher

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

object MindlessScrollNudge {
    private var activeDialog: Dialog? = null

    fun show(context: Context, appName: String): Boolean {
        if (!Settings.canDrawOverlays(context) || activeDialog?.isShowing == true) return false

        vibrate(context)
        val dialog = Dialog(context)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 24), dp(context, 22), dp(context, 24), dp(context, 18))
            background = GradientDrawable().apply {
                setColor(Color.rgb(25, 27, 35))
                cornerRadius = dp(context, 24).toFloat()
                setStroke(dp(context, 1), Color.rgb(70, 74, 88))
            }
        }

        TextView(context).apply {
            text = "Take a mindful pause"
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            content.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        TextView(context).apply {
            text = "You have been scrolling $appName for a while. Want to take a short break?"
            setTextColor(Color.rgb(215, 216, 222))
            textSize = 16f
            setPadding(0, dp(context, 8), 0, dp(context, 20))
            content.addView(this, LinearLayout.LayoutParams(-1, -2))
        }

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val snooze = Button(context).apply {
            text = "+5 mins"
            isAllCaps = false
        }
        val close = Button(context).apply {
            text = "Close app"
            isAllCaps = false
        }
        val buttonParams = LinearLayout.LayoutParams(0, dp(context, 52), 1f).apply {
            marginEnd = dp(context, 8)
        }
        buttons.addView(snooze, buttonParams)
        buttons.addView(close, LinearLayout.LayoutParams(0, dp(context, 52), 1f))
        content.addView(buttons)

        fun dismiss() {
            dialog.dismiss()
            activeDialog = null
        }
        snooze.setOnClickListener {
            context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("mindless_scroll_snoozed_until", System.currentTimeMillis() + 5 * 60_000L)
                .apply()
            dismiss()
        }
        close.setOnClickListener {
            context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                .edit().remove("mindless_scroll_snoozed_until").apply()
            dismiss()
            context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        dialog.setContentView(content)
        dialog.window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.65f }
            setGravity(Gravity.CENTER)
        }
        dialog.setOnDismissListener { activeDialog = null }
        activeDialog = dialog
        dialog.show()
        dialog.window?.setLayout(dp(context, 340), WindowManager.LayoutParams.WRAP_CONTENT)
        return true
    }

    private fun vibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 140, 80, 140), -1))
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(longArrayOf(0, 140, 80, 140), -1)
        }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()
}
