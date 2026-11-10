package org.proven.tresenratlla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

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
        if (isGameOver || cells[position].symbol.isNotEmpty()) {
            return this
        }

        val updatedCells = cells.map { cell ->
            if (cell.position == position) {
                cell.placeSymbol(currentTurn)
            } else {
                cell
            }
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

    // @AA Metodo que revisa todas las combinaciones ganadoras (filas, columnas y diagonales)
    fun checkWinner(): String? {
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )

        for (pattern in winPatterns) {
            val (a, b, c) = pattern
            val symA = cells[a].symbol
            if (symA.isNotEmpty() && symA == cells[b].symbol && symA == cells[c].symbol) {
                return symA
            }
        }
        return null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }
}