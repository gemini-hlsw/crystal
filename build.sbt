Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / crossScalaVersions := List("3.3.0")
ThisBuild / tlBaseVersion      := "0.34"

ThisBuild / tlCiReleaseBranches := Seq("master")

lazy val root = tlCrossRootProject.aggregate(crystal)

lazy val crystal = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .settings(
    scalacOptions += "-language:implicitConversions",
    libraryDependencies ++=
      Settings.Libraries.CatsJS.value ++
        Settings.Libraries.CatsEffectJS.value ++
        Settings.Libraries.Fs2JS.value ++
        Settings.Libraries.Monocle.value ++
        Settings.Libraries.Log4Cats.value ++
        (
          Settings.Libraries.MUnit.value ++
            Settings.Libraries.Discipline.value ++
            Settings.Libraries.DisciplineMUnit.value ++
            Settings.Libraries.CatsLaws.value ++
            Settings.Libraries.MonocleMacro.value ++
            Settings.Libraries.MonocleLaw.value
        ).map(_ % Test)
  )
  .jsSettings(
    libraryDependencies ++= {
      Settings.Libraries.ScalaJSReact.value ++ (if (scalaBinaryVersion.value == "3")
                                                  Settings.Libraries.LucumaReact.value
                                                else Settings.Libraries.ReactCommon.value)
    }
  )
