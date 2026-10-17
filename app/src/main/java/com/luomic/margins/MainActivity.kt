package com.luomic.margins

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.luomic.margins.ui.theme.MarginsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.File

private val client = OkHttpClient()

private const val api: String = "https://pighub.top/images/"
//响应数据
data class Response<T>(
    val code: Int,
    val message: String,
    val data: T
)
data class PigItem(
    val id: Int,
    val title: String,
    val filename: String,
    val mtime: Long,
    @SerializedName("view_count") val viewCount: Int,
    @SerializedName("download_count") val downloadCount: Int,
    @SerializedName("image_url") val imageUrl: String
)
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var content by remember { mutableStateOf<List<PigItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            content = getPig()
        } catch (e: Exception) {
            error = "你是小猪，联系小猪：${e.message}"
        }
    }
    when {
        error != null -> Text(error!!,modifier)
        content.isEmpty() -> Text("正在加载小猪",modifier)
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(content, key = {it.id}){ item ->
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    GlideImage(
                        model = "${api}${item.filename}",
                        contentDescription = item.filename,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .height(200.dp)
                            .width(300.dp)
                            .align(Alignment.CenterHorizontally)

                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        scope.launch{
                            val file = downloadImage(context = context,url = "${api}${item.filename}", fileName = item.filename)
                            Toast.makeText(
                                context,
                                if (file != null) "已保存：${file}" else "下载失败",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }, modifier= Modifier.align(Alignment.CenterHorizontally)){
                        Text("下载")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(item.filename.substringBeforeLast("."),modifier = Modifier.align(Alignment.CenterHorizontally))

                    Text("下载：${item.downloadCount}",modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(12.dp))
                }
                HorizontalDivider()
            }
        }
    }
}
private suspend fun downloadImage(context: Context, url: String, fileName: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "image/*")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                    ) ?: return@withContext null

                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        response.body.byteStream().use { input -> input.copyTo(output) }
                    }

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    "下载/${fileName}"
                } else {
                    // Android 9 及以下：需要 WRITE_EXTERNAL_STORAGE 权限
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(dir, fileName)
                    response.body.byteStream().use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    file.absolutePath
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

private suspend fun getPig(): List<PigItem> {
    return withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://pighub.top/api/images?sort=2")
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            val json = response.body.string()

            // 1. 先看 HTTP 状态码
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "HTTP $code ${response.message}，返回内容前200字符：${json.take(200)}"
                )
            }

            // 2. 看是不是空响应
            if (json.isBlank()) {
                throw IllegalStateException("服务器返回了空内容")
            }

            // 3. 用可空类型接收，解析失败时把原始内容带出来
            val type = object : TypeToken<Response<List<PigItem>>>() {}.type
            val result: Response<List<PigItem>>? = Gson().fromJson(json, type)
                ?: throw IllegalStateException(
                    "JSON 解析失败，原始内容前200字符：${json.take(200)}"
                )

            // 4. 看业务 code
            if (result?.code != 0) {
                throw IllegalStateException(
                    "接口错误 code=${result?.code}，message=${result?.message}"
                )
            }
            result.data
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