package com.martin.misfinanzas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.martin.misfinanzas.data.CsvImportador
import com.martin.misfinanzas.data.PdfImportador
import com.martin.misfinanzas.data.ResultadoLecturaPdf
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.TransactionViewModel
import com.martin.misfinanzas.ui.screens.AddTransactionDialog
import com.martin.misfinanzas.ui.screens.ComparacionExtractoScreen
import com.martin.misfinanzas.ui.screens.DashboardScreen
import com.martin.misfinanzas.ui.screens.HistorialScreen
import com.martin.misfinanzas.ui.screens.HomeScreen
import com.martin.misfinanzas.ui.screens.PendientesScreen
import com.martin.misfinanzas.ui.theme.MisFinanzasTheme

private enum class Pantalla { INICIO, HISTORIAL, PENDIENTES, RESUMEN }

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionViewModel by viewModels()

    private fun accesoNotificacionesActivo(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        setContent {
            MisFinanzasTheme {
                var pantalla by remember { mutableStateOf(Pantalla.INICIO) }
                var dialogoAbierto by remember { mutableStateOf(false) }
                var transaccionEnEdicion by remember { mutableStateOf<Transaction?>(null) }
                var notiActivo by remember { mutableStateOf(accesoNotificacionesActivo()) }

                var urisPendientesPassword by remember { mutableStateOf<List<Uri>>(emptyList()) }
                var passwordIngresada by remember { mutableStateOf("") }
                var errorLectura by remember { mutableStateOf<String?>(null) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            notiActivo = accesoNotificacionesActivo()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val transacciones by viewModel.transacciones.collectAsState()
                val pendientes by viewModel.pendientesRevision.collectAsState()
                val automaticas by viewModel.automaticas.collectAsState()
                
                val balanceNequi by viewModel.balanceNequi.collectAsState()
                val balanceBancolombia by viewModel.balanceBancolombia.collectAsState()
                val balanceNu by viewModel.balanceNu.collectAsState()
                val balanceEfectivo by viewModel.balanceEfectivo.collectAsState()
                
                val comparacion by viewModel.comparacion.collectAsState()

                fun procesarArchivos(uris: List<Uri>, password: String?) {
                    val movimientos = mutableListOf<com.martin.misfinanzas.data.MovimientoExtracto>()
                    var necesitaPassword = false
                    var error: String? = null

                    for (uri in uris) {
                        val tipo = contentResolver.getType(uri)
                        if (tipo == "application/pdf") {
                            when (val resultado = PdfImportador.leer(this@MainActivity, uri, password)) {
                                is ResultadoLecturaPdf.Exito -> movimientos.addAll(resultado.movimientos)
                                is ResultadoLecturaPdf.RequierePassword -> necesitaPassword = true
                                is ResultadoLecturaPdf.Error -> error = resultado.mensaje
                            }
                        } else {
                            val texto = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                            if (texto != null) movimientos.addAll(CsvImportador.aMovimientos(texto))
                        }
                    }

                    when {
                        necesitaPassword -> {
                            urisPendientesPassword = uris
                        }
                        movimientos.isNotEmpty() -> {
                            urisPendientesPassword = emptyList()
                            passwordIngresada = ""
                            viewModel.prepararComparacion(movimientos)
                        }
                        else -> {
                            urisPendientesPassword = emptyList()
                            errorLectura = error ?: "No se encontraron movimientos en los archivos seleccionados."
                        }
                    }
                }

                val selectorArchivo = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        procesarArchivos(uris, password = null)
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = pantalla == Pantalla.INICIO,
                                onClick = { pantalla = Pantalla.INICIO },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
                                label = { Text("Inicio") }
                            )
                            NavigationBarItem(
                                selected = pantalla == Pantalla.HISTORIAL,
                                onClick = { pantalla = Pantalla.HISTORIAL },
                                icon = { Icon(Icons.Filled.History, contentDescription = "Historial") },
                                label = { Text("Historial") }
                            )
                            NavigationBarItem(
                                selected = pantalla == Pantalla.PENDIENTES,
                                onClick = { pantalla = Pantalla.PENDIENTES },
                                icon = {
                                    if (pendientes.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${pendientes.size}") } }) {
                                            Icon(Icons.Filled.PendingActions, contentDescription = "Pendientes")
                                        }
                                    } else {
                                        Icon(Icons.Filled.PendingActions, contentDescription = "Pendientes")
                                    }
                                },
                                label = { Text("Pendientes") }
                            )
                            NavigationBarItem(
                                selected = pantalla == Pantalla.RESUMEN,
                                onClick = { pantalla = Pantalla.RESUMEN },
                                icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Resumen") },
                                label = { Text("Resumen") }
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            transaccionEnEdicion = null
                            dialogoAbierto = true
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Agregar movimiento")
                        }
                    }
                ) { padding ->
                    val modifier = Modifier.padding(padding)

                    val comparacionActual = comparacion
                    if (comparacionActual != null) {
                        ComparacionExtractoScreen(
                            comparacion = comparacionActual,
                            onAgregarFaltantes = { viewModel.agregarFaltantes(comparacionActual.faltan) },
                            onEliminarSobrante = { viewModel.eliminarSobrante(it) },
                            onCerrar = { viewModel.limpiarComparacion() },
                            modifier = modifier
                        )
                    } else {
                        when (pantalla) {
                            Pantalla.INICIO -> HomeScreen(
                                balanceNequi = balanceNequi,
                                balanceBancolombia = balanceBancolombia,
                                balanceNu = balanceNu,
                                balanceEfectivo = balanceEfectivo,
                                transaccionesRecientes = transacciones,
                                accesoNotificacionesActivo = notiActivo,
                                onAbrirAjustesNotificaciones = {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                                onClickTransaction = {
                                    transaccionEnEdicion = it
                                    dialogoAbierto = true
                                },
                                modifier = modifier
                            )
                            Pantalla.HISTORIAL -> HistorialScreen(
                                transacciones = transacciones,
                                onClickTransaction = {
                                    transaccionEnEdicion = it
                                    dialogoAbierto = true
                                },
                                onImportarExtracto = {
                                    selectorArchivo.launch(
                                        arrayOf(
                                            "application/pdf", "text/*", "text/csv",
                                            "text/comma-separated-values", "*/*"
                                        )
                                    )
                                },
                                modifier = modifier
                            )
                            Pantalla.PENDIENTES -> PendientesScreen(
                                pendientes = pendientes,
                                automaticas = automaticas,
                                onClickTransaction = {
                                    transaccionEnEdicion = it
                                    dialogoAbierto = true
                                },
                                modifier = modifier
                            )
                            Pantalla.RESUMEN -> DashboardScreen(
                                transacciones = transacciones,
                                onClickTransaction = {
                                    transaccionEnEdicion = it
                                    dialogoAbierto = true
                                },
                                modifier = modifier
                            )
                        }
                    }
                }

                if (urisPendientesPassword.isNotEmpty()) {
                    val cantidad = urisPendientesPassword.size
                    AlertDialog(
                        onDismissRequest = { urisPendientesPassword = emptyList(); passwordIngresada = "" },
                        title = { Text(if (cantidad > 1) "Estos PDF tienen contraseña" else "Este PDF tiene contraseña") },
                        text = {
                            Column {
                                if (cantidad > 1) {
                                    Text(
                                        "Se va a usar la misma contraseña para los $cantidad archivos.",
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                OutlinedTextField(
                                    value = passwordIngresada,
                                    onValueChange = { passwordIngresada = it },
                                    label = { Text("Contraseña") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { procesarArchivos(urisPendientesPassword, passwordIngresada) }) {
                                Text("Abrir")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { urisPendientesPassword = emptyList(); passwordIngresada = "" }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                if (errorLectura != null) {
                    AlertDialog(
                        onDismissRequest = { errorLectura = null },
                        title = { Text("No se pudo leer el archivo") },
                        text = { Text(errorLectura ?: "") },
                        confirmButton = {
                            TextButton(onClick = { errorLectura = null }) {
                                Text("Listo")
                            }
                        }
                    )
                }

                if (dialogoAbierto) {
                    AddTransactionDialog(
                        transaccionExistente = transaccionEnEdicion,
                        onDismiss = { dialogoAbierto = false },
                        onGuardar = { transaccion ->
                            if (transaccionEnEdicion == null) {
                                viewModel.agregar(transaccion)
                            } else {
                                viewModel.actualizar(transaccion)
                            }
                            dialogoAbierto = false
                        },
                        onEliminar = { transaccion ->
                            viewModel.eliminar(transaccion)
                            dialogoAbierto = false
                        }
                    )
                }
            }
        }
    }
}
