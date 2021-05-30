package contributors

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.Base64

interface VimeoService {
    @GET("{id}")
    suspend fun getVideo(
        @Path("id") id: Int,
    ): Response<ResponseBody>
}

@Serializable
data class Video(
    val id: Int,
    val title: String
)

@OptIn(ExperimentalSerializationApi::class)
fun createVimeoService(): VimeoService {
    val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            val request = builder.build()
            chain.proceed(request)
        }
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://player.vimeo.com/video/")
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .client(httpClient)
        .build()
    return retrofit.create(VimeoService::class.java)
}
