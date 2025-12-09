package com.example.weather_station

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // --- Widoki ---
    private lateinit var cardAirQuality: MaterialCardView
    private lateinit var tvAirTitle: TextView
    private lateinit var tvAirStatus: TextView
    private lateinit var tvRatioValue: TextView
    private lateinit var imgAirIcon: ImageView
    private lateinit var switchTheme: MaterialSwitch

    private lateinit var tvTemp: TextView
    private lateinit var tvTempStats: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvHumidityStats: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvPressureStats: TextView
    private lateinit var tvLastUpdate: TextView

    // --- Zmienne ---
    private lateinit var database: DatabaseReference
    private val CHANNEL_ID = "AIR_QUALITY_ALERT"
    private var lastAlertState = -1

    // Zmienne Min/Max
    private var minTemp = Double.MAX_VALUE
    private var maxTemp = Double.MIN_VALUE
    private var minHum = Double.MAX_VALUE
    private var maxHum = Double.MIN_VALUE
    private var minPress = Double.MAX_VALUE
    private var maxPress = Double.MIN_VALUE

    // Śledzenie zmiany daty
    private var lastDayRecorded: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Brak zgody na powiadomienia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupThemeSwitcher()
        setupNotifications()

        lastDayRecorded = getCurrentDateString()
        setupFirebase()
    }

    private fun initializeViews() {
        cardAirQuality = findViewById(R.id.cardAirQuality)
        tvAirTitle = findViewById(R.id.tvAirTitle)
        tvAirStatus = findViewById(R.id.tvAirStatus)
        tvRatioValue = findViewById(R.id.tvRatioValue)
        imgAirIcon = findViewById(R.id.imgAirIcon)
        switchTheme = findViewById(R.id.switchTheme)

        tvTemp = findViewById(R.id.tvTemp)
        tvTempStats = findViewById(R.id.tvTempStats)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvHumidityStats = findViewById(R.id.tvHumidityStats)
        tvPressure = findViewById(R.id.tvPressure)
        tvPressureStats = findViewById(R.id.tvPressureStats)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
    }

    private fun setupThemeSwitcher() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNight = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        switchTheme.isChecked = isNight
        updateSwitchIcon(isNight)

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            updateSwitchIcon(isChecked)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun updateSwitchIcon(isNight: Boolean) {
        if (isNight) {
            switchTheme.setThumbIconResource(R.drawable.ic_mode_dark)
        } else {
            switchTheme.setThumbIconResource(R.drawable.ic_mode_light)
        }
    }

    private fun setupNotifications() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupFirebase() {
        database = FirebaseDatabase.getInstance().getReference("stacja_pogodowa")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val temp = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                    val hum = snapshot.child("wilgotnosc").getValue(Double::class.java) ?: 0.0
                    val press = snapshot.child("cisnienie").getValue(Double::class.java) ?: 0.0
                    val ratio = snapshot.child("air_ratio").getValue(Double::class.java) ?: 1.0

                    updateUI(temp, hum, press, ratio)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Błąd bazy: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(t: Double, h: Double, p: Double, ratio: Double) {
        val currentDay = getCurrentDateString()
        if (currentDay != lastDayRecorded) {
            // Reset wartości na początek nowego dnia
            minTemp = t
            maxTemp = t
            minHum = h
            maxHum = h
            minPress = p
            maxPress = p
            lastDayRecorded = currentDay
        } else {
            if (t < minTemp) minTemp = t
            if (t > maxTemp) maxTemp = t
            if (h < minHum) minHum = h
            if (h > maxHum) maxHum = h
            if (p < minPress) minPress = p
            if (p > maxPress) maxPress = p
        }

        // Główne wartości
        tvTemp.text = String.format("%.1f °C", t)
        tvHumidity.text = String.format("%.0f %%", h)
        tvPressure.text = String.format("%.0f hPa", p)
        tvRatioValue.text = String.format("Indeks jakości: %.2f", ratio)

        // Statystyki Min/Max - teraz czytelne etykiety
        tvTempStats.text = String.format("Min: %.1f° • Max: %.1f°", minTemp, maxTemp)
        tvHumidityStats.text = String.format("Min: %.0f%% • Max: %.0f%%", minHum, maxHum)
        tvPressureStats.text = String.format("Min: %.0f • Max: %.0f", minPress, maxPress)

        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        tvLastUpdate.text = "Ostatnia aktualizacja: $currentTime"

        val bgColor: Int
        val textColor: Int
        val statusText: String

        if (ratio > 0.8) {
            bgColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer)
            textColor = ContextCompat.getColor(this, R.color.md_theme_onPrimaryContainer)
            statusText = "Powietrze: Świetne"
            lastAlertState = 0
        } else if (ratio > 0.5) {
            bgColor = ContextCompat.getColor(this, R.color.md_theme_tertiaryContainer)
            textColor = ContextCompat.getColor(this, R.color.md_theme_onTertiaryContainer)
            statusText = "Powietrze: Dobre"
            lastAlertState = 0
        } else {
            bgColor = ContextCompat.getColor(this, R.color.md_theme_errorContainer)
            textColor = ContextCompat.getColor(this, R.color.md_theme_onErrorContainer)
            statusText = "Uwaga: Smog!"
            if(lastAlertState != 1) {
                sendNotification("Wykryto zanieczyszczenie!", "Jakość powietrza spadła.")
                lastAlertState = 1
            }
        }

        cardAirQuality.setCardBackgroundColor(bgColor)
        tvAirStatus.text = statusText
        tvAirTitle.setTextColor(textColor)
        tvAirStatus.setTextColor(textColor)
        tvRatioValue.setTextColor(textColor)
        imgAirIcon.setColorFilter(textColor)
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun sendNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_air)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Alerty Smogowe", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Powiadomienia o niskiej jakości powietrza"
                enableLights(true)
                lightColor = Color.RED
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}