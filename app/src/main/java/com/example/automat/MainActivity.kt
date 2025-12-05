package com.example.automat

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automat.data.DatabaseHelper
import com.example.automat.data.Repository
import com.example.automat.data.RoomItem
import com.example.automat.ui.theme.AutomatTheme
import java.util.*

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()
    object SearchFilter : Screen()
    data class SearchResults(val queryGuests: Int, val dateFrom: String, val dateTo: String) : Screen()
    object AllRooms : Screen()
    data class RoomDetail(val roomId: Long, val sourceScreen: String) : Screen() // sourceScreen: "search" or "all"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = DatabaseHelper(this)
        val repo = Repository(db)

        setContent {
            AutomatTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
                var previousScreen by remember { mutableStateOf<Screen?>(null) }
                var currentUserFullName by remember { mutableStateOf<String?>(null) }
                var lastSearchParams by remember { mutableStateOf(Triple(1, "", "")) } // guests, dateFrom, dateTo

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (val s = currentScreen) {
                        is Screen.Login -> {
                            var showError by remember { mutableStateOf(false) }
                            LoginScreen(
                                onLogin = { username, password ->
                                    val user = repo.login(username, password)
                                    if (user != null) {
                                        showError = false
                                        currentUserFullName = user.fullname
                                        currentScreen = Screen.Home
                                    } else {
                                        showError = true
                                    }
                                },
                                onRegister = { currentScreen = Screen.Register },
                                showError = showError,
                                onErrorDismiss = { showError = false }
                            )
                        }

                        is Screen.Register -> RegisterScreen(onRegisterDone = { username, password, fullname ->
                            repo.register(username, password, fullname)
                            currentUserFullName = fullname
                            currentScreen = Screen.Home
                        }, onBack = { currentScreen = Screen.Login })

                        is Screen.Home -> HomeScreen(userFullName = currentUserFullName ?: "Пользователь", onLogout = {
                            currentUserFullName = null
                            currentScreen = Screen.Login
                        }, onSearch = { currentScreen = Screen.SearchFilter }, onViewAll = { currentScreen = Screen.AllRooms })

                        is Screen.SearchFilter -> SearchFilterScreen(onFind = { guests, dateFrom, dateTo ->
                            lastSearchParams = Triple(guests, dateFrom, dateTo)
                            previousScreen = Screen.SearchFilter
                            currentScreen = Screen.SearchResults(guests, dateFrom, dateTo)
                        }, onBack = { currentScreen = Screen.Home })

                        is Screen.SearchResults -> SearchResultsScreen(
                            repo = repo,
                            guests = s.queryGuests,
                            dateFrom = s.dateFrom,
                            dateTo = s.dateTo,
                            onBack = { currentScreen = Screen.SearchFilter },
                            onOpenRoom = { id ->
                                previousScreen = currentScreen
                                currentScreen = Screen.RoomDetail(id, "search")
                            }
                        )

                        is Screen.AllRooms -> AllRoomsScreen(
                            repo = repo,
                            onBack = { currentScreen = Screen.Home },
                            onOpenRoom = { id ->
                                previousScreen = currentScreen
                                currentScreen = Screen.RoomDetail(id, "all")
                            }
                        )

                        is Screen.RoomDetail -> RoomDetailScreen(
                            repo = repo,
                            roomId = s.roomId,
                            onBack = {
                                currentScreen = when (s.sourceScreen) {
                                    "search" -> Screen.SearchResults(lastSearchParams.first, lastSearchParams.second, lastSearchParams.third)
                                    "all" -> Screen.AllRooms
                                    else -> Screen.Home
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onRegister: () -> Unit, showError: Boolean, onErrorDismiss: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Вход", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (showError) {
            Text(
                text = "Неверный логин или пароль",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        OutlinedTextField(value = username, onValueChange = {
            username = it
            onErrorDismiss()
        }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = {
            password = it
            onErrorDismiss()
        }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            onLogin(username.trim(), password.trim())
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Войти")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Регистрация")
        }
    }
}

@Composable
fun RegisterScreen(onRegisterDone: (String, String, String) -> Unit, onBack: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullname by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Регистрация", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = fullname, onValueChange = { fullname = it }, label = { Text("ФИО") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRegisterDone(username.trim(), password.trim(), fullname.trim()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Зарегистрироваться")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Назад") }
    }
}

@Composable
fun HomeScreen(userFullName: String, onLogout: () -> Unit, onSearch: () -> Unit, onViewAll: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Здравствуйте, $userFullName", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Выйти из учётной записи") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Text("Поиск номеров по фильтрам") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onViewAll, modifier = Modifier.fillMaxWidth()) { Text("Просмотр всех номеров") }
    }
}

@Composable
fun SearchFilterScreen(onFind: (Int, String, String) -> Unit, onBack: () -> Unit) {
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var guests by remember { mutableStateOf("1") }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    val dateFromPicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateFrom = String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    val dateToPicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateTo = String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Поиск номеров", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = dateFrom,
            onValueChange = {},
            label = { Text("Дата заезда") },
            modifier = Modifier.fillMaxWidth().clickable { dateFromPicker.show() },
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { dateFromPicker.show() }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = dateTo,
            onValueChange = {},
            label = { Text("Дата выезда") },
            modifier = Modifier.fillMaxWidth().clickable { dateToPicker.show() },
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { dateToPicker.show() }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = guests,
            onValueChange = { guests = it },
            label = { Text("Кол-во человек") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { onBack() }) { Text("Назад") }
            Button(onClick = { onFind(guests.toIntOrNull() ?: 1, dateFrom, dateTo) }) { Text("Найти") }
        }
    }
}

@Composable
fun SearchResultsScreen(repo: Repository, guests: Int, dateFrom: String, dateTo: String, onBack: () -> Unit, onOpenRoom: (Long) -> Unit) {
    val rooms by remember { mutableStateOf(repo.searchAvailable(guests)) }
    Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)) {
        Text(text = "Результаты поиска", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (dateFrom.isNotEmpty() && dateTo.isNotEmpty()) {
            Text(text = "Даты: $dateFrom - $dateTo", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(text = "Гостей: $guests", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            RoomList(rooms = rooms, onOpen = onOpenRoom)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
fun AllRoomsScreen(repo: Repository, onBack: () -> Unit, onOpenRoom: (Long) -> Unit) {
    var filter by remember { mutableStateOf("all") }
    val rooms by remember { mutableStateOf(repo.getAllRooms()) }
    val filtered = when (filter) {
        "available" -> rooms.filter { it.isAvailable }
        "unavailable" -> rooms.filter { !it.isAvailable }
        else -> rooms
    }
    Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)) {
        Text(text = "Все номера", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { filter = "all" }) { Text("Все") }
            Button(onClick = { filter = "available" }) { Text("Доступные") }
            Button(onClick = { filter = "unavailable" }) { Text("Недоступные") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            RoomList(rooms = filtered, onOpen = onOpenRoom)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
fun RoomList(rooms: List<RoomItem>, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    if (rooms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет доступных номеров", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rooms) { room ->
                val imageResId = context.resources.getIdentifier(room.imageRes, "drawable", context.packageName)
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onOpen(room.id) }, shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = if (imageResId != 0) imageResId else com.example.automat.R.drawable.ic_room_placeholder),
                            contentDescription = null,
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Колонка с текстом делаем высотой как у изображения и выравниваем содержимое по центру,
                        // чтобы текст не «прилипал» к верхней границе карточки
                        Column(modifier = Modifier.height(88.dp), verticalArrangement = Arrangement.Center) {
                            Text(text = "Номер ${room.number}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Вместимость: ${room.capacity} чел")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Цена: ${room.price} ₽/сутки", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = if (room.isAvailable) "Доступен" else "Занят", color = if (room.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomDetailScreen(repo: Repository, roomId: Long, onBack: () -> Unit) {
    val room = repo.getRoomById(roomId)
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Информация о номере", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (room != null) {
            val imageResId = context.resources.getIdentifier(room.imageRes, "drawable", context.packageName)
            Image(
                painter = painterResource(id = if (imageResId != 0) imageResId else com.example.automat.R.drawable.ic_room_placeholder),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Номер ${room.number}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Вместимость: ${room.capacity} человек", fontSize = 16.sp)
            Text(text = "Цена: ${room.price} ₽/сутки", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (room.isAvailable) "Статус: Доступен" else "Статус: Занят",
                fontSize = 16.sp,
                color = if (room.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!room.amenities.isNullOrEmpty()) {
                Text(text = "Удобства:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = room.amenities, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Text(text = "Описание:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = room.description ?: "Описание отсутствует", fontSize = 16.sp)
        } else {
            Text("Номер не найден")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}
