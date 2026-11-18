# Laboratory 10 - Retrofit

My lab 10. One screen app: it loads GitHub users with Retrofit and shows their logins.
Kotlin, Jetpack Compose, min SDK 24, compile SDK 35, AGP 8.7.3, Kotlin 2.0.21.

Everything in it is from Lecture 20.

## Demo video

<details>
<summary>Click to open the demo video (40 seconds)</summary>

https://github.com/ttpu/lab10-retrofit/raw/main/demo.mp4

(the same file is in the repo: `demo.mp4`)

What I show in the video, step by step:

1. Happy path. App starts, spinner, then 5 logins (per_page = 5).
   I tap `mojombo` and `id: 1` with the avatar url appear under the list.
   Logcat: `--> GET https://api.github.com/users?per_page=5` and `<-- 200`.
2. Failure path. Airplane mode on, tap `Retry`. Now it says `No network` and `tries: 1`.
   Two more taps give `tries: 2` and `tries: 3`. After the third one the `Retry` button
   disappears and `Open Wi-Fi settings` takes its place.
3. Recovery path. Airplane mode off, tap `Open Wi-Fi settings`, press back, tap `Retry`.
   The list is back and the counter starts from 0 again.
4. Server error. `404 test` button calls `/users/no-such-login-xyz`.
   Screen: `Server error: 404`, Logcat: `<-- 404`.
5. Optional part. I open `AuthInterceptor.kt` and the commented line in `ApiInstance.kt`
   that would add the `Authorization` header.

</details>

## 1. Dependencies and permission

In `app/build.gradle.kts`:

```kotlin
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
```

In `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Sync went green and `./gradlew assembleDebug` finished without errors.

## 2. Model: User

```kotlin
data class User(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url")
    val avatarUrl: String?
)
```

In the JSON the key is `avatar_url`, in Kotlin I want `avatarUrl`, so `@SerializedName` does the mapping.
The JSON also has `html_url` and `type`. My class does not have them, Gson simply skips them.

Then I renamed the property to `avatarUrll` on purpose. Nothing crashed. The value just stayed
`null` and the screen printed `avatar: null`, which is the dangerous part: a typo here is silent.
After the test I renamed it back.

## 3. Interface: GitHubService

```kotlin
@GET("/users")
suspend fun users(@Query("per_page") perPage: Int): List<User>

@GET("/users/{login}")
suspend fun user(@Path("login") login: String): User
```

Both are `suspend`, so I call them from `viewModelScope`.

## 4. Retrofit builder

Base url is `https://api.github.com/`, with the slash at the end.
I deleted that slash to see what happens, and the app died as soon as the screen opened,
before any request went out:

```
java.lang.IllegalArgumentException: baseUrl must end in /: https://api.github.com
```

The exception comes from `baseUrl()` itself, not from the network. I put the slash back.

## 5. Load users in ViewModel

`LaunchedEffect(Unit) { vm.load() }` runs once, when the screen opens.
Inside `load()` everything happens in `viewModelScope.launch`, and the result goes into a `StateFlow`.
The screen only reads that flow, it never calls the API by itself.
The list is a `LazyColumn` with `key = { it.id }`.

Then I deleted the `INTERNET` line from the manifest. The app crashed and Logcat showed:

```
FATAL EXCEPTION: OkHttp Dispatcher
java.lang.SecurityException: Permission denied (missing INTERNET permission?)
```

Line is back in the manifest now.

## 6. @Query and @Path

`users(5)` sends `?per_page=5`, so exactly 5 items come back.
When I tap a login, `user(login)` is called with `@Path` and `id` with `avatarUrl` are drawn
under the list.

I keep 5 everywhere, because GitHub gives 60 requests per hour per IP and the whole group
sits on the same Wi-Fi.

## 7. UiState

```kotlin
sealed interface UiState {
    data object Loading : UiState
    data class Success(val users: List<User>) : UiState
    data class Error(val message: String) : UiState
}
```

Loading draws `CircularProgressIndicator`, Success draws the list, Error draws the red text.

## 8. ApiInstance

The builder is in `ApiInstance.kt` and nowhere else. `Retrofit` and `OkHttpClient` are `by lazy`,
so they are created once and reused. The ViewModel only asks for the service:

```kotlin
private val service = ApiInstance.createService(GitHubService::class.java)
```

Find in Files for `Retrofit.Builder` gives one result, in `ApiInstance.kt`.

## 9. Logging

`HttpLoggingInterceptor` is attached to the OkHttp client, with `BASIC` in debug and `NONE`
in release, so a release build logs nothing.

This is the place where I lost some time: `BuildConfig.DEBUG` did not compile at first.
AGP 8 does not generate `BuildConfig` unless you ask for it, so I added
`buildConfig = true` into `buildFeatures`.

Logcat in a debug build:

```
--> GET https://api.github.com/users?per_page=5
<-- 200 https://api.github.com/users?per_page=5 (1267ms, unknown-length body)
```

The optional part is `AuthInterceptor.kt`. The line that adds it to the client is commented out,
because my token is fake and GitHub answers 401 to every request with it.

## 10. Errors and retry

Every call goes through `safeApiCall`, and it returns `ApiResult`:

```kotlin
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class HttpError(val code: Int) : ApiResult<Nothing>
    data class NetworkError(val message: String) : ApiResult<Nothing>
}
```

With airplane mode on the call throws `IOException`, that becomes `NetworkError`, and the screen
shows `No network` with a `Retry` button. The `404 test` button gives `HttpError(404)` and
`Server error: 404`. Each failed try adds 1 to the counter, and at 3 the `Retry` button is hidden
and `Open Wi-Fi settings` is shown instead. After a successful load the counter is 0 again.

I did not try the Moshi part.

## How a call travels from my interface to the server

```text
UsersScreen (Compose)
   |  vm.load()
UsersViewModel  -> viewModelScope.launch { safeApiCall { service.users(5) } }
   |
GitHubService (my interface)
   |  I only wrote @GET("/users") and @Query("per_page").
   |  Retrofit reads these annotations and makes the real class for my interface.
Retrofit
   |  builds the url: base url "https://api.github.com/" + "/users" + "?per_page=5"
   |  and makes an okhttp3.Request out of it
OkHttp client
   |  the request goes through the interceptor chain.
   |  HttpLoggingInterceptor prints "--> GET ...", calls chain.proceed(),
   |  and prints "<-- 200 ..." when the answer comes back.
   |  (AuthInterceptor would add the Authorization header here)
Server (api.github.com)
   |  answers with JSON text
GsonConverterFactory
   |  turns that JSON into List<User>. avatar_url goes to avatarUrl because of @SerializedName.
   |  Unknown keys like html_url are skipped.
back to the ViewModel
   |  suspend function returns the list, or throws:
   |  HttpException for 404, IOException when there is no network.
   |  safeApiCall catches both and returns ApiResult.
UsersScreen
      reads the new UiState from the StateFlow and draws the list,
      the spinner or the error message.
```
