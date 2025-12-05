# API Документация

## Data Layer

### Models.kt

#### User
```kotlin
data class User(
    val id: Long,           // Уникальный ID
    val username: String,   // Логин
    val password: String,   // Пароль
    val fullname: String    // ФИО
)
```

#### RoomItem
```kotlin
data class RoomItem(
    val id: Long,              // Уникальный ID
    val number: String,        // Номер комнаты ("101", "202")
    val capacity: Int,         // Вместимость (человек)
    val price: Double,         // Цена (₽/сутки)
    val description: String?,  // Описание
    val isAvailable: Boolean,  // Доступность
    val amenities: String?,    // Удобства (через запятую)
    val imageRes: String       // Имя ресурса изображения
)
```

### DatabaseHelper.kt

#### Методы работы с пользователями

```kotlin
// Аутентификация
fun queryUser(username: String, password: String): User?

// Регистрация
fun insertNewUser(username: String, password: String, fullname: String)
```

#### Методы работы с номерами

```kotlin
// Получить все номера
fun getAllRooms(): List<RoomItem>

// Получить номер по ID
fun getRoomById(id: Long): RoomItem?
```

### Repository.kt

```kotlin
class Repository(private val db: DatabaseHelper) {
    
    // Вход в систему
    fun login(username: String, password: String): User?
    
    // Регистрация
    fun register(username: String, password: String, fullname: String)
    
    // Все номера
    fun getAllRooms(): List<RoomItem>
    
    // Номер по ID
    fun getRoomById(id: Long): RoomItem?
    
    // Поиск доступных номеров по вместимости
    fun searchAvailable(guests: Int): List<RoomItem>
}
```

## UI Layer

### Навигация (Screen sealed class)

```kotlin
sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()
    object SearchFilter : Screen()
    data class SearchResults(val queryGuests: Int, val dateFrom: String, val dateTo: String) : Screen()
    object AllRooms : Screen()
    data class RoomDetail(val roomId: Long, val sourceScreen: String) : Screen()
}
```

### Composable функции

#### LoginScreen
```kotlin
@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit,
    showError: Boolean,
    onErrorDismiss: () -> Unit
)
```

#### RegisterScreen
```kotlin
@Composable
fun RegisterScreen(
    onRegisterDone: (String, String, String) -> Unit,
    onBack: () -> Unit
)
```

#### HomeScreen
```kotlin
@Composable
fun HomeScreen(
    userFullName: String,
    onLogout: () -> Unit,
    onSearch: () -> Unit,
    onViewAll: () -> Unit
)
```

#### SearchFilterScreen
```kotlin
@Composable
fun SearchFilterScreen(
    onFind: (Int, String, String) -> Unit,
    onBack: () -> Unit
)
```

#### SearchResultsScreen
```kotlin
@Composable
fun SearchResultsScreen(
    repo: Repository,
    guests: Int,
    dateFrom: String,
    dateTo: String,
    onBack: () -> Unit,
    onOpenRoom: (Long) -> Unit
)
```

#### AllRoomsScreen
```kotlin
@Composable
fun AllRoomsScreen(
    repo: Repository,
    onBack: () -> Unit,
    onOpenRoom: (Long) -> Unit
)
```

#### RoomDetailScreen
```kotlin
@Composable
fun RoomDetailScreen(
    repo: Repository,
    roomId: Long,
    onBack: () -> Unit
)
```

#### RoomList (вспомогательная)
```kotlin
@Composable
fun RoomList(
    rooms: List<RoomItem>,
    onOpen: (Long) -> Unit
)
```

## Примеры использования

### Добавление нового номера

```kotlin
// В DatabaseHelper.onCreate()
insertRoom(
    db = db,
    number = "303",
    capacity = 3,
    price = 2000.0,
    description = "Люкс с балконом",
    isAvailable = 1,
    amenities = "WiFi, Кондиционер, ТВ, Минибар, Балкон",
    imageRes = "room_303"
)
```

### Поиск номеров

```kotlin
val repo = Repository(DatabaseHelper(context))
val availableRooms = repo.searchAvailable(guests = 2)
```

### Получение деталей номера

```kotlin
val room = repo.getRoomById(1L)
room?.let {
    println("${it.number}: ${it.price}₽/сутки")
}
```

## База данных

### SQL запросы

Все номера:
```sql
SELECT id,number,capacity,price,description,isAvailable,amenities,imageRes 
FROM rooms 
ORDER BY id
```

Номер по ID:
```sql
SELECT id,number,capacity,price,description,isAvailable,amenities,imageRes 
FROM rooms 
WHERE id = ?
```

Пользователь:
```sql
SELECT id,username,password,fullname 
FROM users 
WHERE username = ? AND password = ?
```
