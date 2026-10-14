package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyTask
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    appViewModel: AppViewModel
) {
    val tasks by appViewModel.dailyTasks.collectAsStateWithLifecycle()
    val streakCount by appViewModel.streakCount.collectAsStateWithLifecycle()
    val bestStreak by appViewModel.bestStreak.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val headerDateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumberFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

    var selectedDateStr by remember { mutableStateOf(dateFormat.format(Date())) }
    var showAddTaskModal by remember { mutableStateOf(false) }

    val weekDays = remember {
        val days = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..6) {
            days.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        days
    }

    val selectedDateTasks = remember(tasks, selectedDateStr) {
        tasks.filter { it.date == selectedDateStr }
    }

    val completedCount = selectedDateTasks.count { it.isCompleted }
    val totalCount = selectedDateTasks.size
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Schedule", fontWeight = FontWeight.Bold, color = Yellow) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    actionIconContentColor = Yellow
                ),
                actions = {
                    Surface(
                        color = Yellow,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = DarkBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "$streakCount Days",
                                color = DarkBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskModal = true },
                containerColor = Yellow,
                contentColor = DarkBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = Color.White
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Date & Best Streak
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    headerDateFormat.format(Date()),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Yellow
                                )
                                Text(
                                    "Best Streak: $bestStreak Days 🔥",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (streakCount > 0) Yellow else DarkBlueDeep,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (streakCount > 0) DarkBlue else Yellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Weekly View Horizontal Date Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val todayFormatted = dateFormat.format(Date())
                    weekDays.forEach { dateObj ->
                        val dateKey = dateFormat.format(dateObj)
                        val isSelected = selectedDateStr == dateKey
                        val isToday = todayFormatted == dateKey
                        val dayTasks = tasks.filter { it.date == dateKey }
                        val allDone = dayTasks.isNotEmpty() && dayTasks.all { it.isCompleted }

                        Card(
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { selectedDateStr = dateKey },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Yellow else DarkBlue
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Yellow else if (isToday) Yellow.copy(alpha = 0.6f) else CardBorder
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNameFormat.format(dateObj).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DarkBlue else Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = dayNumberFormat.format(dateObj),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DarkBlue else Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = if (allDone) SuccessGreen else if (dayTasks.isNotEmpty()) (if (isSelected) DarkBlue else Yellow) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                    )
                            }
                        }
                    }
                }
            }

            // Daily Progress Card with LinearProgressIndicator
            item {
                Card(
                    border = BorderStroke(1.dp, CardBorder),
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tasks for $selectedDateStr",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Yellow
                            )
                            Text(
                                "$completedCount of $totalCount Completed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (completedCount == totalCount && totalCount > 0) SuccessGreen else Color.White.copy(alpha = 0.75f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (progressFraction == 1f && totalCount > 0) SuccessGreen else Yellow,
                            trackColor = DarkBlueDeep,
                        )
                    }
                }
            }

            // Tasks List
            if (selectedDateTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = Yellow,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No tasks scheduled for this date.", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showAddTaskModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = DarkBlue),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("+ Add Task Now", fontWeight = FontWeight.Bold, color = DarkBlue)
                            }
                        }
                    }
                }
            } else {
                items(selectedDateTasks, key = { it.id }) { task ->
                    Card(
                        border = BorderStroke(1.dp, if (task.isCompleted) CardBorder else Yellow.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkBlue
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { appViewModel.updateDailyTaskCompleted(task, !task.isCompleted) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (task.isCompleted) "Completed" else "Incomplete",
                                    tint = if (task.isCompleted) SuccessGreen else Yellow,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.taskName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (task.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (task.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.isCompleted) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.7f),
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                            IconButton(
                                onClick = { appViewModel.deleteDailyTask(task) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Task",
                                    tint = AlertRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddTaskModal) {
        AddTaskModal(
            onDismiss = { showAddTaskModal = false },
            onSave = { title, category, description, reminderTime ->
                val fullTitle = if (reminderTime.isNotBlank()) "[$reminderTime] $title ($category)" else "$title ($category)"
                appViewModel.addDailyTask(fullTitle, selectedDateStr, description)
                showAddTaskModal = false
            }
        )
    }
}
