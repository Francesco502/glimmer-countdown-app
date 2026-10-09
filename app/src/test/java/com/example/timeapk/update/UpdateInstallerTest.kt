package com.example.timeapk.update

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateInstallerTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun completeDownloadReplacesPreviousApkForKnownAndUnknownLengths() = runBlocking {
        val bytes = "new complete APK".toByteArray()
        listOf(bytes.size.toLong(), -1L).forEach { length ->
            val apk = previousApk()

            writeDownloadedApk(ByteArrayInputStream(bytes), apk, length)

            assertArrayEquals(bytes, apk.readBytes())
            assertFalse(partialFile(apk).exists())
        }
    }

    @Test
    fun invalidOrIncompleteDownloadKeepsPreviousApkAndRemovesPartialFile() {
        val bytes = "new complete APK".toByteArray()
        listOf(
            bytes to 0L,
            bytes to -2L,
            bytes to bytes.size.toLong() + 1L,
            bytes to bytes.size.toLong() - 1L,
            byteArrayOf() to -1L
        ).forEach { (content, length) ->
            val apk = previousApk()

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { writeDownloadedApk(ByteArrayInputStream(content), apk, length) }
            }

            assertPreviousApkIntact(apk)
        }
    }

    @Test
    fun interruptedStreamKeepsPreviousApkAndRemovesPartialFile() {
        val apk = previousApk()
        val failure = IOException("Connection interrupted")
        val input = object : ByteArrayInputStream("partial APK".toByteArray()) {
            var reads = 0

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (reads++ > 0) throw failure
                return super.read(buffer, offset, length)
            }
        }

        val actual = assertThrows(IOException::class.java) {
            runBlocking { writeDownloadedApk(input, apk, -1L) }
        }

        assertSame(failure, actual)
        assertPreviousApkIntact(apk)
    }

    @Test
    fun cancellationDuringDownloadIsPropagatedWithoutReplacingPreviousApk() {
        val apk = previousApk()
        val job = Job()
        val input = object : ByteArrayInputStream("partial APK".toByteArray()) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                val read = super.read(buffer, offset, length)
                job.cancel(CancellationException("Page closed"))
                return read
            }
        }

        assertThrows(CancellationException::class.java) {
            runBlocking(job) { writeDownloadedApk(input, apk, -1L) }
        }

        assertPreviousApkIntact(apk)
    }

    @Test
    fun non200HttpResponsesKeepPreviousApk() {
        listOf(206, 404, 500).forEach { status ->
            val apk = previousApk()
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("Download rejected")
                    .body("response body".toResponseBody())
                    .build()
            }.build()
            val call = client.newCall(Request.Builder().url("https://example.test/update.apk").build())

            assertThrows(IllegalStateException::class.java) {
                runBlocking { downloadApk(call, apk) }
            }

            assertPreviousApkIntact(apk)
        }
    }

    @Test(timeout = 10_000)
    fun cancellationClosesBlockingHttpDownloadAndRemovesPartialFile() = runBlocking {
        val apk = previousApk()
        val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val releaseServer = CountDownLatch(1)
        val serverThread = thread(isDaemon = true) {
            server.use {
                it.accept().use { socket ->
                    socket.soTimeout = 5_000
                    val request = socket.getInputStream().bufferedReader()
                    while (request.readLine()?.isNotBlank() == true) Unit
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\nConnection: close\r\n\r\npartial".toByteArray())
                        flush()
                    }
                    releaseServer.await(5, TimeUnit.SECONDS)
                }
            }
        }
        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build()
        val call = client.newCall(
            Request.Builder().url("http://localhost:${server.localPort}/update.apk").build()
        )
        val download = launch(Dispatchers.IO) { downloadApk(call, apk) }
        try {
            withTimeout(2_000) {
                while (partialFile(apk).length() == 0L) delay(10)
            }

            download.cancel(CancellationException("Page closed"))
            withTimeout(2_000) { download.join() }

            assertTrue(call.isCanceled())
            assertTrue(download.isCancelled)
            assertPreviousApkIntact(apk)
        } finally {
            releaseServer.countDown()
            server.close()
            serverThread.join(2_000)
        }
    }

    private fun previousApk(): File = File(temp.newFolder(), "update.apk").apply {
        writeText("previous complete APK")
    }

    private fun partialFile(apk: File) = File(apk.parentFile, "${apk.name}.part")

    private fun assertPreviousApkIntact(apk: File) {
        assertEquals("previous complete APK", apk.readText())
        assertFalse(partialFile(apk).exists())
    }
}
