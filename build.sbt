val zioVersion = "1.0.12"
lazy val root = project
  .in(file("."))
  .settings(
    name         := "scala3-example-project",
    description  := "Example sbt project that compiles using Scala 3",
    version      := "0.1.0",
    scalaVersion := "3.1.0",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-kafka"    % "0.17.1",
      "dev.zio" %% "zio-streams"  % zioVersion,
      "dev.zio" %% "zio-test"     % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      "io.d11"        %% "zhttp"          % "1.0.0.0-RC17",
      "org.typelevel" %% "cats-core"      % "2.6.1",
      "org.typelevel" %% "alleycats-core" % "2.6.1"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
