package com.liquidlauncher

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutEngineTest {
    @Test fun pageCountHandlesEmptyAndFullPages() {
        assertEquals(1, HomeLayoutEngine.pageCount(emptyList()))
        assertEquals(1, HomeLayoutEngine.pageCount((1..20).map { it.toString() }))
        assertEquals(2, HomeLayoutEngine.pageCount((1..21).map { it.toString() }))
    }

    @Test fun movesAcrossPageBoundariesWithoutLosingApps() {
        val initial = (1..25).map { "app:$it" }
        val result = HomeLayoutEngine.reorder(initial, 0, 20)
        assertEquals(25, result.size)
        assertEquals("app:1", result[20])
        assertEquals(initial.toSet(), result.toSet())
    }

    @Test fun unavailableAppsAreRemovedButFoldersRetained() {
        val result = HomeLayoutEngine.mergeInstalled(
            listOf("app:A", "app:B", "folder:1", "app:A"),
            setOf("app:A"),
            setOf("folder:1")
        )
        assertEquals(listOf("app:A", "folder:1"), result)
    }

    @Test fun creatingFolderPreservesUnrelatedApps() {
        val result = HomeLayoutEngine.createFolderLayout(
            listOf("A", "B", "C", "D"), "A", "C", "folder:x"
        )
        assertEquals(listOf("folder:x", "B", "D"), result)
    }
}
