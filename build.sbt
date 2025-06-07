Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / crossScalaVersions := List("3.7.1")
ThisBuild / tlBaseVersion      := "0.48"

ThisBuild / tlCiReleaseBranches := Seq("master")

ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v4"),
    name = Some("Setup Node"),
    params = Map("node-version" -> "22", "cache" -> "npm"),
    cond = Some("matrix.project == 'rootJS'")
  ),
  WorkflowStep.Run(List("npm ci"))
)

lazy val root = tlCrossRootProject.aggregate(core, testkit, tests)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("modules/core"))
  .settings(
    name                                    := "crystal",
    scalacOptions += "-language:implicitConversions",
    // temporary? fix for upgrading to Scala 3.7: https://github.com/scala/scala3/issues/22890
    dependencyOverrides += "org.scala-lang" %% "scala3-library" % scalaVersion.value,
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
    name                                    := "crystal-testkit",
    // temporary? fix for upgrading to Scala 3.7: https://github.com/scala/scala3/issues/22890
    dependencyOverrides += "org.scala-lang" %% "scala3-library" % scalaVersion.value,
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
  .jsSettings(
    libraryDependencies ++= {
      Settings.Libraries.ScalaJSReactTest.value.map(_ % Test)
    },
    jsEnv := new lucuma.LucumaJSDOMNodeJSEnv(),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .dependsOn(testkit)
