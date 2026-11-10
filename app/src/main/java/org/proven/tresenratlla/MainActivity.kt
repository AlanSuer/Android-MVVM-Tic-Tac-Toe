package org.proven.tresenratlla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// @AA --- 1. Model (Lògica de Negoci i Dades) ---

// @AA Data class que representa cada casilla individual del tablero (id, posicion y simbolo "X", "O" o vacio)
data class CellModel(
    val id: Int,
    val position: Int,
    val symbol: String = ""
) {
    // @AA Funcion que devuelve una copia inmutable de la casilla con el nuevo simbolo usando .copy()
    fun placeSymbol(newSymbol: String): CellModel = copy(symbol = newSymbol)
}

// @AA Data class que representa el estado completo del tablero y la partida
data class BoardModel(
    val cells: List<CellModel> = emptyList(),
    val currentTurn: String = "X",
    val winner: String? = null,
    val isDraw: Boolean = false,
    val isGameOver: Boolean = false
) {
    companion object {
        // @AA Metodo de fabrica estatico para inicializar el tablero de 0 con las 9 casillas vacias
        fun createInitialBoard(): BoardModel {
            val initialCells = List(9) { index ->
                CellModel(
                    id = index,
                    position = index,
                    symbol = ""
                )
            }
            return BoardModel(cells = initialCells)
        }
    }

    // @AA Logica pura para ejecutar un movimiento en una posicion y devolver un nuevo BoardModel con .copy()
    fun makeMove(position: Int): BoardModel {
        // @AA Si el juego ha terminado o la casilla ya tiene un simbolo, devolvemos el mismo estado sin cambios
        if (isGameOver || cells[position].symbol.isNotEmpty()) {
            return this
        }

        // @AA Creamos una nueva lista actualizando solo la casilla pulsada con el simbolo del turno actual
        val updatedCells = cells.map { cell ->
            if (cell.position == position) {
                cell.placeSymbol(currentTurn)
            } else {
                cell
            }
        }

        // @AA Creamos un tablero temporal con las casillas actualizadas para comprobar si hay ganador
        val tempBoard = this.copy(cells = updatedCells)
        val detectedWinner = tempBoard.checkWinner()

        // @AA Comprobamos si hay empate (no hay ganador y todas las casillas estan llenas)
        val drawState = detectedWinner == null && updatedCells.all { it.symbol.isNotEmpty() }
        val gameOverState = detectedWinner != null || drawState

        // @AA Cambiamos el turno entre X y O solo si la partida sigue en curso
        val nextTurn = if (gameOverState) currentTurn else if (currentTurn == "X") "O" else "X"

        // @AA Retornamos la nueva instancia inmutable del modelo con todos los datos actualizados
        return this.copy(
            cells = updatedCells,
            currentTurn = nextTurn,
            winner = detectedWinner,
            isDraw = drawState,
            isGameOver = gameOverState
        )
    }

    // @AA Metodo que revisa todas las combinaciones ganadoras (filas, columnas y diagonales)
    fun checkWinner(): String? {
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // @AA Filas horizontales
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // @AA Columnas verticales
            listOf(0, 4, 8), listOf(2, 4, 6)                  // @AA Diagonales
        )

        for (pattern in winPatterns) {
            val (a, b, c) = pattern
            val symA = cells[a].symbol
            if (symA.isNotEmpty() && symA == cells[b].symbol && symA == cells[c].symbol) {
                return symA // @AA Devuelve "X" o "O" si coinciden las tres posiciones
            }
        }
        return null // @AA Si nadie ha ganado todavia, devuelve null
    }
}

// @AA --- 2. ViewModel (Gestió de l'Estat de la UI) ---

class GameViewModel : ViewModel() {
    // @AA MutableStateFlow privado para guardar y modificar el estado reactivo del tablero internamente
    private val _board = MutableStateFlow(BoardModel.createInitialBoard())

    // @AA asStateFlow() convierte el flujo mutable en un StateFlow de solo lectura para que la vista no pueda alterarlo directamente
    val board: StateFlow<BoardModel> = _board.asStateFlow()

    // @AA Evento llamado desde la interfaz cuando el usuario pulsa una casilla
    fun onCellClick(position: Int) {
        _board.value = _board.value.makeMove(position)
    }

    // @AA Evento para reiniciar la partida restaurando el tablero inicial
    fun restartGame() {
        _board.value = BoardModel.createInitialBoard()
    }
}

// @AA --- View (Interfície d'Usuari amb Jetpack Compose) ---

class MainActivity : ComponentActivity() {
    // @AA Delegado by viewModels() que instancia el GameViewModel y lo mantiene vivo aunque gire la pantalla
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // @AA Usamos MaterialTheme directamente para aplicar estilos Material3 sin depender de archivos de tema externos
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// @AA Pantalla principal del juego que observa el estado y organiza los elementos visuales
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // @AA collectAsState() escucha el StateFlow del ViewModel y fuerza la recomposicion de la UI cada vez que el tablero cambia
    val board by viewModel.board.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tres en Ratlla - Alan Aparicio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // @AA Indicador visual dinamico: muestra victoria, empate o el turno actual
        val statusText = when {
            board.winner != null -> " Victoria per al jugador ${board.winner}!"
            board.isDraw -> "Empat! Ningú guanya."
            else -> "Torn del jugador: ${board.currentTurn}"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            color = if (board.isGameOver) MaterialTheme.colorScheme.primary else Color.Unspecified,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // @AA Componente que dibuja la cuadricula de 3x3
        BoardGrid(
            cells = board.cells,
            isGameOver = board.isGameOver,
            onCellClick = { position -> viewModel.onCellClick(position) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // @AA Boton para reiniciar la partida llamando al metodo del ViewModel
        Button(onClick = { viewModel.restartGame() }) {
            Text(
                text = if (board.isGameOver) "Jugar de nou" else "Reiniciar partida",
                fontSize = 16.sp
            )
        }
    }
}

// @AA Composable encargado de renderizar el tablero de 3x3 usando LazyVerticalGrid
@Composable
fun BoardGrid(
    cells: List<CellModel>,
    isGameOver: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // @AA LazyVerticalGrid con GridCells.Fixed(3) monta una cuadricula de 3 columnas exactas optimizada para Compose
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.size(330.dp)
    ) {
        // @AA items() recorre la lista de casillas usando el id como clave unica para optimizar el repintado
        items(cells, key = { it.id }) { cell ->
            CellItemView(
                cell = cell,
                enabled = !isGameOver && cell.symbol.isEmpty(),
                onCellClick = { onCellClick(cell.position) }
            )
        }
    }
}

// @AA Composable que representa visualmente cada casilla individual y detecta los clics
@Composable
fun CellItemView(
    cell: CellModel,
    enabled: Boolean,
    onCellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // @AA Card con modificador .clickable() que solo permite pulsar si la casilla esta vacia y la partida activa
    Card(
        modifier = modifier
            .size(100.dp)
            .clickable(enabled = enabled) { onCellClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cell.symbol,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (cell.symbol == "X") Color(0xFF1976D2) else Color(0xFFD32F2F)
            )
        }
    }
}