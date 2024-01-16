val scala3Version = "3.4.0-RC1"

lazy val commonSettings = Seq(
    javaOptions ++= Seq(
      // "-XX:-DetectLocksInCompiledFrames",
      "-XX:+UnlockDiagnosticVMOptions",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+UseNewCode"
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

lazy val catsInterop = project
  .in(file("cats-interop"))
  .settings(commonSettings)
  .settings(
    name := "lynx-cats-interop",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.typelevel" %% "cats-effect" % "3.2.9",
      "org.typelevel" %% "munit-cats-effect" % "2.0.0-M4" % Test
    )
  )
  .dependsOn(core)

lazy val enableContinuations = Seq(
  // forking is necessary in order to enable the access of the internal vm types below.
  fork := true,

  // enable access of jdk.internal.vm.{ Continuation, ContinuationScope }
  javaOptions ++= Seq(
    "--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED",
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(core, catsInterop)
