package org.dorossi.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.dorossi.app.R
import org.dorossi.app.core.LocaleSupport
import org.dorossi.app.ui.components.BackTopBar
import org.dorossi.app.ui.components.RadioRow
import org.dorossi.app.ui.components.SectionHeader
import org.dorossi.app.ui.components.SettingsRow

/** App settings, opened from the «إعدادات التطبيق» link at the bottom of the platform pages. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReload: () -> Unit,
    onClearCache: () -> Unit,
    onShare: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val currentLanguage = remember { LocaleSupport.currentTag() }

    Scaffold(
        topBar = { BackTopBar(title = stringResource(R.string.settings), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_platform))
            SettingsRow(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.action_reload),
                subtitle = stringResource(R.string.action_reload_hint),
                onClick = onReload,
            )
            SettingsRow(
                icon = Icons.Filled.CleaningServices,
                title = stringResource(R.string.action_clear_cache),
                subtitle = stringResource(R.string.action_clear_cache_hint),
                onClick = onClearCache,
            )
            SettingsRow(
                icon = Icons.Filled.Share,
                title = stringResource(R.string.action_share),
                subtitle = stringResource(R.string.action_share_hint),
                onClick = onShare,
            )

            if (LocaleSupport.supportedTags.size > 1) {
                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_language))
                LocaleSupport.supportedTags.forEach { tag ->
                    RadioRow(
                        text = LocaleSupport.displayName(tag),
                        selected = tag == currentLanguage,
                        onClick = { if (tag != currentLanguage) LocaleSupport.setLanguage(tag) },
                    )
                }
            }

            HorizontalDivider()
            SettingsRow(icon = Icons.Filled.Info, title = stringResource(R.string.about), onClick = onOpenAbout)
        }
    }
}
