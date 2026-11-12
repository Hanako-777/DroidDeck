package com.droiddeck.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.droiddeck.launcher.glasses.Glasses
import com.droiddeck.launcher.glasses.GlassesState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1200dp-h752dp-land", application = android.app.Application::class)
class SteamHomeHeadsetTest {
    @get:Rule val compose = createRobolectricComposeRule()
    private var vrStarts = 0
    private val connected = GlassesState(present = true, model = "VITURE Pro XR", controllable = true, connected = true,
        displayId = 2, width = 1920, height = 1080, hz = 60f)

    @After fun unplug() { headset(GlassesState()) }

    @Test fun nothingShowsUntilAHeadsetIsConnected() {
        show()
        for (state in listOf(GlassesState(), GlassesState(present = true, displayId = 2, width = 1920, height = 1080),
            connected.copy(connected = false, permission = Glasses.Permission.ASKING))) {
            headset(state)
            settle()
            compose.onAllNodesWithTag("play-steamvr").assertCountEquals(0)
            compose.onAllNodesWithTag("drop-open").assertCountEquals(0)
        }
        compose.onAllNodesWithTag("glasses-open").assertCountEquals(0)
    }

    @Test fun aConnectedHeadsetGetsPlaySteamVrWithItsSettingsBadge() {
        headset(connected)
        show()
        compose.onAllNodesWithTag("glasses-open").assertCountEquals(0)
        compose.onNodeWithTag("play-steamvr").assertIsEnabled().performClick()
        assertEquals(1, vrStarts)
        compose.onAllNodesWithText("VITURE PRO XR").assertCountEquals(0)
        compose.onNodeWithTag("drop-open").performClick()
        settle()
        compose.onNodeWithTag("drop-close").assertExists()
        compose.onAllNodesWithText("VITURE PRO XR").assertCountEquals(1)
        compose.onNodeWithTag("drop-close").performClick()
        settle()
        compose.onAllNodesWithTag("drop-close").assertCountEquals(0)
    }

    @Test fun unpluggingClosesTheSheetAndHidesTheButton() {
        headset(connected)
        show()
        compose.onNodeWithTag("drop-open").performClick()
        settle()
        headset(GlassesState())
        settle()
        compose.onAllNodesWithTag("drop-close").assertCountEquals(0)
        compose.onAllNodesWithTag("play-steamvr").assertCountEquals(0)
    }

    @Test fun aMomentWithoutAPictureOnlyPausesPlaySteamVr() {
        headset(connected.copy(displayId = android.view.Display.INVALID_DISPLAY))
        show()
        compose.onNodeWithTag("play-steamvr").assertIsNotEnabled()
        compose.onNodeWithTag("drop-open").assertExists()
    }

    @Test fun localInstallButtonsHideIndependentlyWhenTheirComponentsAreReady() {
        val ready = mutableStateOf(false)
        val proton = mutableStateOf(false)
        var runtimePicks = 0
        var protonPicks = 0
        show(
            runtimeReady = { ready.value }, onImportRuntime = { runtimePicks++ },
            protonInstalled = { proton.value }, onImportProton = { protonPicks++ },
        )
        val runtimeLabel = "Install Linux runtime from file"
        val protonLabel = "Install DroidDeck-Proton from file"
        compose.onNodeWithText(runtimeLabel).assertIsEnabled().performClick()
        assertEquals(1, runtimePicks)
        compose.onNodeWithText(protonLabel).assertIsNotEnabled()
        compose.runOnIdle { ready.value = true }
        settle()
        compose.onAllNodesWithText(runtimeLabel).assertCountEquals(0)
        compose.onNodeWithText(protonLabel).assertIsEnabled().performClick()
        assertEquals(1, protonPicks)
        compose.runOnIdle { proton.value = true }
        settle()
        compose.onAllNodesWithText(protonLabel).assertCountEquals(0)
    }

    private fun headset(state: GlassesState) {
        val field = Glasses::class.java.getDeclaredField("_state").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (field.get(Glasses) as MutableStateFlow<GlassesState>).value = state
    }

    private fun settle() = compose.mainClock.advanceTimeBy(2_000)

    private fun show(
        runtimeReady: () -> Boolean = { true }, onImportRuntime: (() -> Unit)? = null,
        protonInstalled: () -> Boolean = { false }, onImportProton: (() -> Unit)? = null,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            DroidDeckTheme {
                CompositionLocalProvider(LocalFrontFocus provides FrontFocus()) {
                    FocusGlideHost {
                        SteamHome(FrontEndState(
                            installed = "r9".takeIf { runtimeReady() }, ready = runtimeReady(), available = null, busy = false, stage = "", percent = -1,
                            localProtonInstalled = protonInstalled(),
                            desktopInstalled = true, offlineAccount = null, offline = false, frameGenLabel = "",
                            romsDir = null, logsEnabled = false, steamGames = emptyList(), emulators = emptyList(), running = null,
                        ), FrontEndActions(
                            onPlay = {}, onPlaySteamVr = { vrStarts++ }, onPlayDesktopUi = {}, onSteamGame = {}, onDesktop = {}, onEmulator = {},
                            onRom = {}, onResume = {}, onSteamSettings = {}, onDesktopSettings = {},
                            onInstallPackage = {}, onRemovePackage = {}, onRuntime = {}, onFrameGenPick = {},
                            onImportRuntime = onImportRuntime, onImportProton = onImportProton,
                            onImportLossless = {}, onProtons = {}, onComponents = {}, onPerformance = {},
                            onRoms = {}, onFiles = {}, onLogs = {}, onOffline = {},
                            onWirelessAdbPair = { _, _, _, _ -> }, onWirelessAdbApply = { _, _, _, _ -> },
                            onSetPhantomProcessLimit = { _, _ -> },
                        ), Modifier.fillMaxSize())
                    }
                }
            }
        }
        settle()
    }
}
