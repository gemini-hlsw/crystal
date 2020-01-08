import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement._

object Settings {

  object LibraryVersions {
    val scalajsReact                = "1.5.0"
    val cats                        = "2.0.0"
    val fs2                         = "2.0.0"

    val catsTestkitScalatest        = "1.0.0-RC1"
    val disciplineScalatest         = "1.0.0-RC1"
  }

  object Libraries {
    val ReactScalaJS = Def.setting(Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % LibraryVersions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % LibraryVersions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle-cats" % LibraryVersions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats" % LibraryVersions.scalajsReact
    ))

    val CatsJS = Def.setting(Seq[ModuleID](
      "org.typelevel" %%% "cats-core" % LibraryVersions.cats,
      "org.typelevel" %%% "cats-testkit" % LibraryVersions.cats % "test",
      "org.typelevel" %%% "cats-testkit-scalatest" % LibraryVersions.catsTestkitScalatest % "test"
    ))

    val CatsEffectsJS = Def.setting(Seq[ModuleID](
      "org.typelevel" %%% "cats-effect" % LibraryVersions.cats,
      "org.typelevel" %%% "cats-effect-laws" % LibraryVersions.cats % "test"
    ))

    val Fs2JS = Def.setting(Seq[ModuleID](
      "co.fs2" %%% "fs2-core" % LibraryVersions.fs2
    ))

    val Discipline = Def.setting(Seq[ModuleID](
      "org.scalatest" %%% "scalatest" % "3.1.0-RC2" % "test",
      "org.typelevel" %%% "discipline-scalatest" % LibraryVersions.disciplineScalatest % "test"
    ))
  }

}