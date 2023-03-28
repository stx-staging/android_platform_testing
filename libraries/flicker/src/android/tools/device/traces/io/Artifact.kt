/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tools.device.traces.io

import android.tools.common.CrossPlatform
import android.tools.common.IScenario
import android.tools.common.io.BUFFER_SIZE
import android.tools.common.io.FLICKER_IO_TAG
import android.tools.common.io.IArtifact
import android.tools.common.io.ResultArtifactDescriptor
import android.tools.common.io.RunStatus
import android.tools.device.traces.deleteIfExists
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class Artifact(
    runStatus: RunStatus,
    private val scenario: IScenario,
    outputDir: File,
    files: Map<ResultArtifactDescriptor, File>
) : IArtifact {
    lateinit var file: File
        private set

    init {
        require(!scenario.isEmpty) { "Scenario shouldn't be empty" }

        createZipFile(outputDir, runStatus, scenario).use { zipOutputStream ->
            files.forEach { (descriptor, artifact) ->
                addFile(zipOutputStream, artifact, nameInArchive = descriptor.fileNameInArtifact)
            }
        }
    }

    override val runStatus: RunStatus
        get() =
            RunStatus.fromFileName(file.name)
                ?: error("Failed to get RunStatus from file name ${file.name}")

    override val path: String
        get() = file.absolutePath

    override fun updateStatus(newStatus: RunStatus) {
        val currFile = file
        val newFile = getNewFilePath(newStatus)
        if (currFile != newFile) {
            IoUtils.moveFile(currFile, newFile)
            file = newFile
        }
    }

    override fun deleteIfExists() {
        file.deleteIfExists()
    }

    override fun hasTrace(descriptor: ResultArtifactDescriptor): Boolean {
        var found = false
        forEachFileInZip { found = found || (it.name == descriptor.fileNameInArtifact) }
        return found
    }

    override fun traceCount(): Int {
        var count = 0
        forEachFileInZip { count++ }
        return count
    }

    override fun toString(): String = file.absolutePath

    private fun createZipFile(
        outputDir: File,
        runStatus: RunStatus,
        scenario: IScenario
    ): ZipOutputStream {
        // Ensure output directory exists
        outputDir.mkdirs()

        val newFileName = runStatus.generateArchiveNameFor(scenario)
        this.file = outputDir.resolve(newFileName)

        require(!existsArchiveFor(outputDir, scenario)) {
            val files =
                try {
                    file.parentFile
                        .listFiles()
                        ?.filterNot { it.isDirectory }
                        ?.map { it.absolutePath }
                } catch (e: Throwable) {
                    null
                }
            "An archive for $scenario already exists in ${file.parentFile.absolutePath}. " +
                "Directory contains ${files?.joinToString()?.ifEmpty { "no files" }}"
        }

        CrossPlatform.log.d(FLICKER_IO_TAG, "Creating artifact archive at $file")
        return ZipOutputStream(BufferedOutputStream(FileOutputStream(file), BUFFER_SIZE))
    }

    private fun existsArchiveFor(outputDir: File, scenario: IScenario): Boolean {
        return RunStatus.values().any {
            outputDir.resolve(it.generateArchiveNameFor(scenario)).exists()
        }
    }

    /** updates the artifact status to [newStatus] */
    private fun getNewFilePath(newStatus: RunStatus): File {
        return file.resolveSibling(newStatus.generateArchiveNameFor(scenario))
    }

    private fun addFile(zipOutputStream: ZipOutputStream, artifact: File, nameInArchive: String) {
        CrossPlatform.log.v(FLICKER_IO_TAG, "Adding $artifact with name $nameInArchive to zip")
        val fi = FileInputStream(artifact)
        val inputStream = BufferedInputStream(fi, BUFFER_SIZE)
        inputStream.use {
            val entry = ZipEntry(nameInArchive)
            zipOutputStream.putNextEntry(entry)
            val data = ByteArray(BUFFER_SIZE)
            var count: Int = it.read(data, 0, BUFFER_SIZE)
            while (count != -1) {
                zipOutputStream.write(data, 0, count)
                count = it.read(data, 0, BUFFER_SIZE)
            }
        }
        zipOutputStream.closeEntry()
        artifact.deleteIfExists()
    }

    @Throws(IOException::class)
    override fun readBytes(descriptor: ResultArtifactDescriptor): ByteArray? {
        CrossPlatform.log.d(FLICKER_IO_TAG, "Reading descriptor=$descriptor from $this")

        var foundFile = false
        val outByteArray = ByteArrayOutputStream()
        val tmpBuffer = ByteArray(BUFFER_SIZE)
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == descriptor.fileNameInArtifact) {
                    val outputStream = BufferedOutputStream(outByteArray, BUFFER_SIZE)
                    try {
                        var size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        while (size > 0) {
                            outputStream.write(tmpBuffer, 0, size)
                            size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        }
                        it.closeEntry()
                    } finally {
                        outputStream.flush()
                        outputStream.close()
                    }
                    foundFile = true
                    break
                }
                zipEntry = it.nextEntry
            }
        }

        return if (foundFile) outByteArray.toByteArray() else null
    }

    private fun withZipFile(predicate: (ZipInputStream) -> Unit) {
        if (!file.exists()) {
            val directory = file.parentFile.absolutePath
            val files =
                try {
                    file.parentFile
                        .listFiles()
                        ?.filterNot { it.isDirectory }
                        ?.map { it.absolutePath }
                } catch (e: Throwable) {
                    null
                }
            throw FileNotFoundException(
                "$file could not be found! " +
                    "Found ${files?.joinToString()?.ifEmpty { "no files" }} in '$directory'."
            )
        }

        val zipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(file), BUFFER_SIZE))
        try {
            predicate(zipInputStream)
        } finally {
            zipInputStream.closeEntry()
            zipInputStream.close()
        }
    }

    private fun forEachFileInZip(predicate: (ZipEntry) -> Unit) {
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                predicate(zipEntry)
                zipEntry = it.nextEntry
            }
        }
    }
}
