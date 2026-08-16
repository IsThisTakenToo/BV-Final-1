package com.spotvault.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the two drawable mistakes that break DropPin Vault at runtime rather than
 * at compile time:
 *
 *  1. Malformed `android:pathData` — Android's PathParser throws while inflating,
 *     which makes launchers fall back to the stock Android icon.
 *  2. Passing a non-vector, non-raster drawable (layer-list, inset, shape, ...) to
 *     Compose `painterResource`, which throws and kills the activity on launch.
 */
class DrawableResourceIntegrityTest {

    private val argumentCounts = mapOf(
        'M' to 2, 'L' to 2, 'T' to 2,
        'H' to 1, 'V' to 1,
        'C' to 6, 'S' to 4, 'Q' to 4,
        'A' to 7,
        'Z' to 0
    )

    private val numberPattern = Regex("""[-+]?(?:\d*\.\d+(?:[eE][-+]?\d+)?|\d+\.?(?:[eE][-+]?\d+)?)""")
    private val commandChars = "MmLlHhVvCcSsQqTtAaZz".toSet()
    private val pathDataPattern = Regex("""android:pathData\s*=\s*"([^"]*)"""")
    private val painterResourcePattern = Regex("""painterResource\(\s*R\.drawable\.(\w+)""")

    private val moduleRoot: File by lazy {
        val workingDir = System.getProperty("user.dir") ?: "."
        generateSequence(File(workingDir)) { it.parentFile }
            .firstOrNull { File(it, "src/main/res").isDirectory }
            ?: error("Could not locate app/src/main/res from $workingDir")
    }

    private val resDir: File get() = File(moduleRoot, "src/main/res")
    private val javaDir: File get() = File(moduleRoot, "src/main/java")

    @Test
    fun everyVectorPathIsParsable() {
        val failures = mutableListOf<String>()
        var checked = 0

        resDir.walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .forEach { file ->
                val text = file.readText()
                if (!text.contains("<vector")) return@forEach

                pathDataPattern.findAll(text).forEach { match ->
                    checked++
                    val pathData = match.groupValues[1]
                    validatePathData(pathData).forEach { problem ->
                        failures += "${file.relativeTo(resDir)}: $problem\n    pathData: $pathData"
                    }
                }
            }

        assertTrue("Expected to find vector paths to validate", checked > 0)
        assertTrue(
            "Invalid VectorDrawable pathData found:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun painterResourceOnlyReferencesVectorsOrBitmaps() {
        val failures = mutableListOf<String>()

        javaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { source ->
                painterResourcePattern.findAll(source.readText()).forEach { match ->
                    val name = match.groupValues[1]
                    val problem = describeUnsupportedPainter(name)
                    if (problem != null) {
                        failures += "${source.name} -> R.drawable.$name: $problem"
                    }
                }
            }

        assertTrue(
            "Compose painterResource only supports VectorDrawable and raster images:\n" +
                failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    private fun describeUnsupportedPainter(drawableName: String): String? {
        val candidates = resDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("drawable") }
            ?.flatMap { dir -> dir.listFiles()?.toList() ?: emptyList() }
            ?.filter { it.nameWithoutExtension == drawableName }
            ?: emptyList()

        if (candidates.isEmpty()) return "drawable resource not found"

        val raster = setOf("png", "jpg", "jpeg", "webp")
        candidates.forEach { file ->
            if (file.extension.lowercase() in raster) return null
            if (file.extension.lowercase() == "xml") {
                val rootTag = file.readText()
                    .substringAfter("<", "")
                    .let { Regex("""^\s*([\w.-]+)""").find(it)?.groupValues?.get(1) }
                    ?.takeIf { it != "?xml" }
                    ?: file.readText()
                        .lineSequence()
                        .mapNotNull { Regex("""<([\w.-]+)""").find(it)?.groupValues?.get(1) }
                        .firstOrNull { it != "?xml" }

                if (rootTag == "vector") return null
                return "root element is <$rootTag>, which painterResource cannot rasterize"
            }
        }
        return "unsupported drawable type"
    }

    private fun validatePathData(pathData: String): List<String> {
        val problems = mutableListOf<String>()
        val segments = mutableListOf<Pair<Char, String>>()

        var index = 0
        while (index < pathData.length) {
            val char = pathData[index]
            when {
                char in commandChars -> {
                    var end = index + 1
                    while (end < pathData.length && pathData[end] !in commandChars) end++
                    segments += char to pathData.substring(index + 1, end)
                    index = end
                }
                char.isWhitespace() || char == ',' -> index++
                else -> {
                    problems += "unexpected character '$char'"
                    index++
                }
            }
        }

        if (segments.isEmpty()) return listOf("empty pathData")
        if (segments.first().first !in "Mm") {
            problems += "must start with M or m, found '${segments.first().first}'"
        }

        segments.forEach { (command, rawArgs) ->
            val expected = argumentCounts.getValue(command.uppercaseChar())
            val args = numberPattern.findAll(rawArgs).count()
            val stray = numberPattern.replace(rawArgs, " ").replace(",", "").trim()

            if (stray.isNotEmpty()) {
                problems += "command '$command' has stray characters '$stray'"
            }
            if (expected == 0) {
                if (args != 0) problems += "command '$command' takes no arguments but found $args"
            } else if (args == 0 || args % expected != 0) {
                problems += "command '$command' needs a multiple of $expected arguments but found $args"
            }
        }

        return problems
    }
}
