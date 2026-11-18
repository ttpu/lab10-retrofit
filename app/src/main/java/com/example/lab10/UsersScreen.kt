package com.example.lab10

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun UsersScreen(vm: UsersViewModel = viewModel()) {

    val state = vm.state.collectAsState().value
    val selected = vm.selected.collectAsState().value
    val tries = vm.tries.collectAsState().value
    val context = LocalContext.current

    // ekran ochilganda bir marta chaqiriladi
    LaunchedEffect(Unit) {
        vm.load()
    }

    Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 48.dp)) {

        Text("GitHub users", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }

            is UiState.Success -> {
                LazyColumn {
                    items(state.users, key = { it.id }) { item ->
                        UserItem(item) {
                            vm.openUser(item.login)
                        }
                    }
                }
            }

            is UiState.Error -> {
                Text(state.message, color = Color.Red, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("tries: " + tries)
                Spacer(modifier = Modifier.height(8.dp))

                if (tries < 3) {
                    Button(onClick = { vm.load() }) {
                        Text("Retry")
                    }
                } else {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                        // qaytganda yana Retry chiqsin
                        vm.resetTries()
                    }) {
                        Text("Open Wi-Fi settings")
                    }
                }
            }
        }

        if (selected != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("id: " + selected.id)
            Text("avatar: " + selected.avatarUrl)
        }

        Spacer(modifier = Modifier.height(16.dp))
        // 404 ni tekshirish uchun
        Button(onClick = { vm.loadBadUser() }) {
            Text("404 test")
        }
    }
}

@Composable
fun UserItem(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = user.login,
            fontSize = 18.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UserItemPreview() {
    UserItem(User("mojombo", 1, "https://avatars.githubusercontent.com/u/1")) { }
}
