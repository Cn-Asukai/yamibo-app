package me.thenano.yamibo.yamibo_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import me.thenano.yamibo.yamibo_app.components.feedback.YamiboErrorContent
import platform.Foundation.NSProcessInfo

@Composable
fun Issue88ValidationScreen() {
    val environment = NSProcessInfo.processInfo.environment
    val scenario = environment["YAMIBO_QA_CASE"]?.toString() ?: "network"
    val scale = environment["YAMIBO_QA_SCALE"]?.toString()?.toFloatOrNull() ?: 1f
    val height = environment["YAMIBO_QA_HEIGHT"]?.toString()?.toIntOrNull()
    val message = remember(scenario) {
        when (scenario) {
            "business" -> "論壇維護中，請稍後重試。"
            "long-business" -> "維護通知：" + "伺服器維護中，請稍後重試。".repeat(200)
            else -> "[Network] 網路錯誤\nurl: https://user:secret@bbs.yamibo.com/forum.php?token=fixture-secret\n" +
                "error: Error Domain=NSURLErrorDomain Code=-1009\n" +
                "Cookie: sid=fixture-cookie\nAuthorization: Bearer fixture-bearer\n" +
                "UserInfo=network path unsatisfied, interface en0, dns\n".repeat(200)
        }
    }
    var retries by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
        Column(Modifier.fillMaxSize()) {
            Box(if (height != null) Modifier.height(height.dp) else Modifier.weight(1f)) {
                YamiboErrorContent(message = message, onRetry = { retries++ })
            }
            Text("QA retries=$retries", modifier = Modifier.padding(8.dp))
        }
    }
}
