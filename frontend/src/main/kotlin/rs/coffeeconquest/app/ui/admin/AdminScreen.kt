package rs.coffeeconquest.app.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.R
import rs.coffeeconquest.app.ui.common.Avatar
import rs.coffeeconquest.app.ui.common.EmptyBox
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.RoleChip
import rs.coffeeconquest.app.ui.common.PhotoThumb
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.common.rememberVisiblePhoto
import rs.coffeeconquest.app.ui.formatDistance
import rs.coffeeconquest.app.ui.points
import rs.coffeeconquest.app.ui.relativeTime
import rs.coffeeconquest.shared.model.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onCafeClick: (String) -> Unit,
    viewModel: AdminViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.admin_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {
            val tabs = listOf(
                AdminTab.CAFES to stringResource(R.string.admin_tab_cafes),
                AdminTab.CHECK_INS to stringResource(R.string.admin_tab_checkins),
                AdminTab.USERS to stringResource(R.string.admin_tab_users),
            )
            PrimaryTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == state.tab }) {
                tabs.forEach { (tab, label) ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.onTabChange(tab) },
                        text = { Text(label) },
                    )
                }
            }

            when (state.tab) {
                AdminTab.CAFES -> StateContent(state.pendingCafes, viewModel::load) { cafes ->
                    if (cafes.isEmpty()) {
                        EmptyBox(stringResource(R.string.admin_empty_cafes))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(cafes, key = { it.id }) { cafe ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            PhotoThumb(
                                                rememberVisiblePhoto(
                                                    cafe.photoId,
                                                    photos,
                                                    viewModel::requestPhoto
                                                ),
                                                Modifier
                                                    .width(56.dp)
                                                    .height(56.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(cafe.name, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    listOfNotNull(cafe.address, cafe.city).joinToString(
                                                        stringResource(R.string.admin_list_separator),
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        cafe.description?.let {
                                            Spacer(Modifier.height(8.dp))
                                            Text(it, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            stringResource(R.string.admin_coordinates, cafe.latitude, cafe.longitude),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { viewModel.moderateCafe(cafe.id, true) },
                                                enabled = !state.busy,
                                            ) { Text(stringResource(R.string.admin_button_approve)) }
                                            OutlinedButton(
                                                onClick = { viewModel.moderateCafe(cafe.id, false) },
                                                enabled = !state.busy,
                                            ) { Text(stringResource(R.string.admin_button_reject)) }
                                            TextButton(onClick = { onCafeClick(cafe.id) }) { Text(stringResource(R.string.admin_button_details)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.CHECK_INS -> StateContent(state.flagged, viewModel::load) { checkIns ->
                    if (checkIns.isEmpty()) {
                        EmptyBox(stringResource(R.string.admin_empty_checkins))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(checkIns, key = { it.id }) { checkIn ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            stringResource(
                                                R.string.admin_checkin_user_at_cafe,
                                                checkIn.username,
                                                checkIn.cafeName,
                                            ),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            stringResource(
                                                R.string.admin_checkin_time_method,
                                                relativeTime(checkIn.createdAtEpochMs),
                                                checkIn.method,
                                            ) +
                                                (formatDistance(checkIn.distanceMeters)?.let {
                                                    stringResource(R.string.admin_checkin_distance_suffix, it)
                                                } ?: ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        checkIn.flagReason?.let {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                        if (checkIn.photoId != null) {
                                            Spacer(Modifier.height(8.dp))
                                            PhotoThumb(
                                                rememberVisiblePhoto(
                                                    checkIn.photoId,
                                                    photos,
                                                    viewModel::requestPhoto
                                                ),
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp),
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { viewModel.moderateCheckIn(checkIn.id, true) },
                                                enabled = !state.busy,
                                            ) { Text(stringResource(R.string.admin_button_accept)) }
                                            OutlinedButton(
                                                onClick = { viewModel.moderateCheckIn(checkIn.id, false) },
                                                enabled = !state.busy,
                                            ) { Text(stringResource(R.string.admin_button_cancel)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.USERS -> StateContent(state.users, viewModel::load) { users ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(users, key = { it.id }) { user ->
                            var menuOpen by remember { mutableStateOf(false) }
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(
                                        rememberVisiblePhoto(
                                            user.avatarPhotoId,
                                            photos,
                                            viewModel::requestPhoto
                                        ),
                                        user.displayName,
                                        size = 40,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            stringResource(
                                                R.string.admin_user_username_points,
                                                user.username,
                                                points(user.points),
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            RoleChip(user.role)
                                            if (user.isBanned) {
                                                Pill(
                                                    stringResource(R.string.admin_pill_banned),
                                                    background = MaterialTheme.colorScheme.errorContainer,
                                                    foreground = MaterialTheme.colorScheme.onErrorContainer,
                                                )
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        TextButton(
                                            onClick = { menuOpen = true },
                                            modifier = Modifier.clickable { menuOpen = true },
                                        ) { Text(stringResource(R.string.admin_button_role)) }
                                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                            Role.entries.forEach { role ->
                                                DropdownMenuItem(
                                                    text = { Text(role.name) },
                                                    onClick = {
                                                        menuOpen = false
                                                        viewModel.setRole(user.id, role)
                                                    },
                                                )
                                            }
                                        }
                                        TextButton(
                                            onClick = { viewModel.setBanned(user.id, !user.isBanned) },
                                            enabled = !state.busy,
                                        ) {
                                            Text(
                                                if (user.isBanned) {
                                                    stringResource(R.string.admin_button_unban)
                                                } else {
                                                    stringResource(R.string.admin_button_ban)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
