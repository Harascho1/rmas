package rs.coffeeconquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.ui.CoffeeConquestApp
import rs.coffeeconquest.app.ui.theme.CoffeeConquestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(this)
        enableEdgeToEdge()
        setContent {
            CoffeeConquestTheme {
                CoffeeConquestApp()
            }
        }
    }
}
