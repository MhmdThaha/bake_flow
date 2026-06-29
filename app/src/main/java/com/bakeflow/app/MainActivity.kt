package com.bakeflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bakeflow.app.navigation.BakeFlowNavHost
import com.bakeflow.app.ui.theme.BakeFlowTheme

/**
 * Single-activity host for the BakeFlow Compose UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BakeFlowTheme {
                BakeFlowNavHost(appContainer = (application as BakeFlowApplication).appContainer)
            }
        }
    }
}
