package com.example.billkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.room.Room


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "billkeeper.db").build()
        val repo = LedgerRepository(db)
        val vm = LedgerViewModel(repo)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1B5E20),
                    secondary = Color(0xFF2E7D32),
                    surface = Color(0xFFF5F5F5),
                    background = Color(0xFFFAFAFA)
                )
            ) {
                BillKeeperApp(vm)
            }
        }
    }
}
