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
  aggregate(crystalJVM, crystalJS).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val crystal = crossProject(JVMPlatform, JSPlatform).in(file("."))
  .settings(
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-encoding", "UTF-8"
    ),
    libraryDependencies ++=
      Settings.Libraries.CatsJS.value ++
        Settings.Libraries.CatsEffectJS.value ++
        Settings.Libraries.Fs2JS.value ++
        Settings.Libraries.Monocle.value,
    scmInfo := Some(ScmInfo(
      url("https://https://github.com/rpiaggio/crystal"),
      "scm:git:git@github.com:rpiaggio/crystal.git",
      Some("scm:git:git@github.com:rpiaggio/crystal.git"))),
    pomIncludeRepository := { _ => false }
  )
  .jsSettings(
    scalacOptions ++= Seq(
      "-P:scalajs:suppressMissingJSGlobalDeprecations"
    ),
    libraryDependencies ++=
      Settings.Libraries.ReactScalaJS.value
  )

lazy val crystalJS = crystal.js

lazy val crystalJVM = crystal.jvm

sonatypeProfileName := "com.rpiaggio"

packagedArtifacts in root := Map.empty