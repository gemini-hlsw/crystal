import org.scalajs.linker.interface.OutputPatterns

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / crossScalaVersions := List("3.6.2")
ThisBuild / tlBaseVersion      := "0.47"

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
      Settings.Libraries.ScalaJSReact.value
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
  // .configs(IntegrationTest)
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
  .jsSettings(
    libraryDependencies ++= {
      Settings.Libraries.ScalaJSReactTest.value.map(_ % Test)
    },
    // libraryDependencies ++= Seq(
    //   ("org.webjars.npm" % "react"     % "18.3.1" % Test).intransitive(),
    //   ("org.webjars.npm" % "react-dom" % "18.3.1" % Test).intransitive()
    // ),
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    Test / scalaJSLinkerConfig ~= {
      // Enable ECMAScript module output.
      _.withModuleKind(ModuleKind.ESModule)
        // Use .mjs extension.
        .withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs"))
    }
  )
  // .jsSettings(
  //   Defaults.itSettings: _*
  // )
  .dependsOn(testkit)
