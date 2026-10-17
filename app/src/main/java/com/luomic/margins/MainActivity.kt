package com.luomic.margins

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.luomic.margins.ui.theme.MarginsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val client = OkHttpClient()
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarginsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "World",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var content by remember { mutableStateOf("加载中...") }
    LaunchedEffect(Unit) {
        content = getPig()
    }
    Text(
        text = content,
        modifier = modifier,
    )
}
private suspend fun getPig(): String {
    return withContext(Dispatchers.IO){
        try {
            val request = Request.Builder()
                .url("https://pighub.top/api/images?sort=2")
                .build()
            client.newCall(request).execute().use{ response ->
                response.body.string()
            }
        } catch (e: Exception){
            "请求失败${e.message}"
        }
    }
}
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MarginsTheme {
        Greeting("World!")
    }
}