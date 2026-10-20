package com.example.timeapk.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EventImageRegistryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val imageName = "a".repeat(32) + ".jpg"
    private var now = 1_000L
    private val grace = EventImageRegistry.GRACE_PERIOD_MILLIS

    private fun registry(metadata: File, images: File) = EventImageRegistry(metadata, images) { now }

    @Test
    fun onlyRegisteredCandidatesPastTheFullGracePeriodAreRemoved() {
        val images = temporary.newFolder("images")
        val image = File(images, imageName).apply { writeText("image") }
        val historical = File(images, "b".repeat(32) + ".jpg").apply { writeText("old draft") }
        val metadata = File(temporary.root, "registry")
        val store = registry(metadata, images)
        assertTrue(store.registerCandidate(imageName))
        now += grace - 1
        assertTrue(store.cleanup { emptySet() })
        assertTrue(image.exists())
        now++
        assertTrue(store.cleanup { emptySet() })
        assertFalse(image.exists())
        assertTrue(historical.exists())
    }

    @Test
    fun persistedDraftAndDatabaseReferencesProtectImagesAcrossRestart() {
        val images = temporary.newFolder("images")
        val image = File(images, imageName).apply { writeText("image") }
        val metadata = File(temporary.root, "registry")
        val first = registry(metadata, images)
        assertTrue(first.registerCandidate(imageName))
        assertTrue(first.replaceReferences("draft-test", setOf(imageName)))
        now += grace * 2
        val restarted = registry(metadata, images)
        assertTrue(restarted.cleanup { emptySet() })
        assertTrue(image.exists())
        assertTrue(restarted.replaceReferences("draft-test", emptySet()))
        now += grace
        assertTrue(restarted.cleanup { setOf(imageName) })
        assertTrue(image.exists())
        assertTrue(restarted.cleanup { emptySet() })
        assertFalse(image.exists())
    }

    @Test
    fun releasingAnOldDraftStartsANewGracePeriodAndKeepsOtherOwners() {
        val images = temporary.newFolder("images")
        val image = File(images, imageName).apply { writeText("image") }
        val store = registry(File(temporary.root, "registry"), images)
        assertTrue(store.registerCandidate(imageName))
        assertTrue(store.replaceReferences("draft-test", setOf(imageName)))
        assertTrue(store.replaceReferences("undo-1", setOf(imageName)))
        now += grace * 3
        assertTrue(store.replaceReferences("draft-test", emptySet()))
        assertTrue(store.cleanup { emptySet() })
        assertTrue(image.exists())
        now += grace
        assertTrue(store.cleanup { emptySet() })
        assertTrue(image.exists())
        assertTrue(store.releaseOwnersWithPrefix("undo-"))
        now += grace - 1
        assertTrue(store.cleanup { emptySet() })
        assertTrue(image.exists())
        now++
        assertTrue(store.cleanup { emptySet() })
        assertFalse(image.exists())
    }

    @Test
    fun failedReferenceReadAndCorruptMetadataNeverDeleteImages() {
        val images = temporary.newFolder("images")
        val image = File(images, imageName).apply { writeText("image") }
        val metadata = File(temporary.root, "registry")
        val store = registry(metadata, images)
        assertTrue(store.registerCandidate(imageName))
        now += grace
        assertFalse(store.cleanup { error("Database unavailable") })
        assertTrue(image.exists())
        assertTrue(registry(metadata, images).cleanup { setOf(imageName) })
        metadata.writeText("broken metadata")
        assertFalse(registry(metadata, images).cleanup { emptySet() })
        assertTrue(image.exists())
    }

    @Test
    fun failedProtectionWriteDisablesCleanupAndRetainsItsCandidate() {
        val images = temporary.newFolder("images")
        val image = File(images, imageName).apply { writeText("image") }
        val metadata = File(temporary.root, "registry")
        val store = registry(metadata, images)
        assertTrue(store.registerCandidate(imageName))
        now += grace
        File(temporary.root, "registry.tmp").mkdir()
        assertFalse(store.replaceReferences("draft-test", setOf(imageName)))
        assertFalse(store.cleanup { emptySet() })
        assertTrue(image.exists())
    }

    @Test
    fun laterDraftPicksKeepImagesFromEarlierSavedStateCheckpoints() {
        val images = temporary.newFolder("images")
        val previousName = "b".repeat(32) + ".jpg"
        val previous = File(images, previousName).apply { writeText("previous pick") }
        val current = File(images, imageName).apply { writeText("current pick") }
        val metadata = File(temporary.root, "registry")
        val first = registry(metadata, images)
        assertTrue(first.retainDraftReferences("draft-test", setOf(previousName)))
        assertTrue(first.retainDraftReferences("draft-test", setOf(imageName)))
        now += grace * 2
        val restarted = registry(metadata, images)
        assertTrue(restarted.cleanup { emptySet() })
        assertTrue(previous.exists())
        assertTrue(current.exists())
        assertTrue(restarted.replaceReferences("draft-test", emptySet()))
        now += grace
        assertTrue(restarted.cleanup { emptySet() })
        assertFalse(previous.exists())
        assertFalse(current.exists())
    }

    @Test
    fun unsuccessfulDeletionRetainsTheCandidateForTheNextRun() {
        val images = temporary.newFolder("images")
        val image = File(images, imageName).apply { mkdir() }
        val obstacle = File(image, "obstacle").apply { writeText("not a regular image") }
        val metadata = File(temporary.root, "registry")
        val first = registry(metadata, images)
        assertTrue(first.registerCandidate(imageName))
        now += grace
        assertTrue(first.cleanup { emptySet() })
        assertTrue(image.isDirectory)
        assertTrue(obstacle.delete())
        assertTrue(image.delete())
        image.writeText("retry image")
        assertTrue(registry(metadata, images).cleanup { emptySet() })
        assertFalse(image.exists())
    }

    @Test
    fun invalidNamesCannotRegisterOrDeleteOutsideTheImageDirectory() {
        val images = temporary.newFolder("images")
        val outside = temporary.newFile("outside.jpg")
        val store = registry(File(temporary.root, "registry"), images)
        assertFalse(store.registerCandidate("../outside.jpg"))
        now += grace
        assertFalse(store.cleanup { emptySet() })
        assertTrue(outside.exists())
    }
}
