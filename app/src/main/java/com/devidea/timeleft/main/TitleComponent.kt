package com.devidea.timeleft.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayDateTime() {
    val currentDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("M월 d일 E요일")
    val formattedDateTime = currentDateTime.format(formatter)

    Row(
        modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
    verticalAlignment = Alignment.CenterVertically) {

        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {


            Text(
                text = "오늘",
                style = typography.displaySmall
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = formattedDateTime,
                style = typography.displayMedium
            )
        }
        IconButton(
            onClick = { /* Handle settings button click here */ }) {
            Icon(
                imageVector = Icons.Outlined.ImageSearch,
                contentDescription = "discription"
            )
        }
    }
}

@Preview
@Composable
fun PreviewTodayDateTime() {
    TodayDateTime()
}
