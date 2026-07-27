package com.gembyte.poslite.components.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gembyte.poslite.ui.navigation.AppDestination
import com.gembyte.poslite.ui.navigation.drawerItems

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    currentRoute: AppDestination?,
    onItemClick: (AppDestination) -> Unit,
    content: @Composable () -> Unit
) {

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {

            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "POS Lite",
                    modifier = Modifier.padding(
                        horizontal = 16.dp
                    ),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                drawerItems.forEach { item ->
                    AppDrawerItem(
                        title = item.title,
                        selected = currentRoute == item.route,
                        icon = item.icon,
                        onClick = {
                            onItemClick(item.route)
                        }
                    )
                }
            }
        }
    ) {
        content()
    }
}