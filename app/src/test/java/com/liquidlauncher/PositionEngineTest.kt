package com.liquidlauncher

import org.junit.Assert.*
import org.junit.Test

class PositionEngineTest {
    @Test fun positionsRetainEmptyCells() {
        val first = HomeTile("a", TileType.APP, 0, 0, 0)
        val second = HomeTile("b", TileType.APP, 0, 0, 2)
        assertTrue(PositionEngine.free(listOf(first, second), HomeTile("c", TileType.APP, 0, 0, 1)))
        assertFalse(PositionEngine.free(listOf(first, second), HomeTile("c", TileType.APP, 0, 0, 2)))
    }
    @Test fun preservesPositionsAcrossPages() {
        val tiles = listOf(HomeTile("a", TileType.APP, 0, 0, 0), HomeTile("b", TileType.APP, 0, 0, 1))
        val moved = PositionEngine.move(tiles, "a", 1, 3, 2)
        assertEquals(1, moved.first().page)
        assertEquals(3, moved.first().row)
        assertEquals(2, moved.first().column)
        assertEquals(1, moved.last().column)
    }
    @Test fun swapsTwoShortcuts() {
        val tiles = listOf(HomeTile("a", TileType.APP, 0, 0, 0), HomeTile("b", TileType.APP, 1, 2, 2))
        val result = PositionEngine.swap(tiles, "a", "b")
        assertEquals(1, result[0].page)
        assertEquals(0, result[1].page)
    }
    @Test fun folderReplacesTwoIconsInFirstPosition() {
        val tiles = PositionEngine.fromOrdered(listOf("a", "b", "c"))
        val result = PositionEngine.createFolder(tiles, "a", "b", "folder:1")
        assertEquals(2, result.size)
        assertEquals(TileType.FOLDER, result.first().type)
        assertEquals("folder:1", result.first().id)
    }
    @Test fun rejectsOverlappingLargerWidgets() {
        val occupied = HomeTile("w", TileType.WIDGET, 0, 0, 0, 2, 2)
        assertFalse(PositionEngine.free(listOf(occupied), HomeTile("a", TileType.APP, 0, 1, 1)))
        assertTrue(PositionEngine.free(listOf(occupied), HomeTile("b", TileType.APP, 0, 3, 3)))
    }
}
