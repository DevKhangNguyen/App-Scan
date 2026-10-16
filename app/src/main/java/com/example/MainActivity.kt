package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.DocumentViewModel
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppTab
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.CropAdjustScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.FilterEnhanceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OcrResultScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AdMobManager

sealed interface AppScreen {
    object Home : AppScreen
    object Docs : AppScreen
    object Tools : AppScreen
    object Camera : AppScreen
    object Crop : AppScreen
    object Filter : AppScreen
    data class Detail(val docId: Long) : AppScreen
    data class Ocr(val docId: Long) : AppScreen
}

class MainActivity : ComponentActivity() {

    private val viewModel: DocumentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Preload AdMob ads in background for smooth presentation
        AdMobManager.preloadInterstitial(this)
        AdMobManager.preloadRewarded(this)

        setContent {
            MyApplicationTheme {
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var currentTab by remember { mutableStateOf(AppTab.HOME) }

    val isMainTab = currentScreen is AppScreen.Home || currentScreen is AppScreen.Docs || currentScreen is AppScreen.Tools

    // Back handling for sub-screens
    BackHandler(enabled = !isMainTab || currentTab != AppTab.HOME) {
        when {
            currentScreen is AppScreen.Ocr -> {
                val docId = (currentScreen as AppScreen.Ocr).docId
                currentScreen = AppScreen.Detail(docId)
            }
            currentScreen is AppScreen.Detail -> {
                currentScreen = if (currentTab == AppTab.DOCS) AppScreen.Docs else AppScreen.Home
            }
            currentScreen is AppScreen.Filter -> {
                currentScreen = AppScreen.Crop
            }
            currentScreen is AppScreen.Crop -> {
                currentScreen = AppScreen.Camera
            }
            currentScreen is AppScreen.Camera -> {
                currentScreen = AppScreen.Home
            }
            currentTab != AppTab.HOME -> {
                currentTab = AppTab.HOME
                currentScreen = AppScreen.Home
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isMainTab) {
                AppBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        currentTab = tab
                        currentScreen = when (tab) {
                            AppTab.HOME -> AppScreen.Home
                            AppTab.DOCS -> AppScreen.Docs
                            AppTab.TOOLS -> AppScreen.Tools
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isMainTab) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToCamera = { currentScreen = AppScreen.Camera },
                        onNavigateToDocumentDetail = { docId -> currentScreen = AppScreen.Detail(docId) },
                        onNavigateToDocsList = {
                            currentTab = AppTab.DOCS
                            currentScreen = AppScreen.Docs
                        },
                        onNavigateToCrop = { currentScreen = AppScreen.Crop }
                    )
                }

                is AppScreen.Docs -> {
                    DocumentsScreen(
                        viewModel = viewModel,
                        onNavigateToDocumentDetail = { docId -> currentScreen = AppScreen.Detail(docId) }
                    )
                }

                is AppScreen.Tools -> {
                    ToolsScreen(
                        viewModel = viewModel,
                        onNavigateToCamera = { currentScreen = AppScreen.Camera }
                    )
                }

                is AppScreen.Camera -> {
                    CameraScanScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.Home },
                        onNavigateToCrop = { currentScreen = AppScreen.Crop }
                    )
                }

                is AppScreen.Crop -> {
                    CropAdjustScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.Camera },
                        onNavigateToFilter = { currentScreen = AppScreen.Filter }
                    )
                }

                is AppScreen.Filter -> {
                    FilterEnhanceScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.Crop },
                        onNavigateToCameraForMore = { currentScreen = AppScreen.Camera },
                        onDocumentSaved = { docId -> currentScreen = AppScreen.Detail(docId) }
                    )
                }

                is AppScreen.Detail -> {
                    DocumentDetailScreen(
                        documentId = screen.docId,
                        viewModel = viewModel,
                        onNavigateBack = {
                            currentScreen = if (currentTab == AppTab.DOCS) AppScreen.Docs else AppScreen.Home
                        },
                        onNavigateToOcrResult = {
                            currentScreen = AppScreen.Ocr(screen.docId)
                        }
                    )
                }

                is AppScreen.Ocr -> {
                    OcrResultScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            currentScreen = AppScreen.Detail(screen.docId)
                        }
                    )
                }
            }
        }
    }
}
