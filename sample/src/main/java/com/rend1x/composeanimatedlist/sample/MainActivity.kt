package com.rend1x.composeanimatedlist.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val benchmarkScenario = intent.toBenchmarkScenarioOrNull()
        setContent {
            SampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (benchmarkScenario == null) {
                        SampleListScreen()
                    } else {
                        BenchmarkScenarioScreen(benchmarkScenario)
                    }
                }
            }
        }
    }
}
