package com.devidea.timeleft.main


import CardWidget
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.datadase.itemdata.DataRepository
import kotlinx.coroutines.launch

class MainWidgetCompose {

    @Composable
    fun MainCompose(MainViewModel: TimeLeftViewModel, repository: DataRepository) {

        val itemValue by MainViewModel.listFlow2.observeAsState()
        val listState = rememberLazyListState()

        Column {
            itemValue?.let { it ->
                Log.d("it", it.size.toString())
                Greeting(itemList = it, listState = listState)

                ItemList(it, listState = listState,
                    DismissDelete = {
                        repository.deleteItem(it.id)
                    },
                    DismissUpdate = {
                        //TODO 날짜 생성방법 생각해보기
                        /*
                        val updateData = UpdateDate(
                            id = it.id,
                            date = "${it.year}-${it.month}-${it.day}",
                            title = it.title,
                            amount = it.amount,
                            category = it.category
                        )

                        val intent =
                            Intent(context, ProduceActivity::class.java).apply {
                                putExtra("updateData", updateData)
                            }
                        context.startActivity(intent)
                        */
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Greeting(itemList: List<AdapterItem>,
             listState: LazyListState = rememberLazyListState()) {
    HorizontalPager(
        pageCount = itemList.size,
        state = rememberPagerState(),
    ) { pager ->
        CardWidget(itemList[pager])
    }
}

class SnackbarVisualsWithError(
    override val message: String,
    val isError: Boolean
) : SnackbarVisuals {
    override val actionLabel: String
        get() = if (isError) "Error" else "OK"
    override val withDismissAction: Boolean
        get() = false
    override val duration: SnackbarDuration
        get() = SnackbarDuration.Indefinite
}

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter",
        "UnusedMaterial3ScaffoldPaddingParameter"
    )
    @Composable
    fun ItemList(
        itemList: List<AdapterItem>,
        listState: LazyListState = rememberLazyListState(),
        DismissDelete: (listItem: AdapterItem) -> Unit,
        DismissUpdate: (listItem: AdapterItem) -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            modifier = Modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .border(2.dp, MaterialTheme.colorScheme.secondary)
                        .padding(12.dp),
                    action = {
                        TextButton(
                            onClick = { }
                        ) { Text(data.visuals.actionLabel ?: "") }
                    }
                ) {
                    Text(data.visuals.message)
                }
            }
                           },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(modifier = Modifier, state = listState) {
                items(count = itemList.size, key = { pos -> itemList[pos].id }) { pos ->
                    val dismissState = rememberDismissState(confirmValueChange = { dismissValue ->
                        when (dismissValue) {
                            DismissValue.Default -> { // dismissThresholds 만족 안한 상태
                                false
                            }

                            DismissValue.DismissedToEnd -> { // -> 방향 스와이프 (수정)
                                DismissUpdate(itemList[pos])
                                false
                            }

                            DismissValue.DismissedToStart -> { // <- 방향 스와이프 (삭제)

                                scope.launch {
                                    val temp = itemList[pos]
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            SnackbarVisualsWithError(
                                                "test",
                                                isError = true
                                            )
                                        )
                                    }
                                }
                                    /*val snackBarResult =
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            itemList[pos].title + " 항목을 삭제했습니다",
                                            actionLabel = "복원"
                                        )
                                    when (snackBarResult.toString()) {
                                        "ActionPerformed" -> {
                                            //ItemRepo.saveItem(temp)
                                        }

                                        else -> Log.d("SnackbarResult", snackBarResult.toString())
                                    }
                                */

                                DismissDelete(itemList[pos])
                                true
                            }
                        }
                    })

                    SwipeToDismiss(
                        state = dismissState,
                        modifier = Modifier
                            .padding(vertical = Dp(1f)),
                        directions = setOf(
                            DismissDirection.EndToStart,
                            DismissDirection.StartToEnd
                        ),
                        background = {
                            val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    DismissValue.Default -> Color.Green.copy(alpha = 0.5f) // dismissThresholds 만족 안한 상태
                                    DismissValue.DismissedToEnd -> Color.Green.copy(alpha = 0.5f) // -> 방향 스와이프 (수정)
                                    DismissValue.DismissedToStart -> Color.Red.copy(alpha = 0.5f) // <- 방향 스와이프 (삭제)
                                }
                            )
                            val icon = when (dismissState.targetValue) {
                                DismissValue.Default -> Icons.Default.Adjust
                                DismissValue.DismissedToEnd -> Icons.Default.Edit
                                DismissValue.DismissedToStart -> Icons.Default.Delete
                            }
                            val scale by animateFloatAsState(
                                when (dismissState.targetValue == DismissValue.Default) {
                                    true -> 0.8f
                                    else -> 1.5f
                                }
                            )
                            val alignment = when (direction) {
                                DismissDirection.EndToStart -> Alignment.CenterEnd
                                DismissDirection.StartToEnd -> Alignment.CenterStart
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 30.dp),
                                contentAlignment = alignment
                            ) {
                                /*androidx.compose.material.Icon(
                                    imageVector = icon,
                                    contentDescription = "",
                                    modifier = Modifier.scale(scale)
                                )*/
                            }
                        },
                        dismissContent = {
                            ColumnItem(itemList[pos])
                        }
                    )
                }
            }
        }
    }


