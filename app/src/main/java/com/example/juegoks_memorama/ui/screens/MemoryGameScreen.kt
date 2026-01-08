package com.example.juegoks_memorama.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameHistoryItem
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.model.SaveFormat
import com.example.juegoks_memorama.model.VerbsData
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

@Composable
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val sdf = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }
    return sdf.format(Date(timestamp))
}

@Composable
fun MemoryGameScreen(
    gameMode: GameMode,
    initialDifficulty: Difficulty,
    onExitGame: () -> Unit,
    viewModel: MemoryGameViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val uiState by viewModel.gameUiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialDifficulty) {
        viewModel.setDifficulty(initialDifficulty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(8.dp), // Reducimos padding general para ganar espacio
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABECERA (Header) ---
        // La hacemos más compacta para dejar espacio al tablero
        if (gameMode == GameMode.BLUETOOTH) {
            if (gameState.cards.isNotEmpty()) {
                MultiplayerHeaderCompact(gameState)
            } else {
                Text("Conectando...", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            SinglePlayerHeaderCompact(
                gameState = gameState,
                onNewGame = { viewModel.startNewGame() },
                onExitGame = { viewModel.onExitGame(); onExitGame() },
                onSaveClick = { viewModel.onSaveClick() },
                onShowHistoryDialog = { viewModel.showHistoryDialog(true) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- TABLERO RESPONSIVO (SIN SCROLL) ---
        if (gameMode == GameMode.BLUETOOTH && gameState.cards.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (gameState.cards.isNotEmpty()) {
            // Usamos BoxWithConstraints para calcular el tamaño exacto de cada carta
            // y que quepan todas en la pantalla sin hacer scroll
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f) // Ocupa todo el espacio restante vertical
                    .fillMaxWidth()
                    .alpha(if (gameMode == GameMode.BLUETOOTH && !gameState.isMyTurn) 0.6f else 1f),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight

                // Calculamos filas necesarias basado en columnas de la dificultad
                val columns = gameState.difficulty.columns
                val totalCards = gameState.cards.size
                val rows = (totalCards + columns - 1) / columns

                // Espacio entre cartas (gap)
                val gap = 4.dp
                val gapPx = 4f // Aproximado para cálculo lógico, Compose usa Dp

                // Tamaño de celda ideal (cuadrada o rectangular según quepa mejor)
                // Restamos el espacio de los gaps
                val cardWidth = (availableWidth - (gap * (columns - 1))) / columns
                val cardHeight = (availableHeight - (gap * (rows - 1))) / rows

                // Usamos el menor tamaño para mantener proporciones decentes,
                // pero permitimos que se estire un poco verticalmente si hace falta.
                // O mejor: forzamos que ocupen el espacio disponible.

                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    for (row in 0 until rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            modifier = Modifier.weight(1f) // Cada fila ocupa el mismo alto
                        ) {
                            for (col in 0 until columns) {
                                val index = row * columns + col
                                if (index < totalCards) {
                                    val card = gameState.cards[index]
                                    Box(modifier = Modifier.weight(1f)) { // Cada carta ocupa el mismo ancho
                                        MemoryCard(
                                            card = card,
                                            onClick = { viewModel.onCardClick(card) }
                                        )
                                    }
                                } else {
                                    // Espacio vacío si la última fila no está completa
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // --- DIÁLOGOS ---
        // (Sin cambios en la lógica de diálogos)
        if (gameState.gameCompleted) {
            GameCompletedDialog(
                moves = gameState.moves,
                myScore = gameState.score,
                opponentScore = gameState.opponentScore,
                elapsedTime = gameState.elapsedTimeInSeconds,
                isMultiplayer = gameMode == GameMode.BLUETOOTH,
                onPlayAgain = {
                    if (gameMode == GameMode.SINGLE_PLAYER) viewModel.startNewGame()
                    else onExitGame()
                },
                onSaveResult = { viewModel.onSaveClick() },
                onExit = { viewModel.onExitGame(); onExitGame() }
            )
        }
        if (uiState.showSaveDialog) {
            SaveGameDialog(uiState.existingSaveNames, { f, fmt -> scope.launch { viewModel.saveGame(f, fmt) } }, { viewModel.showSaveDialog(false) })
        }
        if (uiState.showHistoryDialog) {
            HistoryDialog(uiState.historyItems, { f, fmt -> scope.launch { viewModel.loadGameFromHistory(f, fmt) } }, { viewModel.showHistoryDialog(false) })
        }
        if (uiState.showPostSaveDialog) {
            PostSaveDialog({ viewModel.dismissPostSaveDialog() }, { viewModel.dismissPostSaveDialog(); viewModel.startNewGame() }, { viewModel.dismissPostSaveDialog(); viewModel.onExitGame(); onExitGame() })
        }
        if (uiState.showPostWinSaveDialog) {
            PostWinSaveDialog({ viewModel.dismissPostWinSaveDialog(); viewModel.startNewGame() }, { viewModel.dismissPostWinSaveDialog(); viewModel.onExitGame(); onExitGame() })
        }
    }
}

// --- HEADERS COMPACTOS ---
@Composable
fun MultiplayerHeaderCompact(gameState: com.example.juegoks_memorama.model.GameState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (gameState.isMyTurn) Color(0xFF4CAF50) else Color(0xFFE57373)),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (gameState.isMyTurn) "TU TURNO" else "RIVAL", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Tú: ${gameState.score} | Rival: ${gameState.opponentScore}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text("⏱ ${formatTime(gameState.elapsedTimeInSeconds)}", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SinglePlayerHeaderCompact(
    gameState: com.example.juegoks_memorama.model.GameState,
    onNewGame: () -> Unit,
    onExitGame: () -> Unit,
    onSaveClick: () -> Unit,
    onShowHistoryDialog: () -> Unit
) {
    val view = LocalView.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kolping Verbs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("⏱ ${formatTime(gameState.elapsedTimeInSeconds)}", style = MaterialTheme.typography.titleMedium)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
            StatItemCompact("🏆 Puntos", "${gameState.score}")
            StatItemCompact("🔄 Movs", "${gameState.moves}")
            StatItemCompact("🃏 Pares", "${gameState.matchedPairs}/${gameState.difficulty.pairs}")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SmallButton(text = "🔄 Nuevo", onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onNewGame() })
            SmallButton(text = "💾 Guardar", onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onSaveClick() })
            SmallButton(text = "📜 Historial", onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onShowHistoryDialog() })
            SmallButton(text = "🚪 Salir", onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onExitGame() })
        }
    }
}

@Composable
fun StatItemCompact(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

// --- CARTA ADAPTABLE ---
@Composable
fun MemoryCard(card: com.example.juegoks_memorama.model.Card, onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    var isFaceUp by remember { mutableStateOf(card.isFaceUp) }

    LaunchedEffect(card.isFaceUp) {
        if (card.isFaceUp != isFaceUp) {
            if (card.isFaceUp) rotation.animateTo(180f, animationSpec = tween(400))
            else rotation.animateTo(0f, animationSpec = tween(400))
            isFaceUp = card.isFaceUp
        }
    }

    val animateScale by animateFloatAsState(targetValue = if (card.isMatched) 0.0f else 1f, label = "scale")

    // En lugar de AspectRatio fijo, llenamos el Box padre (que ya tiene pesos distribuidos)
    Box(modifier = Modifier.fillMaxSize().scale(animateScale)) {
        Card(
            modifier = Modifier.fillMaxSize().clickable(enabled = !card.isMatched && !card.isFaceUp, onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Menos elevación para ahorrar espacio visual
            colors = CardDefaults.cardColors(containerColor = if (rotation.value > 90f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary)
        ) {
            if (rotation.value > 90f) {
                // --- CARA FRONTAL (VERBO) ---
                Box(modifier = Modifier.fillMaxSize().padding(2.dp), contentAlignment = Alignment.Center) {
                    val verbText = VerbsData.getText(card.value)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            text = VerbsData.getLanguageCode(card.value),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified, // Auto
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.scale(0.8f) // Un poco más pequeño
                        )
                        // Texto auto-ajustable simple
                        Text(
                            text = verbText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            } else {
                // --- REVERSO ---
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.QuestionMark, contentDescription = null, modifier = Modifier.fillMaxSize(0.5f))
                }
            }
        }
    }
}

// --- DIALOGOS (Solo copiados para mantener funcionalidad) ---
@Composable
fun GameCompletedDialog(moves: Int, myScore: Int, opponentScore: Int, elapsedTime: Long, isMultiplayer: Boolean, onPlayAgain: () -> Unit, onSaveResult: () -> Unit, onExit: () -> Unit) {
    val view = LocalView.current
    val won = myScore > opponentScore
    val tie = myScore == opponentScore
    AlertDialog(
        onDismissRequest = { if (!isMultiplayer) onPlayAgain() else onExit() },
        title = { Text(if (isMultiplayer) (if (tie) "¡Empate!" else if (won) "¡GANASTE!" else "Perdiste...") else "¡Felicidades!") },
        text = { Column { if(isMultiplayer) { Text("Tú: $myScore\nRival: $opponentScore") } else { Text("Tiempo: ${formatTime(elapsedTime)}\nMovimientos: $moves\nPuntos: $myScore") } } },
        confirmButton = { Column(horizontalAlignment = Alignment.End) { if (!isMultiplayer) { Button(onClick = onSaveResult) { Text("Guardar") }; Button(onClick = onPlayAgain) { Text("Jugar de nuevo") } } ; Button(onClick = onExit) { Text("Salir") } } }
    )
}
@Composable fun SaveGameDialog(existing: List<String>, onSave: (String, SaveFormat) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var fmt by remember { mutableStateOf(SaveFormat.JSON) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Guardar") }, text = { Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }); Row { SaveFormat.entries.forEach { f -> Text(f.name, Modifier.clickable { fmt = f }.padding(8.dp), color = if(fmt==f) MaterialTheme.colorScheme.primary else Color.Gray) } } } }, confirmButton = { Button(onClick = { onSave(name, fmt) }) { Text("Guardar") } })
}
@Composable fun HistoryDialog(items: List<GameHistoryItem>, onLoad: (String, SaveFormat) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Historial") }, text = { LazyColumn { items(items) { i -> Text("${i.filename} (${i.state.score} pts)", Modifier.clickable { onLoad(i.filename, i.format) }.padding(8.dp)) } } }, confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } })
}
@Composable fun PostSaveDialog(onContinue: () -> Unit, onNewGame: () -> Unit, onExit: () -> Unit) { AlertDialog(onDismissRequest = onContinue, title = { Text("Guardado") }, text = { Text("¿Qué sigue?") }, confirmButton = { Column { Button(onClick = onContinue){Text("Seguir")}; Button(onClick = onNewGame){Text("Nuevo")}; Button(onClick = onExit){Text("Salir")} } }) }
@Composable fun PostWinSaveDialog(onNewGame: () -> Unit, onExit: () -> Unit) { AlertDialog(onDismissRequest = onNewGame, title = { Text("Guardado") }, text = { Text("¿Qué sigue?") }, confirmButton = { Column { Button(onClick = onNewGame){Text("Nuevo")}; Button(onClick = onExit){Text("Salir")} } }) }