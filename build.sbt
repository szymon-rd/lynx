val scala3Version = "3.3.1"

lazy val commonSettings = Seq(
    javaOptions ++= Seq(
      // "-XX:-DetectLocksInCompiledFrames",
      // "-XX:+UnlockDiagnosticVMOptions",
      // "-XX:+UnlockExperimentalVMOptions",
      // "-XX:+UseNewCode"
    )
) ++ enableContinuations


lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "lynx",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )

lazy val enableContinuations = Seq(
  // forking is necessary in order to enable the access of the internal vm types below.
  fork := true,

  // enable access of jdk.internal.vm.{ Continuation, ContinuationScope }
  javaOptions ++= Seq(
    "--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED"
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(core)
