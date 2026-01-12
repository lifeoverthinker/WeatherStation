package com.example.weather_station

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.widget.Button
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
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    // --- Widoki ---
    private lateinit var cardAirQuality: MaterialCardView
    private lateinit var tvAirTitle: TextView
    private lateinit var tvAirStatus: TextView
    private lateinit var tvRatioValue: TextView
    private lateinit var imgAirIcon: ImageView
    private lateinit var switchTheme: MaterialSwitch

    // Status Badge
    private lateinit var cardStatusBadge: MaterialCardView
    private lateinit var tvConnectionStatus: TextView

    private lateinit var tvTemp: TextView
    private lateinit var tvTempStats: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvHumidityStats: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvPressureStats: TextView
    private lateinit var tvLastUpdate: TextView

    // Wykres i PDF
    private lateinit var simpleGraph: SimpleGraphView
    private lateinit var btnGeneratePdf: Button

    // --- Zmienne Firebase ---
    private lateinit var database: DatabaseReference
    private lateinit var historyRef: DatabaseReference

    private val CHANNEL_ID = "AIR_QUALITY_ALERT"
    private var lastAlertState = -1

    // Statystyki SESJI (do wyświetlania na ekranie)
    private var sessionMinTemp = 100.0
    private var sessionMaxTemp = -100.0
    private var sessionMinHum = 100.0
    private var sessionMaxHum = 0.0
    private var sessionMinPress = 2000.0
    private var sessionMaxPress = 0.0

    // Bieżące
    private var currentTemp = 0.0
    private var currentHum = 0.0
    private var currentPress = 0.0
    private var currentRatio = 0.0

    private var lastDataTimestamp: Long = 0
    private val offlineHandler = Handler(Looper.getMainLooper())
    private var appStartTime: Long = 0

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

        appStartTime = System.currentTimeMillis()

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) { }

        initializeViews()
        setupThemeSwitcher()
        setupNotifications()
        setupOfflineCheck()

        btnGeneratePdf.setOnClickListener {
            fetchDailyStatsAndGeneratePDF()
        }

        setupFirebase()
    }

    private fun initializeViews() {
        cardAirQuality = findViewById(R.id.cardAirQuality)
        tvAirTitle = findViewById(R.id.tvAirTitle)
        tvAirStatus = findViewById(R.id.tvAirStatus)
        tvRatioValue = findViewById(R.id.tvRatioValue)
        imgAirIcon = findViewById(R.id.imgAirIcon)
        switchTheme = findViewById(R.id.switchTheme)

        cardStatusBadge = findViewById(R.id.cardStatusBadge)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)

        tvTemp = findViewById(R.id.tvTemp)
        tvTempStats = findViewById(R.id.tvTempStats)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvHumidityStats = findViewById(R.id.tvHumidityStats)
        tvPressure = findViewById(R.id.tvPressure)
        tvPressureStats = findViewById(R.id.tvPressureStats)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)

        simpleGraph = findViewById(R.id.simpleGraph)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
    }

    private fun setupThemeSwitcher() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNight = currentNightMode == Configuration.UI_MODE_NIGHT_YES
        switchTheme.isChecked = isNight
        updateSwitchIcon(isNight)

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            updateSwitchIcon(isChecked)
            if (isChecked) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun updateSwitchIcon(isNight: Boolean) {
        if (isNight) switchTheme.setThumbIconResource(R.drawable.ic_mode_dark)
        else switchTheme.setThumbIconResource(R.drawable.ic_mode_light)
    }

    private fun setupOfflineCheck() {
        val checkRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val isDataStale = (lastDataTimestamp > 0 && currentTime - lastDataTimestamp > 3000)
                val isNeverConnected = (lastDataTimestamp == 0L && currentTime - appStartTime > 3000)

                if (isDataStale || isNeverConnected) {
                    tvConnectionStatus.text = "● Offline"
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#BA1A1A"))
                } else if (lastDataTimestamp > 0) {
                    tvConnectionStatus.text = "● Online"
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#43A047"))
                }
                offlineHandler.postDelayed(this, 500)
            }
        }
        offlineHandler.post(checkRunnable)
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
        // Dane "na żywo"
        database = FirebaseDatabase.getInstance().getReference("stacja_pogodowa/live")
        database.keepSynced(true)

        // Historia
        historyRef = FirebaseDatabase.getInstance().getReference("stacja_pogodowa/historia")

        // 1. ODCZYT LIVE
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val temp = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                    val hum = snapshot.child("wilgotnosc").getValue(Double::class.java) ?: 0.0
                    val press = snapshot.child("cisnienie").getValue(Double::class.java) ?: 0.0
                    val ratio = snapshot.child("air_ratio").getValue(Double::class.java) ?: 0.0

                    updateUI(temp, hum, press, ratio)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Błąd bazy: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // 2. ODCZYT HISTORII DLA WYKRESU (Ostatnie ~3.5h na podgląd)
        historyRef.limitToLast(200).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val tempVal = snapshot.child("t").getValue(Double::class.java)
                if (tempVal != null) {
                    simpleGraph.addPoint(tempVal.toFloat())
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateUI(t: Double, h: Double, p: Double, ratio: Double) {
        lastDataTimestamp = System.currentTimeMillis()
        currentTemp = t
        currentHum = h
        currentPress = p
        currentRatio = ratio

        tvConnectionStatus.text = "● Online"
        cardStatusBadge.setCardBackgroundColor(Color.parseColor("#43A047"))

        // Min/Max SESJI (tylko odkąd włączono apkę)
        if (t < sessionMinTemp) sessionMinTemp = t
        if (t > sessionMaxTemp) sessionMaxTemp = t
        if (h < sessionMinHum) sessionMinHum = h
        if (h > sessionMaxHum) sessionMaxHum = h
        if (p < sessionMinPress) sessionMinPress = p
        if (p > sessionMaxPress) sessionMaxPress = p

        tvTemp.text = String.format("%.1f °C", t)
        tvHumidity.text = String.format("%.0f %%", h)
        tvPressure.text = String.format("%.0f hPa", p)
        tvRatioValue.text = String.format("Sensor: %.2f V", ratio)

        tvTempStats.text = String.format("Min: %.1f° • Max: %.1f°", sessionMinTemp, sessionMaxTemp)
        tvHumidityStats.text = String.format("Min: %.0f%% • Max: %.0f%%", sessionMinHum, sessionMaxHum)
        tvPressureStats.text = String.format("Min: %.0f • Max: %.0f", sessionMinPress, sessionMaxPress)

        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        tvLastUpdate.text = "Aktualizacja: $currentTime"

        // --- ZMIANA PROGÓW NAPIĘCIOWYCH (ŁATWIEJSZY ALARM) ---
        // < 1.2V = Świetne
        // < 1.5V = Dobre (Było 1.8)
        // > 1.5V = Złe (Było 1.8)

        val bgColor: Int
        val textColor: Int
        val statusText: String

        if (ratio < 1.2) {
            bgColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer)
            textColor = ContextCompat.getColor(this, R.color.md_theme_onPrimaryContainer)
            statusText = "Powietrze: Świetne"
            lastAlertState = 0
        } else if (ratio < 1.5) { // TUTAJ ZMIANA NA 1.5
            bgColor = ContextCompat.getColor(this, R.color.md_theme_tertiaryContainer)
            textColor = ContextCompat.getColor(this, R.color.md_theme_onTertiaryContainer)
            statusText = "Powietrze: Dobre"
            lastAlertState = 0
        } else {
            bgColor = ContextCompat.getColor(this, R.color.md_theme_errorContainer)
            textColor = ContextCompat.getColor(this, R.color.md_theme_onErrorContainer)
            statusText = "Powietrze: Złe"
            if(lastAlertState != 1) {
                // Powiadomienie przy niższym progu (1.5V)
                sendNotification("Wykryto zanieczyszczenie!", "Poziom sensora przekroczył 1.5V!")
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

    private fun fetchDailyStatsAndGeneratePDF() {
        Toast.makeText(this, "Pobieranie danych z całej doby...", Toast.LENGTH_SHORT).show()

        // 1440 minut = 24h
        historyRef.limitToLast(1440).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(this, "Brak historii w chmurze!", Toast.LENGTH_SHORT).show()
                printPDF(sessionMinTemp, sessionMaxTemp, sessionMinHum, sessionMaxHum, sessionMinPress, sessionMaxPress)
                return@addOnSuccessListener
            }

            var dayMinT = 100.0; var dayMaxT = -100.0
            var dayMinH = 100.0; var dayMaxH = 0.0
            var dayMinP = 2000.0; var dayMaxP = 0.0
            var count = 0

            for (child in snapshot.children) {
                val t = child.child("t").getValue(Double::class.java)
                val h = child.child("h").getValue(Double::class.java)
                val p = child.child("p").getValue(Double::class.java)

                if (t != null && h != null && p != null) {
                    if (t < dayMinT) dayMinT = t
                    if (t > dayMaxT) dayMaxT = t
                    if (h < dayMinH) dayMinH = h
                    if (h > dayMaxH) dayMaxH = h
                    if (p < dayMinP) dayMinP = p
                    if (p > dayMaxP) dayMaxP = p
                    count++
                }
            }

            if (count == 0) {
                dayMinT = sessionMinTemp; dayMaxT = sessionMaxTemp
                dayMinH = sessionMinHum; dayMaxH = sessionMaxHum
                dayMinP = sessionMinPress; dayMaxP = sessionMaxPress
            }

            printPDF(dayMinT, dayMaxT, dayMinH, dayMaxH, dayMinP, dayMaxP)

        }.addOnFailureListener {
            Toast.makeText(this, "Błąd pobierania: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printPDF(
        minT: Double, maxT: Double,
        minH: Double, maxH: Double,
        minP: Double, maxP: Double
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 24f; typeface = Typeface.DEFAULT_BOLD }
        val headerPaint = Paint().apply { color = Color.DKGRAY; textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 14f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var y = 50f
        canvas.drawText("Raport Dobowy - Stacja Pogodowa", 50f, y, titlePaint)
        y += 30f
        paint.textSize = 12f
        canvas.drawText("Data: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 50f, y, paint)
        y += 40f

        val col1 = 50f
        val col2 = 200f
        val col3 = 350f
        val col4 = 480f

        canvas.drawText("PARAMETR", col1, y, headerPaint)
        canvas.drawText("AKTUALNIE", col2, y, headerPaint)
        canvas.drawText("MIN (24h)", col3, y, headerPaint)
        canvas.drawText("MAX (24h)", col4, y, headerPaint)
        y += 10f
        canvas.drawLine(40f, y, 550f, y, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        y += 25f

        canvas.drawText("Temperatura", col1, y, textPaint)
        canvas.drawText(String.format("%.1f °C", currentTemp), col2, y, textPaint)
        canvas.drawText(String.format("%.1f °C", minT), col3, y, textPaint)
        canvas.drawText(String.format("%.1f °C", maxT), col4, y, textPaint)
        y += 10f; canvas.drawLine(40f, y, 550f, y, linePaint); y += 25f

        canvas.drawText("Wilgotność", col1, y, textPaint)
        canvas.drawText(String.format("%.0f %%", currentHum), col2, y, textPaint)
        canvas.drawText(String.format("%.0f %%", minH), col3, y, textPaint)
        canvas.drawText(String.format("%.0f %%", maxH), col4, y, textPaint)
        y += 10f; canvas.drawLine(40f, y, 550f, y, linePaint); y += 25f

        canvas.drawText("Ciśnienie", col1, y, textPaint)
        canvas.drawText(String.format("%.0f hPa", currentPress), col2, y, textPaint)
        canvas.drawText(String.format("%.0f hPa", minP), col3, y, textPaint)
        canvas.drawText(String.format("%.0f hPa", maxP), col4, y, textPaint)
        y += 10f; canvas.drawLine(40f, y, 550f, y, Paint().apply { color = Color.BLACK; strokeWidth = 2f }); y += 40f

        canvas.drawText("Sensor Jakości:", 50f, y, titlePaint.apply { textSize = 18f })

        // --- PROGI NAPIĘCIOWE W PDF (ZAKTUALIZOWANE) ---
        val statusText = if(currentRatio < 1.2) "ŚWIETNE" else if(currentRatio < 1.5) "DOBRE" else "ZŁE"
        val statusColor = if(currentRatio < 1.2) Color.GREEN else if(currentRatio < 1.5) Color.BLUE else Color.RED

        val statusPaint = Paint().apply { color = statusColor; textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(statusText, 250f, y, statusPaint)
        y += 50f

        canvas.drawText("Wykres temperatury (Historia z chmury):", 50f, y, headerPaint)
        y += 20f
        canvas.save()
        canvas.translate(50f, y)
        simpleGraph.drawToCanvas(canvas, 500f, 250f)
        canvas.restore()

        pdfDocument.finishPage(page)

        val fileName = "Raport_Stacji_${System.currentTimeMillis()}.pdf"
        try {
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { contentResolver.openOutputStream(it) }
                Toast.makeText(this, "Zapisano w Pobranych!", Toast.LENGTH_LONG).show()
            } else {
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                outputStream = FileOutputStream(file)
                Toast.makeText(this, "Zapisano: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
            outputStream?.use { pdfDocument.writeTo(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Błąd zapisu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        pdfDocument.close()
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

// --- KLASA WYKRESU ---
class SimpleGraphView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val dataPoints = ArrayList<Float>()
    private val paintLine = Paint().apply {
        color = Color.parseColor("#005DAC")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val paintGrid = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    private val paintText = Paint().apply { color = Color.DKGRAY; textSize = 30f; isAntiAlias = true }

    fun addPoint(value: Float) {
        dataPoints.add(value)
        if (dataPoints.size > 200) dataPoints.removeAt(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawToCanvas(canvas, width.toFloat(), height.toFloat())
    }

    fun drawToCanvas(canvas: Canvas, w: Float, h: Float) {
        val paddingLeft = 80f
        val paddingBottom = 40f
        canvas.drawRect(paddingLeft, 0f, w, h - paddingBottom, Paint().apply { style = Paint.Style.STROKE; color = Color.GRAY })

        if (dataPoints.isEmpty()) {
            canvas.drawText("Pobieranie historii...", w/2, h/2, paintText)
            return
        }

        val maxVal = (dataPoints.maxOrNull() ?: 40f) + 1f
        val minVal = (dataPoints.minOrNull() ?: 0f) - 1f
        val range = if (maxVal - minVal == 0f) 10f else maxVal - minVal

        for (i in 0..4) {
            val y = (h - paddingBottom) - ((h - paddingBottom) * i / 4)
            canvas.drawLine(paddingLeft, y, w, y, paintGrid)
            val labelVal = minVal + (range * i / 4)
            canvas.drawText(String.format("%.1f", labelVal), 10f, y + 10f, paintText)
        }

        val stepX = (w - paddingLeft) / (if(dataPoints.size > 1) dataPoints.size - 1 else 1)
        val path = Path()

        for (i in dataPoints.indices) {
            val x = paddingLeft + (i * stepX)
            val availableHeight = h - paddingBottom
            val y = availableHeight - ((dataPoints[i] - minVal) / range * availableHeight)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            if (i == dataPoints.size - 1) {
                canvas.drawCircle(x, y, 8f, Paint().apply { color = Color.RED })
                canvas.drawText("${dataPoints[i]}°C", x - 60, y - 20, paintText)
            }
        }
        canvas.drawPath(path, paintLine)
    }
}