package app.cicada.ui.components


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.cicada.util.SiteIconUtil
import coil.compose.AsyncImage

@Composable
fun SiteIcon(
    title: String,
    website: String?,
    modifier: Modifier = Modifier
) {
    val faviIcon = SiteIconUtil.getFaviIcon(website)

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(48.dp)
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            if (faviIcon != null) {
                AsyncImage(
                    model = faviIcon,
                    contentDescription = "$title icon",
                    modifier = modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = title
                        .firstOrNull()
                        ?.uppercase()
                        ?: "?",

                    color = MaterialTheme.colorScheme.primary,

                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}