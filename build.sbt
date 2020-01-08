import sbtcrossproject.CrossPlugin.autoImport.crossProject

inThisBuild(List(
  name := "crystal",
  scalaVersion := "2.13.1",
  crossScalaVersions := Seq("2.12.10", "2.13.1"),
  organization := "com.rpiaggio",
  homepage := Some(url("https://github.com/rpiaggio/crystal")),
  licenses += ("BSD 3-Clause", url("http://opensource.org/licenses/BSD-3-Clause")),
  developers := List(
    Developer(
      "rpiaggio",
      "Raúl Piaggio",
      "rpiaggio@gmail.com",
      url("http://rpiaggio.com")
    )
  )
))

lazy val root = project.in(file(".")).
  aggregate(crystalJS).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val crystal = crossProject(JVMPlatform, JSPlatform).in(file("."))
  //Settings for all projects
  .settings(
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-encoding", "UTF-8",
      "-P:scalajs:suppressMissingJSGlobalDeprecations"
    ),
//    homepage := Some(url("https://github.com/rpiaggio/crystal")),
//    licenses += ("BSD 3-Clause", url("http://opensource.org/licenses/BSD-3-Clause")),
    scmInfo := Some(ScmInfo(
      url("https://https://github.com/rpiaggio/crystal"),
      "scm:git:git@github.com:rpiaggio/crystal.git",
      Some("scm:git:git@github.com:rpiaggio/crystal.git"))),
//    publishMavenStyle := true,
//    publishTo := sonatypePublishToBundle.value,
//    pomExtra :=
//        <developers>
//          <developer>
//            <id>rpiaggio</id>
//            <name>Raúl Piaggio</name>
//            <url>https://github.com/rpiaggio/</url>
//          </developer>
//        </developers>,
    pomIncludeRepository := { _ => false }
  )
  .jvmSettings(
    libraryDependencies ++=
      Settings.Libraries.CatsJS.value ++
        Settings.Libraries.Fs2JS.value,
    skip in publish := true
  )
  .jsSettings(
    //Scalajs dependencies that are used on the client only
    libraryDependencies ++=
      Settings.Libraries.ReactScalaJS.value ++
        Settings.Libraries.CatsJS.value ++
        Settings.Libraries.CatsEffectsJS.value ++
        Settings.Libraries.Fs2JS.value ++
        Settings.Libraries.Discipline.value
  )

lazy val crystalJS = crystal.js

lazy val crystalJVM = crystal.jvm

sonatypeProfileName := "com.rpiaggio"

packagedArtifacts in root := Map.empty