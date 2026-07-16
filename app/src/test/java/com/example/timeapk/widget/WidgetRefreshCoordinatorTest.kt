package com.example.timeapk.widget

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRefreshCoordinatorTest {
    @Test
    fun concurrentRefreshesNeverOverlapOrApplyOutOfOrder() = runBlocking {
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val active = AtomicInteger(0)
        val maxActive = AtomicInteger(0)
        val events = mutableListOf<String>()

        val first = async {
            WidgetRefreshCoordinator.runLatestSnapshot {
                maxActive.updateAndGet { maxOf(it, active.incrementAndGet()) }
                events += "first-start"
                firstEntered.complete(Unit)
                releaseFirst.await()
                events += "first-end"
                active.decrementAndGet()
            }
        }
        firstEntered.await()
        val second = async {
            WidgetRefreshCoordinator.runLatestSnapshot {
                maxActive.updateAndGet { maxOf(it, active.incrementAndGet()) }
                events += "second-start"
                secondEntered.complete(Unit)
                events += "second-end"
                active.decrementAndGet()
            }
        }

        assertEquals(false, secondEntered.isCompleted)
        releaseFirst.complete(Unit)
        first.await()
        second.await()

        assertEquals(1, maxActive.get())
        assertEquals(listOf("first-start", "first-end", "second-start", "second-end"), events)
    }
}
