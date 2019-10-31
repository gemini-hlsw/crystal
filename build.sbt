import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "crystal"

version := "0.0.1"

scalaVersion := "2.12.10"

crossScalaVersions := Seq("2.12.10", "2.13.1")

def BaseProject(name: String): Project =
  Project(name, file(name))
    .settings(
      organization := "com.rpiaggio",
      //      version := "1.1.3",
      scalacOptions ++= Seq(
        "-deprecation",
        "-feature",
        "-Xfatal-warnings",
        "-encoding", "UTF-8",
        "-P:scalajs:suppressMissingJSGlobalDeprecations"
      ),
      homepage := Some(url("https://github.com/rpiaggio/crystal")),
      licenses += ("BSD 3-Clause", url("http://opensource.org/licenses/BSD-3-Clause")),
      scmInfo := Some(ScmInfo(
        url("https://https://github.com/rpiaggio/crystal"),
        "scm:git:git@github.com:rpiaggio/crystal.git",
        Some("scm:git:git@github.com:rpiaggio/crystal.git"))),
      publishMavenStyle := true,
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },
      pomExtra :=
        <developers>
          <developer>
            <id>rpiaggio</id>
            <name>Raúl Piaggio</name>
            <url>https://github.com/rpiaggio/</url>
          </developer>
        </developers>,
      pomIncludeRepository := { _ => false }
    )
    .enablePlugins(ScalaJSPlugin)


lazy val root = project.in(file(".")).
  aggregate(crystalJS).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val crystal = crossProject(JSPlatform).in(file(".")).
  //Settings for all projects
  settings(
    name := "crystal",
    publishTo := Some(Resolver.file("file", new File("../maven-repo")))
  ).jsSettings(
  //Scalajs dependencies that are used on the client only
  libraryDependencies ++=
    Settings.Libraries.ReactScalaJS.value ++
      Settings.Libraries.CatsJS.value ++
      Settings.Libraries.Fs2JS.value
)

lazy val crystalJS = crystal.js

sonatypeProfileName := "com.rpiaggio"

packagedArtifacts in root := Map.empty