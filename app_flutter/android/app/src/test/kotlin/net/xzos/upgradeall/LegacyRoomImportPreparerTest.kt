package net.xzos.upgradeall

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LegacyRoomImportPreparerTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun missingSourceReturnsNotFoundAndDoesNotCheckpoint() {
        val source = File(temp.root, "source/app_metadata_database.db")
        val destination = File(temp.root, "destination/app_metadata_database.db")
        val checkpointer = RecordingCheckpointer()

        val result = LegacyRoomImportPreparer(checkpointer).prepare(source, destination)

        assertFalse(result.found)
        assertNull(result.databasePath)
        assertEquals("No legacy Room database found", result.message)
        assertTrue(checkpointer.databases.isEmpty())
        assertFalse(destination.exists())
    }

    @Test
    fun copiesExistingSqliteTripletAndCallsCheckpoint() {
        val source = File(temp.root, "source/app_metadata_database.db")
        val destination = File(temp.root, "destination/app_metadata_database.db")
        source.writeTextWithParents("db")
        File(source.path + "-wal").writeTextWithParents("wal")
        File(source.path + "-shm").writeTextWithParents("shm")
        val checkpointer = RecordingCheckpointer()

        val result = LegacyRoomImportPreparer(checkpointer).prepare(source, destination)

        assertTrue(result.found)
        assertEquals(destination.absolutePath, result.databasePath)
        assertEquals("Legacy Room database prepared", result.message)
        assertEquals("db", destination.readText())
        assertEquals("wal", File(destination.path + "-wal").readText())
        assertEquals("shm", File(destination.path + "-shm").readText())
        assertEquals(listOf(destination), checkpointer.databases)
    }

    @Test
    fun removesStaleDestinationSidecarsWhenSourceSidecarsAreAbsent() {
        val source = File(temp.root, "source/app_metadata_database.db")
        val destination = File(temp.root, "destination/app_metadata_database.db")
        source.writeTextWithParents("fresh-db")
        destination.writeTextWithParents("old-db")
        File(destination.path + "-wal").writeTextWithParents("stale-wal")
        File(destination.path + "-shm").writeTextWithParents("stale-shm")
        val checkpointer = RecordingCheckpointer()

        LegacyRoomImportPreparer(checkpointer).prepare(source, destination)

        assertEquals("fresh-db", destination.readText())
        assertFalse(File(destination.path + "-wal").exists())
        assertFalse(File(destination.path + "-shm").exists())
        assertEquals(listOf(destination), checkpointer.databases)
    }

    private fun File.writeTextWithParents(text: String) {
        parentFile?.mkdirs()
        writeText(text)
    }

    private class RecordingCheckpointer : CopiedDatabaseCheckpointer {
        val databases = mutableListOf<File>()

        override fun checkpoint(database: File) {
            databases.add(database)
        }
    }
}
