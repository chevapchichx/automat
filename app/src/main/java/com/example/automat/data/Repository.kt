package com.example.automat.data

/**
 * Repository - слой абстракции для работы с данными.
 * Предоставляет простой интерфейс для взаимодействия с базой данных.
 *
 * @property db Экземпляр DatabaseHelper для доступа к SQLite
 */
class Repository(private val db: DatabaseHelper) {

    /**
     * Аутентификация пользователя.
     *
     * @param username Логин пользователя
     * @param password Пароль пользователя
     * @return Объект User если учетные данные верны, иначе null
     */
    fun login(username: String, password: String): User? {
        return db.queryUser(username, password)
    }

    /**
     * Регистрация нового пользователя.
     *
     * @param username Логин
     * @param password Пароль
     * @param fullname ФИО
     */
    fun register(username: String, password: String, fullname: String) {
        db.insertNewUser(username, password, fullname)
    }

    /**
     * Получение списка всех номеров (доступных и занятых).
     *
     * @return Список всех номеров
     */
    fun getAllRooms(): List<RoomItem> = db.getAllRooms()

    /**
     * Получение информации о конкретном номере.
     *
     * @param id ID номера
     * @return Объект RoomItem или null, если номер не найден
     */
    fun getRoomById(id: Long): RoomItem? = db.getRoomById(id)

    /**
     * Поиск доступных номеров по количеству гостей.
     * Возвращает только свободные номера с вместимостью >= указанного количества гостей.
     *
     * @param guests Количество гостей
     * @return Список доступных номеров
     */
    fun searchAvailable(guests: Int): List<RoomItem> {
        return db.getAllRooms().filter { it.isAvailable && it.capacity >= guests }
    }
}
