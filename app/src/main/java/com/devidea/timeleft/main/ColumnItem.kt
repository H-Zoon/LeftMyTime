package com.devidea.timeleft.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.component.CircularProgress
import java.text.DecimalFormat

@Composable
fun ColumnItem(item: AdapterItem) {

    val date = item.title
    Card(
        modifier = Modifier
            .height(106.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 15.dp
        )
    ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val typography = MaterialTheme.typography
                CircularProgress(progress = item.percent, Modifier.height(70.dp).width(70.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier) {
                    Text(
                        text = date,
                        style = typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.heightIn(10.dp))

                    Text(
                        text = item.title,
                        style = typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                }
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = date,
                        style = typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = date,
                        style = typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(16.dp))
            }
        }
    }

fun formatAmount(amount: Int): String {
    return AmountDecimalFormat.format(amount)
}

private val AmountDecimalFormat = DecimalFormat("###,###")

@Preview
@Composable
fun PreviewMyCardView() {

    ColumnItem(timeItem())
}

fun timeItem(): AdapterItem {
    val adapterItem = AdapterItem()
    adapterItem.title = "제목입니다"
    adapterItem.percent = 60F
    adapterItem.endString = "23:59:59"
    adapterItem.leftString = "자난 시간입니다"

    return adapterItem
}

