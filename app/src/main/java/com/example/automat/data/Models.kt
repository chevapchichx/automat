package com.example.automat.data

/**
 * Модель данных пользователя.
 *
 * @property id Уникальный идентификатор пользователя
 * @property username Логин пользователя
 * @property password Пароль пользователя
 * @property fullname ФИО пользователя
 */
data class User(
    val id: Long,
    val username: String,
    val password: String,
    val fullname: String
)

/**
 * Модель данных номера отеля.
 *
 * @property id Уникальный идентификатор номера
 * @property number Номер комнаты (например, "101", "202")
 * @property capacity Вместимость номера (количество человек)
 * @property price Цена за сутки (в рублях)
 * @property description Описание номера
 * @property isAvailable Доступность номера (true - свободен, false - занят)
 * @property amenities Удобства в номере (WiFi, Кондиционер, ТВ и т.д.), разделенные запятой
 * @property imageRes Имя ресурса изображения (room_101, room_102 и т.д.)
 */
data class RoomItem(
    val id: Long,
    val number: String,
    val capacity: Int,
    val price: Double,
    val description: String?,
    val isAvailable: Boolean,
    val amenities: String?,
    val imageRes: String
)
