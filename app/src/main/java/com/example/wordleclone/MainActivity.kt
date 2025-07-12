package com.example.wordleclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.wordleclone.ui.theme.WordleCloneTheme

// Define LetterStatus outside or as a nested class if preferred
enum class LetterStatus {
    CORRECT, // Green
    PRESENT, // Yellow
    ABSENT,  // Gray
    UNKNOWN  // Default key color
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WordleCloneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Or your specific background like Color(0xFF13141C)
                ) {
                    WordleGame()
                }
            }
        }
    }
}

@Composable
fun WordleGame() {
    val context = LocalContext.current

    val wordList by remember {
        mutableStateOf(
            context.assets.open("words.txt")
                .bufferedReader()
                .readLines()
                .map { it.trim().uppercase() }
                .filter { it.length == 5 }
        )
    }

    val targetWordState = remember { mutableStateOf(wordList.random()) }
    val targetWord = targetWordState.value // Using property delegate for easier access

    var letterStatuses by remember { mutableStateOf<Map<Char, LetterStatus>>(emptyMap()) }

    var currentGuess by remember { mutableStateOf("") }
    var guesses by remember { mutableStateOf<List<String>>(emptyList()) }
    var gameOver by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun updateLetterStatuses(guess: String, currentTargetWord: String) {
        val newStatuses = letterStatuses.toMutableMap()
        guess.forEachIndexed { index, char ->
            val currentStatusInMap = newStatuses[char] // Status from previous guesses for this char
            val newStatusForCharInGuess = when {
                currentTargetWord[index] == char -> LetterStatus.CORRECT
                char in currentTargetWord -> LetterStatus.PRESENT
                else -> LetterStatus.ABSENT
            }

            // Update only if the new status is "better" (Correct > Present > Absent)
            // or if the letter hasn't been seen before or was UNKNOWN.
            if (currentStatusInMap == null || currentStatusInMap == LetterStatus.UNKNOWN ||
                (newStatusForCharInGuess == LetterStatus.CORRECT) ||
                (newStatusForCharInGuess == LetterStatus.PRESENT && currentStatusInMap != LetterStatus.CORRECT) ||
                (newStatusForCharInGuess == LetterStatus.ABSENT && currentStatusInMap == LetterStatus.UNKNOWN)
            ) {
                newStatuses[char] = newStatusForCharInGuess
            }
        }
        letterStatuses = newStatuses
    }

    fun resetGame() {
        currentGuess = ""
        guesses = emptyList()
        gameOver = false
        gameWon = false
        message = ""
        targetWordState.value = wordList.random()
        letterStatuses = emptyMap() // Reset letter statuses
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13141C))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "WORDLE",
            color = Color(0xFFF5F5DC),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 80.dp, bottom = 20.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (i in 0 until 6) {
                when {
                    i < guesses.size -> {
                        GuessRow(guess = guesses[i], targetWord = targetWord)
                    }
                    i == guesses.size && !gameOver && !gameWon -> {
                        GuessRow(guess = currentGuess.padEnd(5, ' '), targetWord = "") // Active input row
                    }
                    else -> {
                        GuessRow(guess = "     ", targetWord = "") // Empty placeholder row
                    }
                }
                if (i < 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Text(
            text = message,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = if (gameWon) Color(0xFF006400) else Color(0xFF910E04), // Dark Green for win, Dark Red for loss/error
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Keyboard Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                "QWERTYUIOP".forEach { char ->
                    KeyButton(
                        char = char,
                        onClick = { if (currentGuess.length < 5 && !gameOver && !gameWon) currentGuess += char },
                        letterStatus = letterStatuses[char] ?: LetterStatus.UNKNOWN,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Keyboard Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(0.5f)) // For centering the 'ASDFGHJKL' row
                "ASDFGHJKL".forEach { char ->
                    KeyButton(
                        char = char,
                        onClick = { if (currentGuess.length < 5 && !gameOver && !gameWon) currentGuess += char },
                        letterStatus = letterStatuses[char] ?: LetterStatus.UNKNOWN,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }
            // Keyboard Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ActionKeyButton(
                    text = "DEL",
                    onClick = {
                        if (currentGuess.isNotEmpty() && !gameOver && !gameWon) {
                            currentGuess = currentGuess.dropLast(1)
                        }
                    },
                    containerColor = Color(0xFFC71002), // Dark Red
                    modifier = Modifier.weight(1.5f)
                )
                "ZXCVBNM".forEach { char ->
                    KeyButton(
                        char = char,
                        onClick = { if (currentGuess.length < 5 && !gameOver && !gameWon) currentGuess += char },
                        letterStatus = letterStatuses[char] ?: LetterStatus.UNKNOWN,
                        modifier = Modifier.weight(1f)
                    )
                }
                ActionKeyButton(
                    text = "ENTER",
                    onClick = {
                        if (currentGuess.length == 5 && !gameOver && !gameWon) {
                            if (currentGuess in wordList) {
                                guesses = guesses + currentGuess
                                updateLetterStatuses(currentGuess, targetWord) // Update keyboard colors
                                if (currentGuess == targetWord) {
                                    gameWon = true
                                    gameOver = true // Game also ends on win
                                    message = "You won! The word was $targetWord"
                                } else if (guesses.size == 6) {
                                    gameOver = true
                                    message = "Game over! The word was $targetWord"
                                } else {
                                    message = "" // Clear "Not in word list" message
                                }
                                currentGuess = ""
                            } else {
                                message = "NOT A WORD!"
                            }
                        } else if (currentGuess.length < 5 && !gameOver && !gameWon) {
                            message = "Not enough letters"
                        }
                    },
                    containerColor = Color(0xFF006400), // Dark Green
                    modifier = Modifier.weight(1.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { resetGame() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF56007A), // Purple
                contentColor = Color(0xFFF5F5DC)
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("New Game",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
                )
        }
    }
}

@Composable
fun GuessRow(guess: String, targetWord: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 0 until 5) {
            val char = guess.getOrElse(i) { ' ' }.uppercaseChar()
            val isCurrentInputRow = targetWord.isEmpty()

            val backgroundColor = when {
                isCurrentInputRow && char == ' ' -> Color.Transparent // Empty cell in current input row
                isCurrentInputRow -> Color.Transparent // Cell with letter in current input row (no background color until submitted)
                char == targetWord.getOrNull(i) -> Color(0xFF006400) // Dark Green (Correct)
                targetWord.contains(char) -> Color(0xFFA89200)       // Dark Yellow/Gold (Present)
                else -> Color.DarkGray                            // Absent
            }

            val textColor = if (isCurrentInputRow) {
                Color(0xFFF5F5DC) // Beige for current input text
            } else {
                Color.White       // White for submitted guess text (contrasts with green/yellow/gray)
            }

            val borderColor = if (isCurrentInputRow && char != ' ') Color.LightGray else Color.DarkGray


            Box(
                modifier = Modifier
                    .size(56.dp) // Slightly larger boxes
                    .padding(4.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .background(backgroundColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char.toString(),
                    fontSize = 28.sp, // Slightly larger text
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
fun KeyButton(
    char: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    letterStatus: LetterStatus = LetterStatus.UNKNOWN
) {
    val containerColor = when (letterStatus) {
        LetterStatus.CORRECT -> Color(0xFF006400) // Dark Green
        LetterStatus.PRESENT -> Color(0xFFA89200) // Dark Yellow/Gold
        LetterStatus.ABSENT  -> Color(0xFF4A4A4A) // A slightly lighter dark gray for used keys
        LetterStatus.UNKNOWN -> Color(0xFF2C2C2C) // Darker gray for default keys
    }
    val contentColor = Color(0xFFF5F5DC) // Beige text for keys

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 3.dp) // Adjusted padding
            .height(50.dp)
            .clip(RoundedCornerShape(6.dp)) // Slightly more rounded
            .background(containerColor)
            .clickable { onClick() }
    ) {
        Text(
            text = char.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
fun ActionKeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer, // Use your specific colors
    contentColor: Color = Color(0xFFF5F5DC) // Beige
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 3.dp) // Adjusted padding
            .height(50.dp)
            .clip(RoundedCornerShape(6.dp)) // Slightly more rounded
            .background(containerColor)
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF13141C) // Preview with dark background
@Composable
fun WordleGamePreview() {
    WordleCloneTheme {
        WordleGame()
    }
}