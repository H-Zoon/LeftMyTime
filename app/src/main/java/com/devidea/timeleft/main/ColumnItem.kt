package com.devidea.timeleft.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import java.text.DecimalFormat

@Composable
fun ColumnItem(item: AdapterItem) {

    val date = item.title

  /*  val color = when (item.title) {
        "수입" -> colorResource(id = Color.Blue)

        else -> colorResource(id = R.color.spend)
    }
*/

    Column(
        modifier = Modifier
            .height(76.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {

        Spacer(modifier = Modifier.heightIn(10.dp))

        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typography = MaterialTheme.typography
            AccountIndicator(
                color = Color.Black,
                modifier = Modifier
            )
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
        Spacer(Modifier.heightIn(5.dp))

        ItemDivider()
    }
}

@Composable
private fun AccountIndicator(color: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .size(4.dp, 36.dp)
            .background(color = color)
    )
}

@Composable
fun ItemDivider(modifier: Modifier = Modifier) {
    Divider(color = Color.Black, thickness = 1.dp, modifier = modifier)
}

fun formatAmount(amount: Int): String {
    return AmountDecimalFormat.format(amount)
}

private val AmountDecimalFormat = DecimalFormat("###,###")

