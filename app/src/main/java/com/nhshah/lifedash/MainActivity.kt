package com.nhshah.lifedash

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nhshah.lifedash.ui.theme.LifeDashTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.Composable
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    data class AppUsageSession(
        val appName: String,
        val packageName: String,
        val startTime: Long,
        val endTime: Long
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeDashTheme {
                val context = LocalContext.current
                var loading by remember { mutableStateOf(false) }
                var showDetails by remember { mutableStateOf(false) }
                var hasPermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }

                var totalUsage by remember { mutableStateOf(0L) }
                var appUsageMap by remember { mutableStateOf(mapOf<String, Long>()) }
                var lastTimeUsedMap by remember { mutableStateOf(mapOf<String, Long>()) }
                var appLaunchCountMap by remember { mutableStateOf(mapOf<String, Int>()) }
                var appSwitchPattern by remember { mutableStateOf(listOf<String>()) }

                var unlockCount by remember { mutableStateOf(0) }
                var unlockTimes by remember { mutableStateOf(listOf<Long>()) }
                var screenOnOffCount by remember { mutableStateOf(0 to 0) }
                var usageSessions by remember { mutableStateOf(listOf<Pair<Long, Long>>()) }

                var notificationCounts by remember { mutableStateOf(mapOf<String, Int>()) }
                var hourlyNotificationCounts by remember { mutableStateOf(mapOf<Int, Int>()) }

                var appSessions by remember { mutableStateOf(listOf<AppUsageSession>()) } // ✅ added

                LaunchedEffect(hasPermission) {
                    if (hasPermission) {
                        val now = System.currentTimeMillis()
                        val startOfDay = getStartOfDayMillis()

                        totalUsage = getTodayUsageTime(context, startOfDay, now)
                        appUsageMap = getAppUsageStats(context, startOfDay, now)
                        lastTimeUsedMap = getLastTimeUsed(context, startOfDay, now)
                        appLaunchCountMap = getAppLaunchCount(context, startOfDay, now)
                        appSwitchPattern = getAppSwitchPattern(context, startOfDay, now)

                        val unlockData = getUnlockData(context, startOfDay, now)
                        unlockCount = unlockData.first
                        unlockTimes = unlockData.second

                        screenOnOffCount = getScreenOnOffCount(context, startOfDay, now)
                        usageSessions = getUsageSessions(context, startOfDay, now)

                        notificationCounts = getNotificationCounts(context, startOfDay, now)
                        hourlyNotificationCounts = getHourlyNotificationCounts(context, startOfDay, now)

                        appSessions = getAppUsageSessions(context) // ✅ added
                    }
                }

                Scaffold(
                    topBar = {
                        val interFontFamily = FontFamily(
                            Font(R.font.cursive, FontWeight.SemiBold)
                        )
                        Box {
                            // Background with gradient and dark overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(81.dp)
                                    .drawWithCache {
                                        val colorGradient = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0x00000000),
                                                Color(0x00000000)
                                            )
                                        )
                                        val alphaGradient = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 1f),
                                                Color.Black.copy(alpha = 0f)
                                            )
                                        )
                                        onDrawBehind {
                                            drawRect(brush = colorGradient)
                                            drawRect(brush = alphaGradient, blendMode = BlendMode.DstIn)
                                        }
                                    }
                            )

                            // Red shadow (fade downwards)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp) // Shadow thickness
                                    .align(Alignment.BottomStart)
                                    .drawWithCache {
                                        val colorGradient = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF00FFFF), // Cyan (left)
                                                Color(0xFFFF00FF)  // Pink (right)
                                            )
                                        )
                                        val fadeMask = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.3f), // Top (visible)
                                                Color.Transparent                // Bottom (fade out)
                                            )
                                        )
                                        onDrawBehind {
                                            // Color layer (horizontal gradient)
                                            drawRect(brush = colorGradient)

                                            // Opacity mask (vertical fade)
                                            drawRect(brush = fadeMask, blendMode = BlendMode.DstIn)
                                        }
                                    }
                            )


                            // Your TopAppBar with title
                            TopAppBar(
                                title = {
                                    Column(
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(
                                                    style = SpanStyle(
                                                        color = Color(0xFFFFFFFF),
                                                        fontFamily = interFontFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 23.sp
                                                    )
                                                ) { append("Life") }

                                                withStyle(
                                                    style = SpanStyle(
                                                        color = Color(0xFFFFFFFF),
                                                        fontFamily = interFontFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 23.sp
                                                    )
                                                ) { append("Dash") }
                                            }
                                        )

                                        Text(
                                            text = "Take control before time controls you.",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            fontStyle = FontStyle.Italic,
                                            lineHeight = 15.sp,
                                            modifier = Modifier
                                                .padding(start = 2.dp)
                                                .offset(y = (-4).dp)
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { /* TODO: Navigate to chat/help screen */ }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Chat",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .graphicsLayer {
                                                rotationZ = -30f // 🎯 Rotates the icon by 30 degrees
                                            }
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                        }

                    }

                )
//                { padding ->
//                    val scrollState = rememberScrollState()
//                    Column(
//                        modifier = Modifier
//                            .background(Color.Black)
//                            .padding(padding)
//                            .padding(horizontal = 16.dp)
//                            .fillMaxSize()
//                            .verticalScroll(scrollState)
//                    ) {
//                        if (!hasPermission) {
//                            Text("This app needs permission to access usage data.")
//                            Spacer(modifier = Modifier.height(12.dp))
//                            Button(onClick = {
//                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
//                            }) {
//                                Text("Grant Permission")
//                            }
//                        } else {
//                            val totalHr = totalUsage / (1000 * 60 * 60)
//                            val totalMin = (totalUsage / (1000 * 60)) % 60
//                            val totalSec = (totalUsage / 1000) % 60
//
//                            Spacer(modifier = Modifier.height(16.dp))
//
//
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .background(Color.Black)
//                            ) {
//                                // Centered content (the Card)
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                        .padding(bottom = 80.dp), // Reserve space for button
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Column(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        horizontalAlignment = Alignment.CenterHorizontally
//                                    ) {
//                                        GlowingTopMessageCard(totalHr, totalMin, totalSec)
//                                    }
//                                }
//
//                                // Fixed Bottom Button
//                                GlowingTopMessageButton(
//                                    onClick = { /* navigate to detail screen */ },
//                                    color = 0xFF00FFFF,
//                                    modifier = Modifier
//                                        .align(Alignment.BottomCenter)
//                                        .padding(16.dp)
//                                )
//                            }
//
//
//
//
//
//
////                            Column(
////                                modifier = Modifier.
////                                fillMaxWidth(),
////                                horizontalAlignment = Alignment.CenterHorizontally
////                            ) {
////                                GlowingTopMessageCard(totalHr, totalMin, totalSec)
////                            }
//
//
//
//
//
//
////                            Spacer(modifier = Modifier.height(14.dp))
////                            Row(
////                                modifier = Modifier
////                                    .fillMaxWidth()
////                                    .padding(horizontal = 2.dp),
////                                horizontalArrangement = Arrangement.spacedBy(8.dp),
////                                verticalAlignment = Alignment.CenterVertically
////                            ) {
////                                GlowingTopMessageCardForAll(
////                                    text = "Total Unlocks",
////                                    ans = unlockCount,
////                                    modifier = Modifier.weight(1f),
////                                    color = 0xFF00FFFF
////                                )
////                                GlowingTopMessageCardForAll(
////                                    text = "Screen On Count",
////                                    ans = screenOnOffCount.first,
////                                    modifier = Modifier.weight(1f),
////                                    color = 0xFF00FFFF
////                                )
////                            }
////                            Spacer(modifier = Modifier.height(18.dp))
////                            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
////                            val boxWidth = screenWidth * 0.95f
////                            Column(
////                                modifier = Modifier.fillMaxWidth(),
////                                horizontalAlignment = Alignment.CenterHorizontally
////                            ) {
////                                GlowingTopMessageCardForAll(
////                                    text = "Total Apps Used",
////                                    ans = appUsageMap.count(),
////                                    modifier = Modifier.width(screenWidth * 0.95f),
////                                    contentAlignment = Alignment.Center,
////                                    horizontalAlignment = Alignment.CenterHorizontally,
////                                    color = 0xFF00FFFF
////                                )
////                            }
////                            Spacer(modifier = Modifier.height(24.dp))
////                            var shortSessions = 0
////                            var longSessions = 0
////                            for(items in usageSessions) {
////                                val end = items.second
////                                val start = items.first
////                                val duration = end - start
////                                val hr = duration / (1000 * 60 * 60)
////                                val min = (duration / (1000 * 60)) % 60
////                                if (hr >= 1 || min >= 10) {
////                                    longSessions += 1
////                                }
////                                if (hr.toInt() == 0 && min <= 2) {
////                                    shortSessions += 1
////                                }
////                            }
////                            val totalNotifications = notificationCounts.values.sum()
////
////                            Column(
////                                modifier = Modifier.fillMaxWidth(),
////                                horizontalAlignment = Alignment.CenterHorizontally
////                            ) {
////                                GlowingTopMessageCardForAll(
////                                    text = "Total Notifications",
////                                    ans = totalNotifications,
////                                    modifier = Modifier.width(screenWidth * 0.95f),
////                                    contentAlignment = Alignment.Center,
////                                    horizontalAlignment = Alignment.CenterHorizontally,
////                                    color = 0xFF00FFFF
////                                )
////                            }
////                            Spacer(modifier = Modifier.height(14.dp))
//
//
//
//
//
////                            GlowingTopMessageButton(
////                                onClick = { /* navigate to detail screen */ },
////                                color = 0xFF00FFFF // Cyan glow
////                            )
////
////
//
//
//
//
//
//
//////                            Row(
//////                                modifier = Modifier
//////                                    .fillMaxWidth()
//////                                    .padding(horizontal = 2.dp),
//////                                horizontalArrangement = Arrangement.spacedBy(8.dp),
//////                                verticalAlignment = Alignment.CenterVertically
//////                            ) {
//////                                GlowingTopMessageCardForAll(
//////                                    text = "Short Sessions",
//////                                    ans = shortSessions,
//////                                    modifier = Modifier.weight(1f),
//////                                    color = 0xFFFF00FF
//////                                )
//////                                GlowingTopMessageCardForAll(
//////                                    text = "Long Sessions",
//////                                    ans = longSessions,
//////                                    modifier = Modifier.weight(1f),
//////                                    color = 0xFFFF00FF
//////                                )
//////                            }
////
////
////                            Spacer(modifier = Modifier.height(28.dp))
////                            Spacer(modifier = Modifier.height(28.dp))
////                            Spacer(modifier = Modifier.height(28.dp))
////                            Spacer(modifier = Modifier.height(28.dp))
////
////
////                            Text("⏳ Usage Sessions (Unlock → Lock):", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(150.dp)) {
////                                items(usageSessions) { (start, end) ->
////                                    val duration = end - start
////                                    val hr = duration / (1000 * 60 * 60)
////                                    val min = (duration / (1000 * 60)) % 60
////                                    if(hr.toInt()>=1 || min.toInt()>=10){
////                                        longSessions+=1
////                                    }
////                                    if(hr.toInt()==0 && min.toInt()<=2){
////                                        shortSessions+=1
////                                    }
////                                    Text("${formatTime(start)} → ${formatTime(end)} = $hr hr $min min")
////                                }
////                            }
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            Text("📲 App Usage (Today):", fontWeight = FontWeight.Bold)
////
////                            LazyColumn(modifier = Modifier.height(200.dp)) {
////                                items(appUsageMap.toList().sortedByDescending { it.second }) { (pkg, millis) ->
////                                    val hr = millis / (1000 * 60 * 60)
////                                    val min = (millis / (1000 * 60)) % 60
////                                    val sec = (millis / 1000) % 60
////                                    Row(modifier = Modifier.fillMaxWidth()) {
////                                        Text(getAppName(pkg, context), modifier = Modifier.weight(1f))
////                                        Text(String.format("%02d:%02d:%02d", hr, min, sec), modifier = Modifier.weight(0.3f))
////                                    }
//////                                    Text(appUsageMap.count().toString())
////                                }
////                            }
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            Text("🕑 App Last Used Time:", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(150.dp)) {
////                                items(lastTimeUsedMap.toList().sortedByDescending { it.second }) { (pkg, time) ->
////                                    Text("${getAppName(pkg, context)}: ${formatTime(time)}")
////                                }
////                            }
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            Text("🚀 App Launch Count (Approx):", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(150.dp)) {
////                                items(appLaunchCountMap.toList().sortedByDescending { it.second }) { (pkg, count) ->
////                                    Text("${getAppName(pkg, context)}: $count")
////                                }
////                            }
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            Text("🔁 App Switch Pattern (Today):", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(150.dp)) {
////                                items(appSwitchPattern) { pkg ->
////                                    Text(getAppName(pkg, context))
////                                }
////                            }
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            Text("🔔 Notification Counts (Seen):", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(150.dp)) {
////                                items(notificationCounts.toList().sortedByDescending { it.second }) { (pkg, count) ->
////                                    Text("${getAppName(pkg, context)}: $count")
////                                }
////                            }
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            // ✅ App session history (actual open/close sessions)
////                            Text("🧭 App Session History:", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(250.dp)) {
////                                items(appSessions) { session ->
////                                    val start = formatTime(session.startTime)
////                                    val end = formatTime(session.endTime)
////                                    val durationMin = (session.endTime - session.startTime) / (1000 * 60)
////                                    Text("• ${session.appName}: $start → $end (${durationMin} min)")
////                                }
////                            }
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            Text("⏰ Hourly Notification Counts:", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(150.dp)) {
////                                items(hourlyNotificationCounts.toList().sortedBy { it.first }) { (hour, count) ->
////                                    Text(String.format("%02d:00 - %02d:00 : %d", hour, (hour + 1) % 24, count))
////                                }
////                            }
////
////                            Text("📊 Top 10 Apps Used Today:", fontWeight = FontWeight.Bold)
////                            AppUsagePieChart(appUsageMap, context)
////                            Spacer(modifier = Modifier.height(8.dp))
////
////                            Text("⏰ Hourly Notification Counts:", fontWeight = FontWeight.Bold)
////                            LazyColumn(modifier = Modifier.height(150.dp)) {
////                                items(hourlyNotificationCounts.toList().sortedBy { it.first }) { (hour, count) ->
////                                    Text(String.format("%02d:00 - %02d:00 : %d", hour, (hour + 1) % 24, count))
////                                }
////                            }
//
//                        }
//                    }
//                }
                { paddingValues ->
                    val context = LocalContext.current
                    val scrollState = rememberScrollState()

                    if (!hasPermission) {
                        // Permission UI
                        Column(
                            modifier = Modifier
                                .background(Color.Black)
                                .padding(paddingValues)
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "This app needs permission to access usage data.",
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    } else {
                        val totalHr = totalUsage / (1000 * 60 * 60)
                        val totalMin = (totalUsage / (1000 * 60)) % 60
                        val totalSec = (totalUsage / 1000) % 60
                        var shortSessions = 0
                        var longSessions = 0
                        for(items in usageSessions) {
                            val end = items.second
                            val start = items.first
                            val duration = end - start
                            val hr = duration / (1000 * 60 * 60)
                            val min = (duration / (1000 * 60)) % 60
                            if (hr >= 1 || min >= 10) {
                                longSessions += 1
                            }
                            if (hr.toInt() == 0 && min <= 2) {
                                shortSessions += 1
                            }
                        }
                        val totalNotifications = notificationCounts.values.sum()
                        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

                        when {
                            loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TriangularMergeLoader()
                                }
                            }

                            showDetails -> {
                                DetailsScreen(
                                    usageSessions = usageSessions,
                                    appUsageMap = appUsageMap,
                                    appSessions = appSessions,
                                    context = context,
                                    totalHr = totalHr,
                                    totalMin = totalMin,
                                    totalSec = totalSec,
                                    unlocks = unlockCount,
                                    totalNotifications = totalNotifications,
                                    totalAppsUsed = appUsageMap.size,
                                    sessionCount = usageSessions.size,
                                    onBackClick = {
                                        showDetails = false
                                    },
                                    padding = paddingValues
                                )
                            }

                            else -> {

                                // 🧱 Main Layout Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                        .padding(paddingValues)
                                )
                                {
                                    // Full screen Box to contain center + bottom aligned elements
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        // 🎯 Vertically Centered Card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(bottom = 100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                GlowingTopMessageCard(totalHr, totalMin, totalSec)
                                                Spacer(modifier = Modifier.height(18.dp))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    GlowingTopMessageCardForAll(
                                                        text = "Total Unlocks",
                                                        ans = unlockCount,
                                                        modifier = Modifier.weight(1f),
                                                        color = 0xFF00FFFF
                                                    )
                                                    GlowingTopMessageCardForAll(
                                                        text = "Screen On Count",
                                                        ans = screenOnOffCount.first,
                                                        modifier = Modifier.weight(1f),
                                                        color = 0xFF00FFFF
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(18.dp))

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    GlowingTopMessageCardForAll(
                                                        text = "Total Apps Used",
                                                        ans = appUsageMap.count(),
                                                        modifier = Modifier.width(screenWidth * 0.95f),
                                                        contentAlignment = Alignment.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        color = 0xFF00FFFF
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(18.dp))
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    GlowingTopMessageCardForAll(
                                                        text = "Total Notifications",
                                                        ans = totalNotifications,
                                                        modifier = Modifier.width(screenWidth * 0.95f),
                                                        contentAlignment = Alignment.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        color = 0xFF00FFFF
                                                    )
                                                }
                                            }
                                        }


                                        // 🔘 Bottom Button pinned
                                        GlowingTopMessageButton(
                                            onClick = {
                                                loading = true
                                                // Delay for 2.5 seconds before showing details
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    delay(2500)
                                                    loading = false
                                                    showDetails = true
                                                }
                                            },
                                            color = 0xFF00FFFF,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(vertical = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @Composable
    fun GlowingTopMessageCard(totalHr: Long, totalMin: Long, totalSec: Long) {
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val boxWidth = screenWidth * 0.95f
        val cornerRadius = 20.dp

        val glowColor = Color(0xFF00FFFF) // Cyan-like strong glow

        // 🎯 Define the custom font family
        val libertinusFont = FontFamily(
            Font(R.font.libertinusmath, FontWeight.Normal)
        )

        Box(
            modifier = Modifier
                .width(boxWidth)
                .height(135.dp)
                .shadow(
                    elevation = 15.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .background(Color.Black, RoundedCornerShape(cornerRadius))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalHr hr $totalMin min $totalSec sec",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 33.sp,
                    fontFamily = libertinusFont
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total Usage Today",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 17.sp,
                    fontFamily = libertinusFont
                )
            }
        }
    }



    @Composable
    fun GlowingTopMessageCardForAll(
        text: String,
        ans: Int,
        modifier: Modifier = Modifier,
        contentAlignment: Alignment = Alignment.CenterStart,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        color: Long
    ) {
        val cornerRadius = 20.dp
        val glowColor = Color(color)

        // 🎯 Load the custom font
        val libertinusFont = FontFamily(
            Font(R.font.libertinusmath, FontWeight.Normal)
        )

        Box(
            modifier = modifier
                .height(135.dp)
                .shadow(
                    elevation = 15.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .background(Color.Black, RoundedCornerShape(cornerRadius))
                .padding(16.dp),
            contentAlignment = contentAlignment
        ) {
            Column(
                horizontalAlignment = horizontalAlignment
            ) {
                Text(
                    text = ans.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 33.sp,
                    fontFamily = libertinusFont
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 17.sp,
                    fontFamily = libertinusFont
                )
            }
        }
    }



    @Composable
    fun GlowingTopMessageButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        color: Long
    ) {
        val cornerRadius = 10.dp  // 🔽 Reduced from 20.dp to 10.dp
        val pressedState = remember { mutableStateOf(false) }

        val animatedElevation by animateDpAsState(
            targetValue = if (pressedState.value) 2.dp else 10.dp,
            label = "ElevationAnim"
        )

        val libertinusFont = FontFamily(
            Font(R.font.libertinusmath, FontWeight.Normal)
        )

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressedState.value = true
                            tryAwaitRelease()
                            pressedState.value = false
                            onClick()
                        }
                    )
                },
            shape = RoundedCornerShape(cornerRadius),
            tonalElevation = animatedElevation,
            shadowElevation = animatedElevation,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00FFFF), // Cyan
                                Color(0xFFFF00FF)  // Magenta
                            )
                        )
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Know More...",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = libertinusFont
                )
            }
        }
    }










    @Composable
    fun GlowingTopMessageCardForAll1(
        text: String,
        ans: Int,
        modifier: Modifier = Modifier,
        contentAlignment: Alignment = Alignment.CenterStart,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start
    ) {
        val cornerRadius = 20.dp
        val glowColor = Color(0xFF00FFFF).copy(alpha = 1.0f)

        Box(
            modifier = modifier
                .height(130.dp)
                .shadow(
                    elevation = 15.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .background(Color.Black, RoundedCornerShape(cornerRadius))
                .padding(24.dp),
            contentAlignment = contentAlignment
        ) {
            Column(
                horizontalAlignment = horizontalAlignment
            ) {
                Text(
                    text = ans.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }

    @Composable
    fun GlowingTopMessageCardForAll2(
        text: String,
        ans: Int,
        modifier: Modifier = Modifier,
        contentAlignment: Alignment = Alignment.CenterStart,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start
    ) {
        val cornerRadius = 20.dp
        val glowColor = Color(0xFFFF00FF).copy(alpha = 1.0f)

        Box(
            modifier = modifier
                .height(130.dp)
                .shadow(
                    elevation = 15.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .background(Color.Black, RoundedCornerShape(cornerRadius))
                .padding(24.dp),
            contentAlignment = contentAlignment
        ) {
            Column(
                horizontalAlignment = horizontalAlignment
            ) {
                Text(
                    text = ans.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }







    private fun getTodayUsageTime(context: Context, start: Long, end: Long): Long {
        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvents = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()

        val sessionMap = mutableMapOf<String, Long>()
        var totalUsageTime = 0L

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                sessionMap[event.packageName] = event.timeStamp
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                val startTime = sessionMap[event.packageName]
                if (startTime != null && event.timeStamp > startTime) {
                    totalUsageTime += event.timeStamp - startTime
                    sessionMap.remove(event.packageName)
                }
            }
        }

        return totalUsageTime
    }



    private fun getAppUsageStats(context: Context, start: Long, end: Long): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvents = usageStatsManager.queryEvents(start - 60_000, end) // small buffer before start
        val event = UsageEvents.Event()
        val appStartMap = mutableMapOf<String, Long>()
        val appUsageMap = mutableMapOf<String, Long>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (event.timeStamp >= start) {
                        appStartMap[event.packageName] = event.timeStamp
                    }
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val startTime = appStartMap[event.packageName]
                    if (startTime != null) {
                        val adjustedStart = maxOf(startTime, start)
                        val adjustedEnd = minOf(event.timeStamp, end)

                        val duration = adjustedEnd - adjustedStart
                        if (duration in 1..1000L * 60 * 60 * 6) { // ignore absurd sessions
                            appUsageMap[event.packageName] =
                                appUsageMap.getOrDefault(event.packageName, 0L) + duration
                        }
                        appStartMap.remove(event.packageName)
                    }
                }
            }
        }

        // Optionally convert package names to app names here if needed
        return appUsageMap
    }


    private fun getLastTimeUsed(context: Context, start: Long, end: Long): Map<String, Long> {
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        return stats.associate { it.packageName to it.lastTimeUsed }
    }

    private fun getAppLaunchCount(context: Context, start: Long, end: Long): Map<String, Int> {
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val launchCounts = mutableMapOf<String, Int>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkg = event.packageName
                launchCounts[pkg] = launchCounts.getOrDefault(pkg, 0) + 1
            }
        }
        return launchCounts
    }

    private fun getAppSwitchPattern(context: Context, start: Long, end: Long): List<String> {
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val pattern = mutableListOf<String>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                pattern.add(event.packageName)
            }
        }
        return pattern
    }

    private fun getUnlockData(context: Context, start: Long, end: Long): Pair<Int, List<Long>> {
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var count = 0
        val times = mutableListOf<Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                count++
                times.add(event.timeStamp)
            }
        }
        return count to times
    }

    private fun getScreenOnOffCount(context: Context, start: Long, end: Long): Pair<Int, Int> {
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var screenOnCount = 0
        var screenOffCount = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> screenOnCount++
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> screenOffCount++
            }
        }
        return screenOnCount to screenOffCount
    }

    private fun getUsageSessions(context: Context, start: Long, end: Long): List<Pair<Long, Long>> {
        // Pair unlock-lock timestamps (KEYGUARD_HIDDEN → KEYGUARD_SHOWN)
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val sessions = mutableListOf<Pair<Long, Long>>()

        var lastUnlockTime: Long? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    lastUnlockTime = event.timeStamp
                }
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    val lockTime = event.timeStamp
                    if (lastUnlockTime != null) {
                        // Only count if lock comes after unlock
                        if (lockTime > lastUnlockTime) {
                            sessions.add(lastUnlockTime to lockTime)
                            lastUnlockTime = null
                        }
                    }
                }
            }
        }
        return sessions
    }

    private fun getNotificationCounts(context: Context, start: Long, end: Long): Map<String, Int> {
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val counts = mutableMapOf<String, Int>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                event.eventType == 12 // NOTIFICATION_SEEN
            ) {
                val pkg = event.packageName
                counts[pkg] = counts.getOrDefault(pkg, 0) + 1
            }
        }
        return counts
    }

    private fun getHourlyNotificationCounts(context: Context, start: Long, end: Long): Map<Int, Int> {
        // Hour (0-23) -> count of notifications seen
        val manager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val hourlyCounts = mutableMapOf<Int, Int>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                event.eventType == 12 // NOTIFICATION_SEEN
            ) {
                val cal = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hourlyCounts[hour] = hourlyCounts.getOrDefault(hour, 0) + 1
            }
        }
        return hourlyCounts
    }

    private fun getAppName(packageName: String, context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun formatTime(timeMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    data class AppSession(
        val appName: String,
        val packageName: String,
        val startTime: Long,
        val endTime: Long
    )



    private fun getAppUsageSessions(context: Context): List<AppUsageSession> {
        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val usageEvents = usageStatsManager.queryEvents(calendar.timeInMillis, now)
        val event = UsageEvents.Event()
        val sessionMap = mutableMapOf<String, Long>() // package -> startTime
        val sessions = mutableListOf<AppUsageSession>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                sessionMap[event.packageName] = event.timeStamp
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                val startTime = sessionMap[event.packageName]
                if (startTime != null && event.timeStamp - startTime < 1000L * 60 * 60 * 6) { // ignore very long invalid sessions
                    val appName = getAppName(event.packageName, context)
                    sessions.add(
                        AppUsageSession(
                            appName = appName,
                            packageName = event.packageName,
                            startTime = startTime,
                            endTime = event.timeStamp
                        )
                    )
                    sessionMap.remove(event.packageName)
                }
            }
        }

        return sessions.sortedByDescending { it.startTime }
    }


//    @Composable
//    fun AppUsagePieChart(appUsageMap: Map<String, Long>, context: Context) {
//        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
//        val chartSize = screenWidth * 0.95f // 95% of screen width for both width & height
//        val pieEntries = appUsageMap.toList()
//            .filter { it.second > 0 }
//            .sortedByDescending { it.second }
//            .take(10)
//            .map { (pkg, millis) ->
//                val minutes = millis / (1000 * 60f)
//                PieEntry(minutes, pkg)
//            }
//
//        if (pieEntries.isEmpty()) {
//            Text("No usage data to display.")
//            return
//        }
//
//        var selectedEntry by remember { mutableStateOf<PieEntry?>(null) }
//
//        Box (
//            modifier = Modifier.fillMaxWidth(),
//            contentAlignment = Alignment.Center
//        ){
//
//            AndroidView(
//                factory = { ctx ->
//                    PieChart(ctx).apply {
//                        description.isEnabled = false
//                        setUsePercentValues(false)
//                        setDrawEntryLabels(false)
//                        setDrawCenterText(true)
//                        setCenterTextColor(Color.WHITE)
//                        setHoleColor(Color.TRANSPARENT)
//                        setTransparentCircleAlpha(0)
//                        setHoleRadius(40f) // 🔽 reduces the hole size (smaller inner circle)
//
//
//                        legend.apply {
//                            isEnabled = true
//                            textColor = Color.WHITE
//                            textSize = 12f
//                            form = Legend.LegendForm.SQUARE
//                            isWordWrapEnabled = true
//                            maxSizePercent = 1f
//                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
//                            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
//                            orientation = Legend.LegendOrientation.HORIZONTAL
//                            setDrawInside(false)
//                        }
//
//                        val baseColors = ColorTemplate.MATERIAL_COLORS.toMutableList()
//                        val needed = pieEntries.size
//                        if (baseColors.size < needed) {
//                            repeat(needed - baseColors.size) {
//                                baseColors.add(
//                                    Color.rgb(
//                                        (100..255).random(),  // Red
//                                        (100..255).random(),  // Green
//                                        (100..255).random()   // Blue
//                                    )
//                                )
//                            }
//                        }
//
//                        //                    val dataSet = PieDataSet(pieEntries, "").apply {
//                        //                        colors = baseColors
//                        //                        valueTextSize = 0f
//                        //                        sliceSpace = 0f
//                        //                        setDrawValues(false)
//                        //                        setDrawIcons(false)
//                        //                        setAutomaticallyDisableSliceSpacing(false)
//                        //                        selectionShift = 6f
//                        //                    }
//
//
//                        val dataSet = PieDataSet(pieEntries, "").apply {
//                            colors = baseColors
//                            valueTextSize = 0f
//                            sliceSpace =
//                                0f // ✅ Remove spacing between slices (avoids border illusion)
//                            setDrawValues(false)
//                            setDrawIcons(false)
//                            setAutomaticallyDisableSliceSpacing(false)
//                            selectionShift = 6f
//                        }
//
//                        data = PieData(dataSet)
//                        invalidate()
//                        notifyDataSetChanged()
//
//                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
//                            override fun onValueSelected(e: Entry?, h: Highlight?) {
//                                if (e is PieEntry) {
//                                    selectedEntry = e
//                                    val totalSeconds = (e.value * 60).toInt()
//                                    val hours = totalSeconds / 3600
//                                    val minutes = (totalSeconds % 3600) / 60
//                                    val seconds = totalSeconds % 60
//                                    centerText = "${e.label}\n${hours}h ${minutes}m ${seconds}s"
//                                    invalidate()
//                                }
//                            }
//
//                            override fun onNothingSelected() {
//                                selectedEntry = null
//                                centerText = ""
//                                invalidate()
//                            }
//                        })
//                    }
//                },
//
//
//                modifier = Modifier
//                    .width(chartSize)
//                    .height(550.dp)
//
//                //        modifier = Modifier
//                //                .fillMaxWidth(0.95f)
//                //                .height(700.dp)
//            )
//
//
//        }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun AppUsagePieChart(appUsageMap: Map<String, Long>, context: Context) {
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val chartSize = screenWidth * 0.95f // 95% of screen width
        val pieEntries = appUsageMap.toList()
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(10)
            .map { (pkg, millis) ->
                val minutes = millis / (1000 * 60f)
                PieEntry(minutes, pkg)
            }

        if (pieEntries.isEmpty()) {
            Text("No usage data to display.")
            return
        }

        var selectedEntry by remember { mutableStateOf<PieEntry?>(null) }

        // Store the colors to use in both chart and legend
        val baseColors = remember {
            val colors = ColorTemplate.MATERIAL_COLORS.toMutableList()
            if (colors.size < pieEntries.size) {
                repeat(pieEntries.size - colors.size) {
                    colors.add(
                        android.graphics.Color.rgb(
                            (100..255).random(),
                            (100..255).random(),
                            (100..255).random()
                        )
                    )
                }
            }
            colors
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PieChart(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            description.isEnabled = false
                            setUsePercentValues(false)
                            setDrawEntryLabels(false)
                            setDrawCenterText(true)
                            setCenterTextColor(android.graphics.Color.WHITE)
                            setHoleColor(android.graphics.Color.TRANSPARENT)
                            setTransparentCircleAlpha(0)
                            setHoleRadius(40f)
                            legend.isEnabled = false // Hide default legend

                            val dataSet = PieDataSet(pieEntries, "").apply {
                                colors = baseColors
                                valueTextSize = 0f
                                sliceSpace = 0f
                                setDrawValues(false)
                                setDrawIcons(false)
                                setAutomaticallyDisableSliceSpacing(false)
                                selectionShift = 6f
                            }

                            data = PieData(dataSet)
                            invalidate()
                            notifyDataSetChanged()

                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    if (e is PieEntry) {
                                        selectedEntry = e
                                        val totalSeconds = (e.value * 60).toInt()
                                        val hours = totalSeconds / 3600
                                        val minutes = (totalSeconds % 3600) / 60
                                        val seconds = totalSeconds % 60
                                        centerText = "${e.label}\n${hours}h ${minutes}m ${seconds}s"
                                        invalidate()
                                    }
                                }

                                override fun onNothingSelected() {
                                    selectedEntry = null
                                    centerText = ""
                                    invalidate()
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .width(chartSize)
                        .height(400.dp) // You can set this as per your need
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                pieEntries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(baseColors[index]), shape = RectangleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = entry.label,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

}













