package com.liquidlauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class TileType { APP, FOLDER, WIDGET }

data class HomeTile(
    val id: String,
    val type: TileType,
    val page: Int,
    val row: Int,
    val column: Int,
    val rowSpan: Int = 1,
    val columnSpan: Int = 1
) {
    val token: String get() = id
}

object PositionEngine {
    const val COLUMNS = 4
    const val ROWS = 5
    const val CELLS = COLUMNS * ROWS

    fun occupies(tile: HomeTile): Set<Pair<Int, Int>> =
        (tile.row until tile.row + tile.rowSpan).flatMap { r ->
            (tile.column until tile.column + tile.columnSpan).map { c -> r to c }
        }.toSet()

    fun valid(tile: HomeTile): Boolean =
        tile.page >= 0 && tile.row >= 0 && tile.column >= 0 &&
            tile.rowSpan in 1..ROWS && tile.columnSpan in 1..COLUMNS &&
            tile.row + tile.rowSpan <= ROWS && tile.column + tile.columnSpan <= COLUMNS

    fun free(tiles: List<HomeTile>, target: HomeTile, except: String = target.id): Boolean =
        valid(target) && tiles.none {
            it.page == target.page && it.id != except && occupies(it).any { cell -> cell in occupies(target) }
        }

    fun move(tiles: List<HomeTile>, id: String, page: Int, row: Int, column: Int): List<HomeTile> {
        val old = tiles.firstOrNull { it.id == id } ?: return tiles
        val next = old.copy(page = page, row = row, column = column)
        return if (free(tiles, next)) tiles.map { if (it.id == id) next else it } else tiles
    }

    fun swap(tiles: List<HomeTile>, id: String, otherId: String): List<HomeTile> {
        val first = tiles.firstOrNull { it.id == id } ?: return tiles
        val second = tiles.firstOrNull { it.id == otherId } ?: return tiles
        if (first.rowSpan != 1 || first.columnSpan != 1 || second.rowSpan != 1 || second.columnSpan != 1) return tiles
        return tiles.map {
            when (it.id) {
                id -> first.copy(page = second.page, row = second.row, column = second.column)
                otherId -> second.copy(page = first.page, row = first.row, column = first.column)
                else -> it
            }
        }
    }

    fun insert(tiles: List<HomeTile>, id: String, type: TileType = TileType.APP, startPage: Int = 0): List<HomeTile> {
        if (tiles.any { it.id == id }) return tiles
        for (page in startPage..maxOf(startPage, tiles.maxOfOrNull { it.page } ?: 0) + 1) {
            for (row in 0 until ROWS) for (column in 0 until COLUMNS) {
                val target = HomeTile(id, type, page, row, column)
                if (free(tiles, target)) return tiles + target
            }
        }
        return tiles
    }

    fun insertWidget(tiles: List<HomeTile>, id: String, spanRows: Int = 2, spanColumns: Int = 2): List<HomeTile> {
        if (tiles.any { it.id == id }) return tiles
        val lastPage = maxOf(0, tiles.maxOfOrNull { it.page } ?: 0)
        for (page in 0..lastPage + 1) {
            for (row in 0..ROWS - spanRows) {
                for (column in 0..COLUMNS - spanColumns) {
                    val target = HomeTile(id, TileType.WIDGET, page, row, column, spanRows, spanColumns)
                    if (free(tiles, target)) return tiles + target
                }
            }
        }
        return tiles
    }

    fun fromOrdered(keys: List<String>): List<HomeTile> =
        keys.distinct().mapIndexed { index, id ->
            HomeTile(id, if (id.startsWith("folder:")) TileType.FOLDER else TileType.APP,
                index / CELLS, (index % CELLS) / COLUMNS, index % COLUMNS)
        }

    fun prune(tiles: List<HomeTile>, available: Set<String>): List<HomeTile> =
        tiles.filter { it.id in available }.distinctBy { it.id }
            .fold(emptyList()) { result, item -> if (free(result, item)) result + item else result }

    fun createFolder(tiles: List<HomeTile>, first: String, second: String, folder: String): List<HomeTile> {
        val a = tiles.firstOrNull { it.id == first } ?: return tiles
        val b = tiles.firstOrNull { it.id == second } ?: return tiles
        if (first == second || a.type != TileType.APP || b.type != TileType.APP) return tiles
        return (tiles.filterNot { it.id == first || it.id == second } +
            a.copy(id = folder, type = TileType.FOLDER)).sortedWith(compareBy<HomeTile>({ it.page }, { it.row }, { it.column }))
    }
}

class PositionRepository(context: Context) {
    private val prefs = context.getSharedPreferences("liquid_position_v5", Context.MODE_PRIVATE)

    fun load(): List<HomeTile>? {
        val raw = prefs.getString("tiles", null) ?: return null
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).map { n ->
                val item = json.getJSONObject(n)
                HomeTile(
                    id = item.getString("id"),
                    type = TileType.valueOf(item.getString("type")),
                    page = item.getInt("page"), row = item.getInt("row"),
                    column = item.getInt("column"),
                    rowSpan = item.optInt("rowSpan", 1),
                    columnSpan = item.optInt("columnSpan", 1)
                )
            }.filter(PositionEngine::valid)
        }.getOrNull()
    }

    fun save(tiles: List<HomeTile>) {
        val json = JSONArray()
        tiles.forEach { tile ->
            json.put(JSONObject().apply {
                put("id", tile.id); put("type", tile.type.name); put("page", tile.page)
                put("row", tile.row); put("column", tile.column)
                put("rowSpan", tile.rowSpan); put("columnSpan", tile.columnSpan)
            })
        }
        prefs.edit().putString("tiles", json.toString()).apply()
    }
}
