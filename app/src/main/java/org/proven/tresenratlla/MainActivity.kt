package org.proven.tresenratlla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// @AA --- 1. Model (Lògica de Negoci i Dades) ---

data class CellModel(
    val id: Int,
    val position: Int,
    val symbol: String = ""
) {
    fun placeSymbol(newSymbol: String): CellModel = copy(symbol = newSymbol)
}

data class BoardModel(
    val cells: List<CellModel> = emptyList(),
    val currentTurn: String = "X",
    val winner: String? = null,
    val isDraw: Boolean = false,
    val isGameOver: Boolean = false
) {
    companion object {
        fun createInitialBoard(): BoardModel {
            val initialCells = List(9) { index ->
                CellModel(id = index, position = index, symbol = "")
            }
            return BoardModel(cells = initialCells)
        }
    }

    fun makeMove(position: Int): BoardModel {
        if (isGameOver || cells[position].symbol.isNotEmpty()) return this

        val updatedCells = cells.map { cell ->
            if (cell.position == position) cell.placeSymbol(currentTurn) else cell
        }

        val tempBoard = this.copy(cells = updatedCells)
        val detectedWinner = tempBoard.checkWinner()
        val drawState = detectedWinner == null && updatedCells.all { it.symbol.isNotEmpty() }
        val gameOverState = detectedWinner != null || drawState
        val nextTurn = if (gameOverState) currentTurn else if (currentTurn == "X") "O" else "X"

        return this.copy(
            cells = updatedCells,
            currentTurn = nextTurn,
            winner = detectedWinner,
            isDraw = drawState,
            isGameOver = gameOverState
        )
    }

    fun checkWinner(): String? {
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (pattern in winPatterns) {
            val (a, b, c) = pattern
            val symA = cells[a].symbol
            if (symA.isNotEmpty() && symA == cells[b].symbol && symA == cells[c].symbol) return symA
        }
        return null
    }
}

// @AA --- 2. ViewModel (Gestió de l'Estat de la UI) ---

class GameViewModel : ViewModel() {
    // @AA MutableStateFlow privado para guardar y modificar el estado reactivo del tablero internamente
    private val _board = MutableStateFlow(BoardModel.createInitialBoard())

    // @AA asStateFlow() convierte el flujo mutable en un StateFlow de solo lectura para que la vista no pueda alterarlo directamente
    val board: StateFlow<BoardModel> = _board.asStateFlow()

    fun onCellClick(position: Int) {
        _board.value = _board.value.makeMove(position)
    }

    fun restartGame() {
        _board.value = BoardModel.createInitialBoard()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }
}