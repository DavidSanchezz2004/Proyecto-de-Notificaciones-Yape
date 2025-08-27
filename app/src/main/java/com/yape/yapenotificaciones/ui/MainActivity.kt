package com.yape.yapenotificaciones.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.yape.yapenotificaciones.data.Yapeo
import com.yape.yapenotificaciones.excel.ExcelExporter
import com.yape.yapenotificaciones.notif.YapeNotificationListener
import com.yape.yapenotificaciones.util.formatLocalDateTime
import com.yape.yapenotificaciones.util.isNotificationListenerEnabled
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val zone = ZoneId.of("America/Lima")

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val yapeos by vm.yapeos.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val exporter = remember { ExcelExporter(this) }

                var selectedDay by remember { mutableStateOf(LocalDate.now(zone)) }
                var selectedMonth by remember { mutableStateOf(YearMonth.from(selectedDay)) }

                // ComponentName de tu NotificationListener real
                val component = ComponentName(this, YapeNotificationListener::class.java)

                // Usa tu utilidad existente que recibe String
                val notifEnabled = isNotificationListenerEnabled(
                    this,
                    component.flattenToString()
                )

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { padding ->
                    BoxWithConstraints(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val isCompact = maxWidth < 380.dp

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ===== Banner permiso =====
                            PermissionCard(
                                enabled = notifEnabled,
                                onOpenSettings = {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }
                            )

                            // ===== Filtros / chips =====
                            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Rango", fontWeight = FontWeight.SemiBold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AssistChip(
                                            onClick = {
                                                selectedDay = LocalDate.now(zone)
                                                selectedMonth = YearMonth.from(selectedDay)
                                            },
                                            label = { Text("Hoy / Este mes") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Event, contentDescription = null)
                                            }
                                        )
                                        AssistChip(
                                            onClick = {
                                                selectedMonth = selectedMonth.minusMonths(1)
                                                selectedDay = selectedMonth.atDay(1)
                                            },
                                            label = { Text("Mes -1") },
                                            leadingIcon = {
                                                Icon(Icons.Default.EventNote, contentDescription = null)
                                            }
                                        )
                                        AssistChip(
                                            onClick = {
                                                selectedDay = selectedDay.minusDays(1)
                                                selectedMonth = YearMonth.from(selectedDay)
                                            },
                                            label = { Text("Día -1") },
                                            leadingIcon = {
                                                Icon(Icons.Default.EventBusy, contentDescription = null)
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AssistChip(onClick = {}, label = { Text("Día: $selectedDay") })
                                        AssistChip(onClick = {}, label = { Text("Mes: $selectedMonth") })
                                    }
                                }
                            }

                            // ===== Acciones principales =====
                            if (isCompact) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val items = vm.getDayItems(selectedDay)
                                                val uri = exporter.exportDay(selectedDay, items)
                                                if (uri != null) {
                                                    val res = snackbarHostState.showSnackbar(
                                                        message = "Excel de Día guardado",
                                                        actionLabel = "Abrir"
                                                    )
                                                    if (res == SnackbarResult.ActionPerformed) exporter.tryOpenUri(uri)
                                                } else {
                                                    snackbarHostState.showSnackbar("Error al guardar Excel de Día")
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Exportar Día")
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val items = vm.getMonthItems(selectedMonth)
                                                val uri = exporter.exportMonth(selectedMonth, items)
                                                if (uri != null) {
                                                    val res = snackbarHostState.showSnackbar(
                                                        message = "Excel de Mes guardado",
                                                        actionLabel = "Abrir"
                                                    )
                                                    if (res == SnackbarResult.ActionPerformed) exporter.tryOpenUri(uri)
                                                } else {
                                                    snackbarHostState.showSnackbar("Error al guardar Excel de Mes")
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Exportar Mes")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            vm.borrarTodo {
                                                scope.launch { snackbarHostState.showSnackbar("Todos los registros borrados") }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFD32F2F))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Borrar todo")
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val items = vm.getDayItems(selectedDay)
                                                val uri = exporter.exportDay(selectedDay, items)
                                                if (uri != null) {
                                                    val res = snackbarHostState.showSnackbar(
                                                        message = "Excel de Día guardado",
                                                        actionLabel = "Abrir"
                                                    )
                                                    if (res == SnackbarResult.ActionPerformed) exporter.tryOpenUri(uri)
                                                } else {
                                                    snackbarHostState.showSnackbar("Error al guardar Excel de Día")
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Exportar Día")
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val items = vm.getMonthItems(selectedMonth)
                                                val uri = exporter.exportMonth(selectedMonth, items)
                                                if (uri != null) {
                                                    val res = snackbarHostState.showSnackbar(
                                                        message = "Excel de Mes guardado",
                                                        actionLabel = "Abrir"
                                                    )
                                                    if (res == SnackbarResult.ActionPerformed) exporter.tryOpenUri(uri)
                                                } else {
                                                    snackbarHostState.showSnackbar("Error al guardar Excel de Mes")
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Exportar Mes")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            vm.borrarTodo {
                                                scope.launch { snackbarHostState.showSnackbar("Todos los registros borrados") }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFD32F2F))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Borrar todo")
                                    }
                                }
                            }

                            // ===== Lista =====
                            if (yapeos.isEmpty()) {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Sin movimientos aún", fontWeight = FontWeight.Bold)
                                        Text(
                                            "Habilita el acceso a notificaciones y espera una notificación de Yape.\n" +
                                                    "Cuando llegue, se mostrará aquí y podrás exportar."
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(yapeos) { y -> YapeItem(y) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(enabled: Boolean, onOpenSettings: () -> Unit) {
    val icon = if (enabled) Icons.Default.NotificationsActive else Icons.Default.Notifications
    val bg = if (enabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val fg = if (enabled) Color(0xFF2E7D32) else Color(0xFFC62828)
    val title = if (enabled) "Acceso a notificaciones habilitado" else "Acceso a notificaciones deshabilitado"
    val subtitle = if (enabled)
        "La app está escuchando notificaciones de Yape."
    else
        "Actívalo para poder registrar los Yapes."

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bg)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fg)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = fg)
                Text(subtitle, color = fg.copy(alpha = 0.9f))
            }
            if (!enabled) {
                Button(onClick = onOpenSettings) { Text("Habilitar") }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = fg)
            }
        }
    }
}

@Composable
private fun YapeItem(y: Yapeo) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${y.currency} ${"%,.2f".format(y.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = {}, label = { Text(y.direction.name) })
            }
            Text(formatLocalDateTime(y.timestamp), color = Color.Gray)
            if (!y.counterpart.isNullOrBlank()) {
                Text(y.counterpart, fontWeight = FontWeight.SemiBold)
            }
            if (!y.rawText.isNullOrBlank()) {
                Text(y.rawText)
            }
        }
    }
}
