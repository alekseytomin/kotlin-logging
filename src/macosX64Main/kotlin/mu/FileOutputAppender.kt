package mu

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

class FileOutputAppender(private val fileName: String) : Appender {

    private val linesRef = AtomicReference(listOf<String>().freeze())

    init {
        // Check file
        fclose(fopen(fileName, "a")
            ?: throw RuntimeException("Can't open for write file: $fileName"))
        Worker.start().execute(TransferMode.SAFE, { Pair(fileName, linesRef) }) {
            val emptyList = listOf<String>().freeze()
            val fileName = it.first
            val linesRef = it.second
            while (true) {
                val oldLines = linesRef.value
                try {
                    if (linesRef.compareAndSet(oldLines, emptyList)) {
                        if (oldLines.isNotEmpty()) {
                            val cstr = "${oldLines.joinToString(separator = "\n")}\n"
                            val fp = fopen(fileName, "a")
                                ?: throw RuntimeException("Can't open for write file: $fileName")
                            fputs(cstr, fp)
                            fclose(fp)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                // Yes, it looks like ugly.
                // sleep doesn't work.
                // delay works only in coroutine context.
                runBlocking {
                    delay(100)
                }
            }
        }
    }

    private fun log(message: Any?) {
        val line = message.toString()
        while (true) {
            val oldLines = linesRef.value
            val newLines = ArrayList(oldLines)
            newLines.add(line)
            if (linesRef.compareAndSet(oldLines, newLines.freeze())) {
                return
            }
        }
    }

    override fun trace(message: Any?) = log(message)
    override fun debug(message: Any?) = log(message)
    override fun info(message: Any?) = log(message)
    override fun warn(message: Any?) = log(message)
    override fun error(message: Any?) = log(message)
}
