@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.juegoks_memorama.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

// --- LISTA DE VERBOS ---
object VerbsData {
    val list = mapOf(
        1 to ("Run" to "Correr"),
        2 to ("Eat" to "Comer"),
        3 to ("Study" to "Estudiar"),
        4 to ("Work" to "Trabajar"),
        5 to ("Learn" to "Aprender"),
        6 to ("Speak" to "Hablar"),
        7 to ("Read" to "Leer"),
        8 to ("Write" to "Escribir"),
        9 to ("Play" to "Jugar"),
        10 to ("Sleep" to "Dormir"),
        11 to ("Walk" to "Caminar"),
        12 to ("Drink" to "Beber"),
        13 to ("Cook" to "Cocinar"),
        14 to ("Dance" to "Bailar"),
        15 to ("Sing" to "Cantar"),
        16 to ("Swim" to "Nadar"),
        17 to ("Travel" to "Viajar"),
        18 to ("Buy" to "Comprar"),
        19 to ("Sell" to "Vender"),
        20 to ("Open" to "Abrir"),
        21 to ("Close" to "Cerrar"),
        22 to ("Listen" to "Escuchar"),
        23 to ("Watch" to "Mirar"),
        24 to ("Wash" to "Lavar"),
        25 to ("Clean" to "Limpiar"),
        26 to ("Drive" to "Manejar"),
        27 to ("Fly" to "Volar"),
        28 to ("Jump" to "Saltar"),
        29 to ("Think" to "Pensar"),
        30 to ("Know" to "Saber")
    )

    fun getText(value: Int): String {
        val id = abs(value)
        val pair = list[id] ?: return "???"
        return if (value > 0) pair.first else pair.second
    }

    fun getLanguageCode(value: Int): String {
        return if (value > 0) "EN" else "ES"
    }
}

enum class AppThemeOption { IPN, ESCOM }
@Serializable data class Move(val card1Id: Int, val card2Id: Int)
@Serializable data class Card(val id: Int, val value: Int, var isFaceUp: Boolean = false, var isMatched: Boolean = false)
enum class Difficulty(val pairs: Int, val columns: Int) {
    EASY(6, 3), MEDIUM(12, 4), HARD(20, 5)
}
enum class GameMode { SINGLE_PLAYER, BLUETOOTH }
enum class SaveFormat { JSON, XML, TXT }
@Serializable data class GameState(val difficulty: Difficulty = Difficulty.MEDIUM, val cards: List<Card> = emptyList(), val moves: Int = 0, val matchedPairs: Int = 0, val score: Int = 0, val matchStreak: Int = 0, val moveHistory: List<Move> = emptyList(), val gameCompleted: Boolean = false, val elapsedTimeInSeconds: Long = 0, val isTimerRunning: Boolean = false, val isMultiplayer: Boolean = false, val isHost: Boolean = false, val isMyTurn: Boolean = true, val opponentScore: Int = 0, val myPairs: Int = 0, val opponentPairs: Int = 0)
data class GameHistoryItem(val filename: String, val format: SaveFormat, val state: GameState, val timestamp: Long)