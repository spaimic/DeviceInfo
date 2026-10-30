package com.spaimic.deviceinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val tabs = listOf("Dashboard", "CPU")
    var tab by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<List<Row>>(emptyList()) }

    // Refresh every second so CPU frequencies and battery stay live.
    LaunchedEffect(tab) {
        while (true) {
            rows = if (tab == 0) InfoProvider.dashboard(ctx) else InfoProvider.cpu()
            delay(1000)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = {}, label = { Text(t) }, alwaysShowLabel = true)
                }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rows) { (k, v) ->
                Column {
                    Text(k, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(v, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
