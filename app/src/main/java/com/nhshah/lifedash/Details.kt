package com.nhshah.lifedash

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.view.ViewGroup
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.Color as Col

@Composable
fun DetailsScreen(
    padding: PaddingValues,
    usageSessions: List<Pair<Long, Long>>,
    appUsageMap: Map<String, Long>,
    appSessions: List<MainActivity.AppUsageSession>,
    context: Context,
    totalHr: Long,
    totalMin: Long,
    totalSec: Long,
    unlocks: Int,
    totalNotifications: Int,
    totalAppsUsed: Int,
    sessionCount: Int,
    onBackClick: () -> Unit
) {
    val pageCount = 3
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(padding)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> Page1(totalNotifications, totalHr, totalMin, totalSec, totalAppsUsed, sessionCount, unlocks)
                1 -> Page2(appUsageMap, context)
                2 -> Page3(sessionCount, appSessions, usageSessions, context)
            }
        }

        val coroutineScope = rememberCoroutineScope()

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(4.dp)
                        .background(
                            color = Color.Cyan.copy(alpha = if (isSelected) 1f else 0.4f),
                            shape = CircleShape
                        )
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    index,
                                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing) // ⏱️ Slower scroll (default is ~250ms)
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun TriangularMergeLoader(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    color: Color = Color.Cyan,
    showOverlay: Boolean = true // ⬅️ Change to false to hide overlay
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 🔄 Animation for merging/unmerging
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 🔄 Animation for constant rotation
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier.size(size)) {

        // 🎯 Actual rotating & merging loader
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasSize = size.toPx()
            val center = Offset(canvasSize / 2, canvasSize / 2)
            val r = canvasSize / 10
            val offset = canvasSize / 2.5f * (1 - progress)

            val baseAngleRad = Math.toRadians(angle.toDouble()).toFloat()
            val angles = listOf(0f, 120f, 240f)

            angles.forEach { relativeAngle ->
                val theta = baseAngleRad + Math.toRadians(relativeAngle.toDouble()).toFloat()
                val x = center.x + offset * cos(theta)
                val y = center.y + offset * sin(theta)
                drawCircle(color = color, radius = r, center = Offset(x, y))
            }
        }

        // 🧊 Overlay that sits ABOVE the loader
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun Page1(
    totalNotifications: Int,
    totalHr: Long,
    totalMin: Long,
    totalSec: Long,
    totalAppsUsed: Int,
    sessionCount: Int,
    unlockCount: Int,
) {
    val libertinusFont = FontFamily(
        Font(R.font.libertinusmath, FontWeight.Normal)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top)
    ) {
        // 💡 Quote

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Every tap, every scroll, every unlock — a trade of your time. Is it worth it?",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontFamily = libertinusFont,
                textAlign = TextAlign.Center
            )
        }


        // ⏱️ Total Screen Time
        GlowingTopMessageCard(
            totalHr = totalHr,
            totalMin = totalMin,
            totalSec = totalSec
        )

        // 🔢 & 🟣 Row for Apps Used and Sessions
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlowingTopMessageCardForAll(
                text = "Apps Used Today",
                ans = totalAppsUsed,
                color = 0xFFFF00FF,
                modifier = Modifier.weight(1f)
            )

            GlowingTopMessageCardForAll(
                text = "Total Sessions",
                ans = sessionCount,
                color = 0xFFFF00FF,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlowingTopMessageCardForAll(
                text = "Phone Unlocks",
                ans = unlockCount,
                color = 0xFFFF00FF,
                modifier = Modifier.weight(1f)
            )

            GlowingTopMessageCardForAll(
                text = "Total Notifications",
                ans = totalNotifications,
                color = 0xFFFF00FF,
                modifier = Modifier.weight(1f)
            )
        }
        // ⌛ Semi-donut chart for screen time vs off-screen time
        SemiDonutChart(
            screenTimeMinutes = (totalHr * 60 + totalMin).toFloat(),
            modifier = Modifier
                .padding(top = 20.dp)
        )

    }
}


@Composable
fun Page2(appUsageMap: Map<String, Long>, context: Context) {
    val libertinusFont = FontFamily(
        Font(R.font.libertinusmath, FontWeight.Normal)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val totalUsageMillis = appUsageMap.values.sum()
        val top5UsageMillis = appUsageMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .sumOf { it.value }

        val percentTop5 = if (totalUsageMillis > 0)
            String.format("%.2f", (top5UsageMillis.toDouble() / totalUsageMillis) * 100)
        else "0.00"

        GlowingTopStatCard(
            statText = "$percentTop5%",
            subText = "Of today's usage came from your top 5 Apps",
            color = 0xFF00FFFF,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 4.dp, end = 4.dp)
        )

        // 📊 Pie chart for top apps
        AppUsagePieChart(appUsageMap, context)

        Spacer(modifier = Modifier.height(24.dp))

        // 🧾 Header
        Text(
            text = "All Apps Used Today",
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = libertinusFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 🪟 Sort by usage time descending
        val sortedApps = appUsageMap.toList()
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        sortedApps.forEachIndexed { index, (pkg, millis) ->
            val appName = getAppNameFromPackage(context, pkg)
            val totalSec = millis / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60

            val medalEmoji = when (index) {
                0 -> " 🥇"
                1 -> " 🥈"
                2 -> " 🥉"
                else -> ""
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = appName + medalEmoji,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = libertinusFont
                )
                Text(
                    text = String.format("%02dh %02dm %02ds", h, m, s),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontFamily = libertinusFont
                )
            }
            Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)
        }
    }
}


//@Composable
//fun Page3(sessionCount: Int) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(
//            text = "Today's Total Sessions",
//            color = Color.White,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Light
//        )
//        Spacer(modifier = Modifier.height(12.dp))
//        Text(
//            text = "$sessionCount",
//            color = Color.Cyan,
//            fontSize = 60.sp,
//            fontWeight = FontWeight.Bold
//        )
//        Spacer(modifier = Modifier.height(24.dp))
//        Text(
//            text = "Each session is a conscious return to your phone.\nNotice the habit pattern.",
//            color = Color.White.copy(alpha = 0.6f),
//            fontSize = 14.sp,
//            textAlign = TextAlign.Center
//        )
//    }
//}


//@Composable
//fun Page3(sessionCount: Int, appSessions: List<MainActivity.AppUsageSession>, context: Context) {}
//    val libertinusFont = FontFamily(Font(R.font.libertinusmath, FontWeight.Normal))
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//            .padding(16.dp)
//            .verticalScroll(rememberScrollState())
//    ) {
//        Text(
//            text = "🧭 App Session History:",
//            fontWeight = FontWeight.Bold,
//            color = Color.White,
//            fontFamily = libertinusFont,
//            fontSize = 18.sp
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        if (appSessions.isEmpty()) {
//            Text(
//                text = "No session data available today.",
//                color = Color.White.copy(alpha = 0.7f),
//                fontFamily = libertinusFont
//            )
//        } else {
//            appSessions.forEach { session ->
//                val totalSec = (session.endTime - session.startTime) / 1000
//                val minutes = totalSec / 60
//                val seconds = totalSec % 60
//                val start = formatTime(session.startTime)
//                val end = formatTime(session.endTime)
//                val appName = getAppNameFromPackage(context, session.appName)
//
//                Text(
//                    text = "• ${appName}: $start → $end (${minutes}m ${seconds}s)",
//                    color = Color.White,
//                    fontFamily = libertinusFont,
//                    fontSize = 14.sp,
//                    modifier = Modifier.padding(vertical = 4.dp)
//                )
//                Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)
//            }
//        }
//    }
//}

@Composable
fun Page3(
    sessionCount: Int,
    appSessions: List<MainActivity.AppUsageSession>,
    usageSessions: List<Pair<Long, Long>>,
    context: Context
) {
    val hourlySessionCounts = remember(usageSessions) {
        val counts = IntArray(24)
        for ((start, _) in usageSessions) {
            val hour = Calendar.getInstance().apply {
                timeInMillis = start
            }.get(Calendar.HOUR_OF_DAY)
            counts[hour]++
        }
        counts.toList()
    }

    // ✅ Wrap everything in one scrollable Column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // scrolls the whole content
            .padding(16.dp)
    ) {
        GlowingTopMessageCardForSessions(
            text = "Total Sessions",
            ans = sessionCount,
            color = 0xFF00FFFF,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        SessionLineChart(hourlySessionCounts)
        Spacer(modifier = Modifier.height(16.dp))
        UnlockSessionsList(usageSessions = usageSessions)

    }
}

@Composable
fun SessionLengthBarChart(sessions: List<AppUsageSession>) {
    // Step 1: Bucket durations
    val sessionBuckets = remember(sessions) {
        val buckets = mutableMapOf<String, Int>()
        val ranges = listOf(
            60L to "0–1 min",
            5 * 60L to "1–5 min",
            10 * 60L to "5–10 min",
            30 * 60L to "10–30 min",
            Long.MAX_VALUE to "30+ min"
        )
        for (session in sessions) {
            val durationSec = (session.endTime - session.startTime) / 1000
            val label = ranges.first { durationSec <= it.first }.second
            buckets[label] = (buckets[label] ?: 0) + 1
        }
        buckets.toSortedMap(compareBy {
            // Sort by custom order
            listOf("0–1 min", "1–5 min", "5–10 min", "10–30 min", "30+ min").indexOf(it)
        })
    }

    // Step 2: Prepare entries
    val entries = sessionBuckets.entries.mapIndexed { index, entry ->
        BarEntry(index.toFloat(), entry.value.toFloat())
    }

    val labels = sessionBuckets.keys.toList()

    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    500
                )

                setBackgroundColor(Col.TRANSPARENT)
                setDrawGridBackground(false)
                setPinchZoom(false)
                setScaleEnabled(false)
                setTouchEnabled(true)

                description.isEnabled = false
                legend.isEnabled = false

                val dataSet = BarDataSet(entries, "").apply {
                    color = android.graphics.Color.CYAN
                    valueTextColor = android.graphics.Color.WHITE
                    valueTextSize = 10f
                }

                val barData = BarData(dataSet)
                barData.barWidth = 0.7f
                data = barData

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = android.graphics.Color.WHITE
                    valueFormatter = IndexAxisValueFormatter(labels)
                    setDrawGridLines(false)
                    textSize = 12f
                }

                axisLeft.apply {
                    textColor = android.graphics.Color.WHITE
                    axisMinimum = 0f
                    granularity = 1f
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.DKGRAY
                }

                axisRight.isEnabled = false

                invalidate()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}


@Composable
fun SessionLineChart(sessionCounts: List<Int>) {
    var selectedInfo by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // 🔼 Info displayed above the chart
        selectedInfo?.let {
            Text(
                text = it,
                color = Color.White,
                fontSize = 16.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
        }

        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        600
                    )

                    setBackgroundColor(Col.TRANSPARENT)
                    setDrawGridBackground(false)
                    setViewPortOffsets(20f, 20f, 20f, 30f)

                    setTouchEnabled(true)
                    isHighlightPerTapEnabled = true
                    setPinchZoom(true)
                    setScaleEnabled(false)

                    description.isEnabled = true
                    description.text = ""
                    description.textColor = Col.WHITE
                    description.textSize = 12f
                    legend.isEnabled = false
                    legend.textColor = Col.WHITE

                    val entries = sessionCounts.mapIndexed { index, value ->
                        Entry(index.toFloat(), value.toFloat())
                    }

                    val dataSet = LineDataSet(entries, "").apply {
                        color = Col.CYAN
                        lineWidth = 2.5f
                        setDrawCircles(true)
                        circleRadius = 2f
                        setCircleColor(Col.CYAN)
                        setDrawValues(false)
                        setDrawHighlightIndicators(true)
                        highLightColor = Col.WHITE
                    }

                    val lineData = LineData(dataSet)
                    data = lineData

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        granularity = 1f
                        textColor = Col.WHITE
                        textSize = 10f
                        setDrawAxisLine(true)
                        setDrawGridLines(true)
                        gridColor = Col.DKGRAY
                        valueFormatter = IndexAxisValueFormatter((0..23).map { "$it h" })
                        labelRotationAngle = -45f
                    }

                    axisLeft.apply {
                        textColor = Col.WHITE
                        textSize = 10f
                        axisMinimum = 0f
                        granularity = 1f
                        setDrawGridLines(true)
                        gridColor = Col.DKGRAY
                    }

                    axisRight.isEnabled = false

                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            e?.let {
                                val x = it.x.toInt()
                                val y = it.y.toInt()
                                selectedInfo = "\n$y sessions between ${x}h - ${x + 1}h"

                                dataSet.setDrawValues(false)
                                invalidate()
                            }
                        }

                        override fun onNothingSelected() {
                            selectedInfo = null
                            dataSet.setDrawValues(false)
                            invalidate()
                        }
                    })

                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
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
            .height(115.dp)
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
            .height(115.dp)
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
fun GlowingTopMessageCardForSessions(
    text: String,
    ans: Int,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
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
            .height(115.dp)
            .shadow(
                elevation = 20.dp,
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
fun SemiDonutChart(
    modifier: Modifier = Modifier,
    screenTimeMinutes: Float,
    totalDayMinutes: Float = 1440f,
    screenColor: Color = Color.Cyan,
    offScreenColor: Color = Color.DarkGray
) {
    val sweepAngleScreen = (screenTimeMinutes / totalDayMinutes) * 180f
    val sweepAngleOff = 180f - sweepAngleScreen

    val percentWithPhone = String.format("%.2f", screenTimeMinutes / totalDayMinutes * 100)
    val percentWithoutPhone = String.format("%.2f", (1 - screenTimeMinutes / totalDayMinutes) * 100)

    var selectedLabel by remember {
        mutableStateOf("With Phone\n\n($percentWithPhone%)")
    }
    val strokeWidth = 60f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height.toFloat())
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                        if (angle < 0f) angle += 360f

                        if (angle in 180f..360f) {
                            selectedLabel = if (angle <= 180f + sweepAngleScreen) {
                                "With Phone\n\n($percentWithPhone%)"
                            } else {
                                "Without Phone\n\n($percentWithoutPhone%)"
                            }
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            val topLeft = Offset(0f, 0f)
            val arcSize = Size(canvasWidth, canvasHeight * 2)

            drawArc(
                color = screenColor,
                startAngle = 180f,
                sweepAngle = sweepAngleScreen,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            drawArc(
                color = offScreenColor,
                startAngle = 180f + sweepAngleScreen,
                sweepAngle = sweepAngleOff,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }

        if (selectedLabel.isNotEmpty()) {
            Text(
                text = selectedLabel,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 14.sp, // adds spacing between lines
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

val knownApps = mapOf(
    // 🌐 Social Media & Messaging
    "com.instagram.android" to "Instagram",
    "com.whatsapp" to "WhatsApp",
    "com.facebook.katana" to "Facebook",
    "com.facebook.orca" to "Messenger",
    "com.snapchat.android" to "Snapchat",
    "com.twitter.android" to "Twitter",
    "com.reddit.frontpage" to "Reddit",
    "com.discord" to "Discord",
    "com.telegram.messenger" to "Telegram",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.beeasy.shopee" to "Shopee",

    // 🎬 Streaming & Entertainment
    "com.google.android.youtube" to "YouTube",
    "com.netflix.mediaclient" to "Netflix",
    "com.amazon.avod.thirdpartyclient" to "Amazon Prime Video",
    "com.hotstar.android" to "Disney+ Hotstar",
    "tv.mola.app" to "Mola TV",
    "com.mxtech.videoplayer.ad" to "MX Player",
    "com.jio.media.ondemand" to "JioCinema",
    "com.spotify.music" to "Spotify",
    "com.gaana" to "Gaana",
    "com.wynk.music" to "Wynk Music",
    "com.saranyu.shemarooworld" to "Shemaroo",
    "shemaroo.me" to "Shemaroo",

    // 🌍 Browsers
    "com.android.chrome" to "Google Chrome",
    "org.mozilla.firefox" to "Firefox",
    "com.opera.browser" to "Opera",
    "com.microsoft.emmx" to "Microsoft Edge",
    "com.brave.browser" to "Brave Browser",

    // ☎️ Calling & Communication
    "com.google.android.dialer" to "Phone",
    "com.google.android.contacts" to "Contacts",
    "com.truecaller" to "Truecaller",
    "com.google.android.apps.messaging" to "Messages",
    "com.microsoft.teams" to "Microsoft Teams",
    "com.skype.raider" to "Skype",

    // 🛠 Utilities & Tools
    "com.google.android.gm" to "Gmail",
    "com.google.android.calendar" to "Google Calendar",
    "com.google.android.keep" to "Google Keep",
    "com.google.android.apps.docs" to "Google Docs",
    "com.google.android.apps.sheets" to "Google Sheets",
    "com.google.android.apps.slides" to "Google Slides",
    "com.google.android.apps.maps" to "Google Maps",
    "com.google.earth" to "Google Earth",
    "com.google.android.calculator" to "Calculator",
    "com.google.android.apps.photos" to "Google Photos",
    "com.google.android.apps.nbu.paisa.user" to "Google Pay",
    "com.google.android.apps.youtube.creator" to "YouTube Studio",
    "com.google.android.googlequicksearchbox" to "Google Search",
    "com.google.android.inputmethod.latin" to "Gboard",
    "com.google.android.apps.docs.editors.sheets" to "Google Sheets Editor",

    // 🛒 E-commerce & Delivery
    "com.flipkart.android" to "Flipkart",
    "in.amazon.mShop.android.shopping" to "Amazon",
    "com.meesho.supply" to "Meesho",
    "com.myntra.android" to "Myntra",
    "com.nykaa.android" to "Nykaa",
    "com.swiggy.android" to "Swiggy",
    "com.zomato" to "Zomato",
    "com.bigbasket.mobileapp" to "BigBasket",
    "com.dunzo.user" to "Dunzo",

    // 💸 Finance & UPI
    "com.phonepe.app" to "PhonePe",
    "net.one97.paytm" to "Paytm",
    "com.mobikwik_new" to "Mobikwik",
    "com.bhim.upi" to "BHIM UPI",

    // ⚙️ System & OEM Services
    "com.google.android.gms" to "Google Play Services",
    "com.android.settings" to "Settings",
    "com.android.vending" to "Google Play Store",
    "com.miui.securitycenter" to "MI Security",
    "com.samsung.android.themestore" to "Samsung Theme Store",

    // 📚 Study & Reading
    "com.duolingo" to "Duolingo",
    "com.khanacademy.android" to "Khan Academy",
    "org.coursera.android" to "Coursera",
    "org.udemy.android" to "Udemy",
    "com.google.android.apps.books" to "Google Play Books",
    "com.adobe.reader" to "Adobe Acrobat Reader",

    // 👨‍💻 Developer Tools
    "com.termux" to "Termux",
    "org.qpython.qpy" to "QPython",
    "com.jetbrains.intellij" to "JetBrains IDE",
    "com.sololearn" to "SoloLearn",

    // 🧩 Miscellaneous
    "com.android.camera" to "Camera",
    "com.android.gallery3d" to "Gallery",
    "com.cleanmaster.mguard" to "Clean Master",
    "cn.bluepulse.caption" to "BluePulse Captions",
    "com.nis.app" to "Inshorts",
    "com.google.android.photopicker" to "Photo Picker"
)



fun getAppNameFromPackage(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(applicationInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        knownApps[packageName] ?: packageName
    }
}

fun FontFamily.toAndroidTypeface(context: Context): android.graphics.Typeface {
    return ResourcesCompat.getFont(context, R.font.libertinusmath)!!
}


@Composable
fun AppUsagePieChart(appUsageMap: Map<String, Long>, context: Context) {
    val libertinusFont = FontFamily(
        Font(R.font.libertinusmath, FontWeight.Normal)
    )

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val chartSize = screenWidth * 0.95f
    val pieEntries = appUsageMap.toList()
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(5)
        .map { (pkg, millis) ->
            val minutes = millis / (1000 * 60f)
            val appName = getAppNameFromPackage(context, pkg)
            PieEntry(minutes, appName)
        }

    if (pieEntries.isEmpty()) {
        Text(
            text = "No usage data to display.",
            fontFamily = libertinusFont,
            color = Color.White,
            fontSize = 16.sp
        )
        return
    }

    var selectedEntry by remember { mutableStateOf<PieEntry?>(null) }

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
                        setCenterTextTypeface(libertinusFont.toAndroidTypeface(ctx))
                        setCenterTextColor(android.graphics.Color.WHITE)
                        setCenterTextSize(16f) // 👈 Increase this for bigger text
                        setHoleColor(android.graphics.Color.TRANSPARENT)
                        setTransparentCircleAlpha(0)
                        setHoleRadius(40f)
                        legend.isEnabled = false

                        val dataSet = PieDataSet(pieEntries, "").apply {
                            colors = baseColors
                            valueTextSize = 0f
                            sliceSpace = 0f
                            setDrawValues(false)
                            setDrawIcons(false)
                            selectionShift = 6f
                        }

                        data = PieData(dataSet)
                        invalidate()
                        notifyDataSetChanged()

                        val topEntry = pieEntries[0]
                        selectedEntry = topEntry
                        val totalSeconds = (topEntry.value * 60).toInt()
                        val hours = totalSeconds / 3600
                        val minutes = (totalSeconds % 3600) / 60
                        val seconds = totalSeconds % 60
                        centerText = "${topEntry.label}\n${hours}h ${minutes}m ${seconds}s"
                        highlightValue(Highlight(0f, 0, 0))

                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                if (e is PieEntry) {
                                    if (e == selectedEntry) {
                                        selectedEntry = null
                                        centerText = ""
                                        highlightValues(null)
                                    } else {
                                        selectedEntry = e
                                        val totalSec = (e.value * 60).toInt()
                                        val h = totalSec / 3600
                                        val m = (totalSec % 3600) / 60
                                        val s = totalSec % 60
                                        centerText = "${e.label}\n${h}h ${m}m ${s}s"
                                    }
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
                    .height(400.dp)
            )
        }
    }
}

@Composable
fun GlowingTopStatCard(
    statText: String,         // e.g., "81.37%"
    subText: String,          // e.g., "of today's phone usage"
    modifier: Modifier = Modifier,
    color: Long = 0xFF00FFFF
) {
    val cornerRadius = 20.dp
    val glowColor = Color(color)

    val libertinusFont = FontFamily(
        Font(R.font.libertinusmath, FontWeight.Normal)
    )

    Box(
        modifier = modifier
            .height(115.dp)
            .shadow(
                elevation = 20.dp,
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
                text = statText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                fontFamily = libertinusFont
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                fontFamily = libertinusFont
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

data class AppUsageSession(
    val appName: String,
    val startTime: Long,
    val endTime: Long
)

fun groupSessions(appSessions: List<MainActivity.AppUsageSession>): List<Set<String>> {
    if (appSessions.isEmpty()) return emptyList()

    val sessionGap = 10 * 60 * 1000L // 10 minutes
    val sorted = appSessions.sortedBy { it.startTime }

    val grouped = mutableListOf<MutableSet<String>>()
    var currentSet = mutableSetOf(sorted[0].appName)
    var lastEndTime = sorted[0].endTime

    for (i in 1 until sorted.size) {
        val session = sorted[i]
        if (session.startTime - lastEndTime > sessionGap) {
            grouped.add(currentSet)
            currentSet = mutableSetOf()
        }
        currentSet.add(session.appName)
        lastEndTime = session.endTime
    }
    grouped.add(currentSet)

    return grouped // Each set = unique apps used in a session
}

fun toBarEntries(grouped: List<Set<String>>): List<BarEntry> {
    return grouped.mapIndexed { index, appSet ->
        BarEntry(index.toFloat(), appSet.size.toFloat()) // Y = number of apps
    }
}

@Composable
fun SessionAppUsageBarChart(appSessions: List<MainActivity.AppUsageSession>) {
    val grouped = remember { groupSessions(appSessions) }
    val barEntries = remember { toBarEntries(grouped) }

    AndroidView(
        factory = { ctx ->
            HorizontalBarChart(ctx).apply {
                setDrawGridBackground(false)
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(false)
                description.isEnabled = false
                setFitBars(true)

                axisLeft.apply {
                    axisMinimum = 0f
                    granularity = 1f
                    textColor = android.graphics.Color.WHITE
                    setDrawGridLines(false)
                }

                axisRight.isEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = android.graphics.Color.WHITE
                    granularity = 1f
                    setDrawGridLines(false)
                    valueFormatter = IndexAxisValueFormatter(
                        grouped.mapIndexed { i, _ -> "S${i + 1}" }
                    )
                }

                legend.isEnabled = false

                val dataSet = BarDataSet(barEntries, "Apps Used").apply {
                    color = android.graphics.Color.CYAN
                    valueTextColor = android.graphics.Color.WHITE
                    valueTextSize = 12f
                }

                data = BarData(dataSet).apply {
                    barWidth = 0.6f
                }

                invalidate()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height((barEntries.size * 30).dp) // 📈 30dp per bar session
            .padding(16.dp)
    )
}

fun groupSessionsByUnlocks(
    unlockTimestamps: List<Long>,
    appSessions: List<MainActivity.AppUsageSession>
): List<Pair<Long, List<MainActivity.AppUsageSession>>> {
    val result = mutableListOf<Pair<Long, List<MainActivity.AppUsageSession>>>()

    for (i in unlockTimestamps.indices) {
        val start = unlockTimestamps[i]
        val end = if (i < unlockTimestamps.lastIndex) unlockTimestamps[i + 1] else Long.MAX_VALUE

        val sessionsInPeriod = appSessions.filter {
            it.startTime in start until end
        }

        result.add(start to sessionsInPeriod)
    }

    return result
}

fun groupIntoDeviceSessions(appSessions: List<MainActivity.AppUsageSession>): List<List<MainActivity.AppUsageSession>> {
    if (appSessions.isEmpty()) return emptyList()

    val sorted = appSessions.sortedBy { it.startTime }
    val sessionGap = 10 * 60 * 1000L // 10 minutes

    val result = mutableListOf<MutableList<MainActivity.AppUsageSession>>()
    var current = mutableListOf(sorted[0])

    for (i in 1 until sorted.size) {
        val prev = sorted[i - 1]
        val curr = sorted[i]
        if (curr.startTime - prev.endTime > sessionGap) {
            result.add(current)
            current = mutableListOf()
        }
        current.add(curr)
    }
    result.add(current)
    return result
}

@Composable
fun UnlockSessionsList(
    usageSessions: List<Pair<Long, Long>>
) {
    val libertinusFont = FontFamily(Font(R.font.libertinusmath, FontWeight.Normal))

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "🔓 Unlock Sessions",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = libertinusFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        usageSessions.forEachIndexed { index, (start, end) ->
            val startFormatted = formatTime(start)
            val endFormatted = formatTime(end)

            Text(
                text = "• U${index + 1}: $startFormatted → $endFormatted",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontFamily = libertinusFont,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)
        }
    }
}
