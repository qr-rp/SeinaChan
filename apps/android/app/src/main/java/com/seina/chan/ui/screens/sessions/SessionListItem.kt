package com.seina.chan.ui.screens.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seina.chan.data.model.Session
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Hairline
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.InkLight
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.Spacing
import com.seina.chan.ui.theme.SurfaceCard
import com.seina.chan.ui.theme.TextStyles

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionListItem(
    session: Session,
    isSelected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 2.dp else 0.dp,
                shape = AppShapes.md
            )
            .background(if (isSelected) SurfaceCard else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = session.title ?: "新会话",
                style = TextStyles.bodyMd,
                color = Ink
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = session.preview ?: "无消息",
                style = TextStyles.bodySm,
                color = InkLight
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = session.lastActiveAt ?: "",
                    style = TextStyles.caption,
                    color = InkLight,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${session.messageCount}",
                    style = TextStyles.caption,
                    color = Primary
                )
            }
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Hairline)
            )
        }
    }
}
