package jk.ut61eTool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import jk.ut61eTool.ui.navigation.AppNavHost
import jk.ut61eTool.ui.theme.UT61eToolkitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UT61eToolkitTheme {
                AppNavHost()
            }
        }
    }
}
