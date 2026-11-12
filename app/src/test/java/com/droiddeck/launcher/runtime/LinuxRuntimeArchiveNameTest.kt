package com.droiddeck.launcher.runtime

import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LinuxRuntimeArchiveNameTest {
    private fun release(version: String, sha: String) =
        LinuxRuntimeInstaller.Release(version, "https://example.invalid/runtime.tar.zst", sha, 1L)

    @Test fun sameReleaseKeepsTheSameName() {
        val a = LinuxRuntimeInstaller.archiveName(release("r9", "ab".repeat(32)))
        val b = LinuxRuntimeInstaller.archiveName(release("r9", "ab".repeat(32)))
        assertEquals(a, b)
    }

    @Test fun anotherVersionOrChecksumGetsAnotherName() {
        val base = LinuxRuntimeInstaller.archiveName(release("r9", "ab".repeat(32)))
        assertNotEquals(base, LinuxRuntimeInstaller.archiveName(release("r10", "ab".repeat(32))))
        assertNotEquals(base, LinuxRuntimeInstaller.archiveName(release("r9", "cd".repeat(32))))
    }

    @Test fun nameIsASafeFileNameThatTheCleanupRecognises() {
        val name = LinuxRuntimeInstaller.archiveName(release("r9/../x y", "AB/CD ".repeat(10)))
        assertFalse(name.contains('/'))
        assertFalse(name.contains(' '))
        assertTrue(name.startsWith("linuxfs"))
        assertTrue(name.endsWith(".tar.zst"))
    }

    @Test fun shortOrEmptyChecksumStillMakesAName() {
        assertEquals("linuxfs-r9-.tar.zst", LinuxRuntimeInstaller.archiveName(release("r9", "")))
    }

    @Test fun copiesAnArchiveOfTheCatalogSize() {
        val bytes = ByteArray(96 * 1024) { it.toByte() }
        val output = ByteArrayOutputStream()
        LinuxRuntimeInstaller.copyArchive(bytes.inputStream(), output, bytes.size.toLong())
        assertArrayEquals(bytes, output.toByteArray())
    }

    @Test fun rejectsWrongArchiveSizesAndBoundsTheCopy() {
        val bytes = ByteArray(96 * 1024)
        val output = ByteArrayOutputStream()
        assertThrows(IOException::class.java) {
            LinuxRuntimeInstaller.copyArchive(bytes.inputStream(), output, 64 * 1024L)
        }
        assertEquals(64 * 1024, output.size())
        assertThrows(IOException::class.java) {
            LinuxRuntimeInstaller.copyArchive(bytes.inputStream(), ByteArrayOutputStream(), bytes.size + 1L)
        }
    }
}
