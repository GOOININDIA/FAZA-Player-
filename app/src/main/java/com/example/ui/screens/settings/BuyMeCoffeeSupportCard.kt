package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FazaAccentGradientButton
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassIconBadge

const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/FGa6Tc98UN"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuyMeCoffeeSupportCard(
    onOpenSupportLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val openBrowserCd = stringResource(R.string.support_coffee_open_browser_cd)

    FazaGlassCard(
        shape = RoundedCornerShape(28.dp),
        accented = true,
        highlightedBorder = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("about_buy_me_coffee_card")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isUltraCompact = maxWidth < 330.dp
            val contentPadding = if (isUltraCompact) 16.dp else 20.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FazaGlassIconBadge(
                        icon = Icons.Outlined.LocalCafe,
                        contentDescription = stringResource(R.string.support_coffee_icon_cd),
                        size = if (isUltraCompact) 46.dp else 52.dp,
                        iconSize = if (isUltraCompact) 22.dp else 26.dp,
                        cornerRadius = 26.dp,
                        useSolidGradient = true
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.support_coffee_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("about_buy_me_coffee_title")
                        )
                        Text(
                            text = stringResource(R.string.support_coffee_subtitle),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("about_buy_me_coffee_subtitle")
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.support_coffee_description),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = colors.onSecondaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.testTag("about_buy_me_coffee_description")
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SupportBenefitPill(
                        icon = Icons.Outlined.SystemUpdate,
                        label = "Regular Updates"
                    )
                    SupportBenefitPill(
                        icon = Icons.Outlined.BugReport,
                        label = "Bug Fixes"
                    )
                    SupportBenefitPill(
                        icon = Icons.Outlined.Forum,
                        label = "Faster Support"
                    )
                }

                FazaAccentGradientButton(
                    onClick = onOpenSupportLink,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics {
                            contentDescription = openBrowserCd
                        }
                        .testTag("about_buy_me_coffee_button")
                ) {
                    Text(
                        text = stringResource(R.string.support_coffee_button),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportBenefitPill(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
