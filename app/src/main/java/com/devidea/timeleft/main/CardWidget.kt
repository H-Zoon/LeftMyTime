import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.component.AnimatedNumberText
import com.devidea.timeleft.component.CircularProgress

@Composable
fun CardWidget(
    itemList: AdapterItem
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
            Box(modifier = Modifier.height(200.dp)) {
                AnimatedNumberText(itemList.percent.toInt(), Modifier.align(Alignment.Center))
                CircularProgress(progress = itemList.percent, Modifier.height(300.dp).width(300.dp))
            }

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

    CardWidget(timeItem())
}

fun timeItem(): AdapterItem {
    val adapterItem = AdapterItem()
    adapterItem.title = "오늘도 좋은하루우우"
    adapterItem.percent = 60F
    adapterItem.endString = "23:59:59"
    adapterItem.leftString = "테스트트"

    return adapterItem
}