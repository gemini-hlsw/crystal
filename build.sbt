Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / crossScalaVersions := List("3.4.3")
ThisBuild / tlBaseVersion      := "0.42"

ThisBuild / tlCiReleaseBranches := Seq("master")

lazy val root = tlCrossRootProject.aggregate(core, testkit, tests)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("modules/core"))
  .settings(
    name := "crystal",
    scalacOptions += "-language:implicitConversions",
    libraryDependencies ++=
      Settings.Libraries.CatsJS.value ++
        Settings.Libraries.CatsEffectJS.value ++
        Settings.Libraries.Fs2JS.value ++
        Settings.Libraries.Monocle.value ++
        Settings.Libraries.Log4Cats.value
  )
  .jsSettings(
    libraryDependencies ++= {
      Settings.Libraries.ScalaJSReact.value ++
        Settings.Libraries.LucumaReact.value
    }
  )

lazy val testkit = crossProject(JVMPlatform, JSPlatform)
  .in(file("modules/testkit"))
  .settings(
    name := "crystal-testkit",
    libraryDependencies ++=
      Settings.Libraries.ScalaCheck.value
  )
  .dependsOn(core)

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .in(file("modules/tests"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    name := "crystal-tests",
    libraryDependencies ++=
      (Settings.Libraries.MUnit.value ++
        Settings.Libraries.Discipline.value ++
        Settings.Libraries.DisciplineMUnit.value ++
        Settings.Libraries.CatsLaws.value ++
        Settings.Libraries.CatsEffectTestkit.value ++
        Settings.Libraries.MonocleMacro.value ++
        Settings.Libraries.MonocleLaw.value).map(_ % Test)
  )
  .dependsOn(testkit)
