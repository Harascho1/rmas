package rs.coffeeconquest.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import rs.coffeeconquest.app.ui.admin.AdminScreen
import rs.coffeeconquest.app.ui.auth.AuthScreen
import rs.coffeeconquest.app.ui.cafe.AddCafeScreen
import rs.coffeeconquest.app.ui.cafe.CafeDetailScreen
import rs.coffeeconquest.app.ui.checkin.CheckInScreen
import rs.coffeeconquest.app.ui.common.LoadingBox
import rs.coffeeconquest.app.ui.feed.FeedScreen
import rs.coffeeconquest.app.ui.leaderboard.LeaderboardScreen
import rs.coffeeconquest.app.ui.map.MapScreen
import rs.coffeeconquest.app.ui.owner.CafeManageScreen
import rs.coffeeconquest.app.ui.owner.OwnerScreen
import rs.coffeeconquest.app.ui.profile.ProfileScreen
import rs.coffeeconquest.app.ui.profile.UserProfileScreen
import rs.coffeeconquest.app.ui.staff.StaffQrScreen

@Composable
fun CoffeeConquestApp(sessionViewModel: SessionViewModel = viewModel()) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background) {
        when (val current = session) {
            is SessionState.Checking -> LoadingBox()
            is SessionState.SignedOut -> AuthScreen(onAuthenticated = sessionViewModel::onSignedIn)
            is SessionState.SignedIn -> SignedInApp(current, sessionViewModel)
        }
    }
}

@Composable
private fun SignedInApp(session: SessionState.SignedIn, sessionViewModel: SessionViewModel) {
    val navController = rememberNavController()
    val tabs = tabsFor(session.profile.role)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val tabLabel = stringResource(tab.labelRes)
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tabLabel) },
                            label = { Text(tabLabel, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAP,
            modifier = Modifier.padding(
                bottom = if (currentRoute in tabs.map { it.route }) padding.calculateBottomPadding() else 0.dp,
            ),
        ) {
            composable(Routes.MAP) {
                MapScreen(
                    profile = session.profile,
                    onCafeClick = { navController.navigate(Routes.cafeDetail(it)) },
                    onAddCafe = { navController.navigate(Routes.ADD_CAFE) },
                )
            }

            composable(Routes.LEADERBOARD) {
                LeaderboardScreen(
                    profile = session.profile,
                    onUserClick = { navController.navigate(Routes.userProfile(it)) },
                )
            }

            composable(Routes.FEED) {
                FeedScreen(
                    onCafeClick = { navController.navigate(Routes.cafeDetail(it)) },
                    onUserClick = { navController.navigate(Routes.userProfile(it)) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    profile = session.profile,
                    onSignOut = sessionViewModel::signOut,
                    onCafeClick = { navController.navigate(Routes.cafeDetail(it)) },
                    onRefresh = sessionViewModel::refreshProfile,
                )
            }

            composable(Routes.OWNER) {
                OwnerScreen(
                    onCafeClick = { navController.navigate(Routes.cafeManage(it)) },
                    onAddCafe = { navController.navigate(Routes.ADD_CAFE) },
                )
            }

            composable(Routes.STAFF) {
                StaffQrScreen()
            }

            composable(Routes.ADMIN) {
                AdminScreen(onCafeClick = { navController.navigate(Routes.cafeDetail(it)) })
            }

            composable(
                route = Routes.CAFE_DETAIL,
                arguments = listOf(navArgument("cafeId") { type = NavType.StringType }),
            ) { entry ->
                CafeDetailScreen(
                    cafeId = entry.arguments?.getString("cafeId").orEmpty(),
                    profile = session.profile,
                    onBack = { navController.popBackStack() },
                    onCheckIn = { navController.navigate(Routes.checkIn(it)) },
                    onUserClick = { navController.navigate(Routes.userProfile(it)) },
                )
            }

            composable(
                route = Routes.CHECK_IN,
                arguments = listOf(navArgument("cafeId") { type = NavType.StringType }),
            ) { entry ->
                CheckInScreen(
                    cafeId = entry.arguments?.getString("cafeId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onFinished = {
                        sessionViewModel.refreshProfile()
                        navController.popBackStack()
                    },
                )
            }

            composable(Routes.ADD_CAFE) {
                AddCafeScreen(
                    profile = session.profile,
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { entry ->
                UserProfileScreen(
                    userId = entry.arguments?.getString("userId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onCafeClick = { navController.navigate(Routes.cafeDetail(it)) },
                )
            }

            composable(
                route = Routes.CAFE_MANAGE,
                arguments = listOf(navArgument("cafeId") { type = NavType.StringType }),
            ) { entry ->
                CafeManageScreen(
                    cafeId = entry.arguments?.getString("cafeId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
