package com.example.automat.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Константы для базы данных
private const val DB_NAME = "rooms.db"
private const val DB_VERSION = 3

/**
 * DatabaseHelper - класс для работы с локальной SQLite базой данных.
 * Управляет созданием, обновлением схемы БД и предоставляет методы для запросов.
 *
 * @param context Контекст приложения для инициализации базы данных
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    /**
     * Создание таблиц базы данных при первом запуске приложения.
     * Создаются таблицы: users (пользователи) и rooms (номера отеля).
     */
    override fun onCreate(db: SQLiteDatabase) {
        // Создание таблицы пользователей
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                password TEXT,
                fullname TEXT
            )
            """.trimIndent()
        )

        // Создание таблицы номеров отеля
        db.execSQL(
            """
            CREATE TABLE rooms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT,
                capacity INTEGER,
                price REAL,
                description TEXT,
                isAvailable INTEGER,
                amenities TEXT,
                imageRes TEXT
            )
            """.trimIndent()
        )

        // Вставка тестовых пользователей с логинами/паролями: 1/1 и 2/2
        insertUser(db, "1", "1", "Иван Иванов")
        insertUser(db, "2", "2", "Пётр Петров")

        // Вставка тестовых номеров отеля с фотографиями
        insertRoom(db, "101", 2, 1500.0, "Уютный номер с видом", 1, "WiFi, Кондиционер, ТВ", "room_101")
        insertRoom(db, "102", 4, 2500.0, "Семейный номер", 1, "WiFi, Кондиционер, ТВ, Холодильник", "room_102")
        insertRoom(db, "201", 2, 1200.0, "Эконом", 0, "WiFi, ТВ", "room_201")
        insertRoom(db, "202", 3, 1800.0, "Улучшенный", 1, "WiFi, Кондиционер, ТВ, Минибар", "room_202")
    }

    /**
     * Вставка пользователя в базу данных.
     *
     * @param db База данных SQLite
     * @param username Логин пользователя
     * @param password Пароль пользователя
     * @param fullname ФИО пользователя
     */
    private fun insertUser(db: SQLiteDatabase, username: String, password: String, fullname: String) {
        val cv = ContentValues().apply {
            put("username", username)
            put("password", password)
            put("fullname", fullname)
        }
        db.insert("users", null, cv)
    }

    /**
     * Вставка номера отеля в базу данных.
     *
     * @param db База данных SQLite
     * @param number Номер комнаты
     * @param capacity Вместимость (кол-во человек)
     * @param price Цена за сутки
     * @param description Описание номера
     * @param isAvailable Доступность (1 - доступен, 0 - занят)
     * @param amenities Удобства (WiFi, ТВ и т.д.)
     * @param imageRes Имя ресурса изображения
     */
    private fun insertRoom(db: SQLiteDatabase, number: String, capacity: Int, price: Double, description: String, isAvailable: Int, amenities: String?, imageRes: String) {
        val cv = ContentValues().apply {
            put("number", number)
            put("capacity", capacity)
            put("price", price)
            put("description", description)
            put("isAvailable", isAvailable)
            put("amenities", amenities)
            put("imageRes", imageRes)
        }
        db.insert("rooms", null, cv)
    }

    /**
     * Обновление схемы базы данных при изменении версии.
     * Версия 2: добавлено поле amenities (удобства)
     * Версия 3: добавлено поле imageRes (изображения номеров)
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE rooms ADD COLUMN amenities TEXT")
            db.execSQL("UPDATE rooms SET amenities = 'WiFi, ТВ' WHERE id = 1")
            db.execSQL("UPDATE rooms SET amenities = 'WiFi, Кондиционер, ТВ, Холодильник' WHERE id = 2")
            db.execSQL("UPDATE rooms SET amenities = 'WiFi, ТВ' WHERE id = 3")
            db.execSQL("UPDATE rooms SET amenities = 'WiFi, Кондиционер, ТВ, Минибар' WHERE id = 4")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE rooms ADD COLUMN imageRes TEXT")
            db.execSQL("UPDATE rooms SET imageRes = 'room_101' WHERE number = '101'")
            db.execSQL("UPDATE rooms SET imageRes = 'room_102' WHERE number = '102'")
            db.execSQL("UPDATE rooms SET imageRes = 'room_201' WHERE number = '201'")
            db.execSQL("UPDATE rooms SET imageRes = 'room_202' WHERE number = '202'")
        }
    }

    // --- Публичные методы для работы с данными ---

    /**
     * Аутентификация пользователя.
     *
     * @param username Логин
     * @param password Пароль
     * @return Объект User если пользователь найден, иначе null
     */
    fun queryUser(username: String, password: String): User? {
        val db = readableDatabase
        val c: Cursor = db.rawQuery("SELECT id,username,password,fullname FROM users WHERE username = ? AND password = ?", arrayOf(username, password))
        return if (c.moveToFirst()) {
            val user = User(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))
            c.close()
            user
        } else {
            c.close()
            null
        }
    }

    /**
     * Регистрация нового пользователя.
     *
     * @param username Логин
     * @param password Пароль
     * @param fullname ФИО
     */
    fun insertNewUser(username: String, password: String, fullname: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("username", username)
            put("password", password)
            put("fullname", fullname)
        }
        db.insert("users", null, cv)
    }

    /**
     * Получение списка всех номеров.
     *
     * @return Список объектов RoomItem
     */
    fun getAllRooms(): List<RoomItem> {
        val db = readableDatabase
        val c = db.rawQuery("SELECT id,number,capacity,price,description,isAvailable,amenities,imageRes FROM rooms ORDER BY id", null)
        val list = mutableListOf<RoomItem>()
        while (c.moveToNext()) {
            val item = RoomItem(c.getLong(0), c.getString(1), c.getInt(2), c.getDouble(3), c.getString(4), c.getInt(5) != 0, c.getString(6), c.getString(7))
            list.add(item)
        }
        c.close()
        return list
    }

    /**
     * Получение информации о конкретном номере по ID.
     *
     * @param id ID номера
     * @return Объект RoomItem если номер найден, иначе null
     */
    fun getRoomById(id: Long): RoomItem? {
        val db = readableDatabase
        val c = db.rawQuery("SELECT id,number,capacity,price,description,isAvailable,amenities,imageRes FROM rooms WHERE id = ?", arrayOf(id.toString()))
        return if (c.moveToFirst()) {
            val item = RoomItem(c.getLong(0), c.getString(1), c.getInt(2), c.getDouble(3), c.getString(4), c.getInt(5) != 0, c.getString(6), c.getString(7))
            c.close()
            item
        } else {
            c.close()
            null
        }
    }
}
