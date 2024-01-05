val scala3Version = "3.3.1"

lazy val commonSettings = Seq(
    javaOptions ++= Seq(
      "--add-exports java.base/jdk.internal.vm=ALL-UNNAMED",
      "-XX:-DetectLocksInCompiledFrames",
      "-XX:+UnlockDiagnosticVMOptions",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+UseNewCode"
    ),
    Test / fork := true
)


lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "lynx",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )

lazy val root = project
  .in(file("."))
  .aggregate(core)


core / Runtime / run / javaOptions += "--add-exports java.base/jdk.internal.vm=ALL-UNNAMED"
core / Test / run / javaOptions += "--add-exports java.base/jdk.internal.vm=ALL-UNNAMED"
