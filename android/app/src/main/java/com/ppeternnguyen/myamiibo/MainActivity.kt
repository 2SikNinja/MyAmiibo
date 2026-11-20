package com.ppeternnguyen.myamiibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ppeternnguyen.myamiibo.ui.MyAmiiboAppRoot
import com.ppeternnguyen.myamiibo.ui.theme.MyAmiiboTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAmiiboTheme {
                MyAmiiboAppRoot()
            }
        }
    }
}
