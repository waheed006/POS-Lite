package com.gembyte.poslite.components.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppDrawerItem(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {

    NavigationDrawerItem(
        label = {
            Text(
                text = title,
                fontSize = 18.sp
            )
        },
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        modifier = Modifier.padding(
            horizontal = 12.dp,
            vertical = 4.dp
        )
    )
}