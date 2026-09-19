package app.cicada

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import app.cicada.ui.navigation.CicadaNavigation
import app.cicada.ui.theme.CicadaTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent{
            CicadaTheme {
                CicadaNavigation()
            }
        }
    }
}