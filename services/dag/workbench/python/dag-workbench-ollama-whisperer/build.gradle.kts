import org.gradle.internal.os.OperatingSystem

plugins { base }

group = "org.ubiquia"
version = "0.1.0"

fun shCmd(vararg parts: String): List<String> =
    if (OperatingSystem.current().isWindows) listOf("cmd", "/c") + parts.toList()
    else listOf("bash", "-lc", parts.joinToString(" "))

val uvBuild by tasks.registering(Exec::class) {
    workingDir = project.projectDir
    // do NOT call bash on Windows
    commandLine(shCmd("uv build"))
}

tasks.named("build") { dependsOn(uvBuild) }
