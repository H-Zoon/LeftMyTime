import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.main.CircularProgress

@Composable
fun CardWidget(itemList: AdapterItem
) {
    Card(
        modifier = Modifier
            .height(400.dp)
            .width(300.dp)
            .padding(10.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = itemList.title,
                modifier = Modifier.padding(20.dp)
            )

            CircularProgress(progress = itemList.percent)

            Text(
                text = itemList.leftString,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Preview
@Composable
fun PreviewMyCardView() {
    //CardWidget()
}