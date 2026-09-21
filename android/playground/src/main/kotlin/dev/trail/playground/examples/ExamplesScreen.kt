package dev.trail.playground.examples

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.trail.core.TrailColor

private val examples = linkedMapOf(
    "basic" to "Named preset", "runtime-color" to "Runtime color", "plugin" to "Custom plugin",
    "playback" to "Playback controls", "sequence" to "Sequence", "layers" to "Layers",
    "google-maps" to "Google Maps", "android-view" to "Android View",
)

@Composable fun ExamplesScreen(mapsEnabled: Boolean, onBack: () -> Unit) {
    var selected by remember { mutableStateOf("basic") }
    var expanded by remember { mutableStateOf(false) }
    var coral by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val code = remember(selected) { context.assets.open("examples/$selected.txt").bufferedReader().use { it.readText() } }
    val brandColor = if (coral) TrailColor.Coral else TrailColor.Blue
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF32D6AD), onPrimary = Color(0xFF0C1921))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF0C1921), contentColor = MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.systemBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("examplesBack")) { Text("← Trail Studio") }
                Text("Learn by running it.", fontSize = 26.sp)
                Text("These examples compile with the app.", fontSize = 13.sp, color = Color(0xFF93A9B4))
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("examplePicker")) { Text(examples.getValue(selected)) }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        examples.forEach { (id, title) ->
                            DropdownMenuItem(text = { Text(title) }, onClick = { selected = id; expanded = false })
                        }
                    }
                }
                if (selected == "runtime-color" || selected == "plugin") {
                    OutlinedButton(onClick = { coral = !coral }, modifier = Modifier.testTag("changeBrandColor")) { Text("Change brand color") }
                }
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF162C37), modifier = Modifier.fillMaxWidth().height(260.dp).testTag("examplePreview")) {
                    key(selected) {
                        when (selected) {
                            "basic" -> BasicRouteExample()
                            "runtime-color" -> RuntimeColorExample(brandColor)
                            "plugin" -> PluginExample(brandColor)
                            "playback" -> PlaybackExample()
                            "sequence" -> SequenceExample()
                            "layers" -> LayersExample()
                            "google-maps" -> if (mapsEnabled) GoogleMapsExample() else Text("Add MAPS_API_KEY to local.properties, then rebuild to run this example.", Modifier.padding(20.dp))
                            "android-view" -> AndroidViewExample()
                        }
                    }
                }
                SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
                    Text(code, Modifier.fillMaxSize().verticalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
