import org.gradle.internal.os.OperatingSystem

plugins { base }

group = "org.ubiquia"
version = "0.1.0"

fun shCmd(vararg parts: String): List<String> =
    if (OperatingSystem.current().isWindows) listOf("cmd", "/c") + parts.toList()
    else listOf("bash", "-lc", parts.joinToString(" "))

val uvBuild by tasks.registering(Exec::class) {
    workingDir = project.projectDir
    commandLine(shCmd("uv build"))
}

val testPython by tasks.registering(Exec::class) {
    workingDir = project.projectDir
    environment("PYTHONPATH", "src")

    commandLine(shCmd("uv run --extra dev -m pytest -vv -s --maxfail=1 tests"))

    standardOutput = System.out
    errorOutput = System.err

    mustRunAfter(uvBuild)
}

tasks.named("build") { dependsOn(uvBuild) }
tasks.named("check") { dependsOn(testPython) }
