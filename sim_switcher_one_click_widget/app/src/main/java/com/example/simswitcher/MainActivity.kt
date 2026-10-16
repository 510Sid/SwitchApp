package com.example.simswitcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var oneClickButton: Button
    private lateinit var sim1NickInput: EditText
    private lateinit var sim2NickInput: EditText
    private lateinit var historyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 101)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(56, 56, 56, 56)
        }

        val titleText = TextView(this).apply {
            text = "1-Click SIM Data Switcher"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }

        statusText = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        }

        // 1-Click Manual Switch Button (always targets the currently INACTIVE SIM)
        oneClickButton = Button(this).apply {
            textSize = 16f
            setOnClickListener {
                SimSwitchHelper.switchToInactiveSim(this@MainActivity, source = "App")
                refreshUI()
            }
        }

        // Custom SIM Nickname Inputs (saved to SharedPreferences & synced with Widget)
        sim1NickInput = EditText(this).apply {
            hint = "SIM 1 Label (e.g., Personal 5G)"
            setText(SimSwitchHelper.getCustomNickname(this@MainActivity, 1, ""))
        }

        sim2NickInput = EditText(this).apply {
            hint = "SIM 2 Label (e.g., Work Data)"
            setText(SimSwitchHelper.getCustomNickname(this@MainActivity, 2, ""))
        }

        val saveLabelsBtn = Button(this).apply {
            text = "Save Custom SIM Labels"
            textSize = 13f
            setOnClickListener {
                SimSwitchHelper.setSimNicknames(
                    this@MainActivity,
                    sim1NickInput.text.toString(),
                    sim2NickInput.text.toString()
                )
                refreshUI()
                Toast.makeText(this@MainActivity, "Saved SIM labels & updated Widget!", Toast.LENGTH_SHORT).show()
            }
        }

        historyText = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 12)
        }

        val clearHistoryBtn = Button(this).apply {
            text = "Clear Switch History"
            textSize = 12f
            setOnClickListener {
                SimSwitchHelper.clearSwitchHistory(this@MainActivity)
                refreshUI()
            }
        }

        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(oneClickButton)
        layout.addView(sim1NickInput)
        layout.addView(sim2NickInput)
        layout.addView(saveLabelsBtn)
        layout.addView(historyText)
        layout.addView(clearHistoryBtn)
        setContentView(layout)

        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun refreshUI() {
        val status = SimSwitchHelper.getSimStatus(this)
        statusText.text = "Active Data: SIM ${status.activeSlot} (${status.activeCarrier}) • ${status.signalBars}/4 bars"
        oneClickButton.text = "Switch Data to SIM ${status.inactiveSlot} (${status.inactiveCarrier})"

        val history = SimSwitchHelper.getSwitchHistory(this)
        historyText.text = if (history.isEmpty()) {
            "Recent Switches: None yet"
        } else {
            "Recent Manual Switches:\n" + history.joinToString("\n")
        }
    }
}
