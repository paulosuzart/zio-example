lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3-example-project",
    description := "Example sbt project that compiles using Scala 3",
    version := "0.1.0",
    scalaVersion := "3.1.0",
    libraryDependencies += "dev.zio" %% "zio" % "1.0.12",
    libraryDependencies += "io.d11" %% "zhttp" % "1.0.0.0-RC17"
  )
