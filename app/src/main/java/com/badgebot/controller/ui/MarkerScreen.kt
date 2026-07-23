package com.badgebot.controller.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.print.PrintHelper
import com.badgebot.controller.aruco.ArucoBitmap
import com.badgebot.controller.aruco.ArucoMarker
import com.badgebot.controller.util.Sharing

/**
 * Displays an ArUco (`DICT_4X4_50`) fiducial marker with controls to change the
 * id and to print or share it.
 */
@Composable
fun MarkerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var markerId by remember { mutableIntStateOf(0) }
    val bitmap = remember(markerId) { ArucoBitmap.render(markerId, sizePx = 720) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "ArUco marker (DICT_4X4_50)",
            style = MaterialTheme.typography.titleMedium,
        )

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "ArUco marker $markerId",
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledIconButton(
                onClick = { if (markerId > 0) markerId-- },
                enabled = markerId > 0,
            ) { Icon(Icons.Filled.Remove, contentDescription = "Previous marker") }

            Text("ID $markerId", style = MaterialTheme.typography.headlineSmall)

            FilledIconButton(
                onClick = { if (markerId < ArucoMarker.DICTIONARY_SIZE - 1) markerId++ },
                enabled = markerId < ArucoMarker.DICTIONARY_SIZE - 1,
            ) { Icon(Icons.Filled.Add, contentDescription = "Next marker") }
        }

        Text(
            "Print this marker and place it on the ground. The camera view will " +
                "use it as a reference point for path following.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.size(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val printBitmap = ArucoBitmap.render(markerId, sizePx = 2048)
                    PrintHelper(context).apply {
                        scaleMode = PrintHelper.SCALE_MODE_FIT
                        colorMode = PrintHelper.COLOR_MODE_MONOCHROME
                    }.printBitmap("BadgeBot marker $markerId", printBitmap)
                },
            ) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Print")
            }

            OutlinedButton(
                onClick = {
                    val shareBitmap = ArucoBitmap.render(markerId, sizePx = 2048)
                    Sharing.shareBitmap(
                        context = context,
                        bitmap = shareBitmap,
                        fileName = "badgebot-aruco-$markerId.png",
                        title = "Share ArUco marker",
                    )
                },
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Share")
            }
        }
    }
}
