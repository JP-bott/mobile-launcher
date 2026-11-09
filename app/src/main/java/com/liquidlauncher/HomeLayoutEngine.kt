package com.liquidlauncher

/**
 * Pure layout rules, deliberately independent of Compose and Android.
 * Keeping reordering deterministic makes it easier to test and extend.
 */
object HomeLayoutEngine {
    const val COLUMNS = 4
    const val PAGE_SIZE = 20

    fun pageCount(keys: List<String>): Int = maxOf(1, (keys.size + PAGE_SIZE - 1) / PAGE_SIZE)

    fun page(keys: List<String>, index: Int): List<String> {
        if (index < 0) return emptyList()
        return keys.drop(index * PAGE_SIZE).take(PAGE_SIZE)
    }

    fun reorder(keys: List<String>, from: Int, to: Int): List<String> {
        if (from !in keys.indices) return keys
        val output = keys.toMutableList()
        val item = output.removeAt(from)
        output.add(to.coerceIn(0, output.size), item)
        return output
    }

    fun mergeInstalled(current: List<String>, available: Set<String>, folders: Set<String>): List<String> =
        current.filter { it in available || it in folders }.distinct()

    fun createFolderLayout(current: List<String>, first: String, second: String, folderToken: String): List<String> {
        if (first == second || first !in current || second !in current) return current
        val position = current.indexOf(first)
        val result = current.filterNot { it == first || it == second }.toMutableList()
        result.add(position.coerceIn(0, result.size), folderToken)
        return result
    }
}
