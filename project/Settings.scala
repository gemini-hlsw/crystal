import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement._

object Settings {

  object LibraryVersions {
    val scalajsReact    = "1.7.1"
    val cats            = "2.1.1"
    val catsEffect      = "2.1.3"
    val fs2             = "2.4.2"
    val monocle         = "2.0.5"
    val log4Cats        = "1.1.1"
    val mUnit           = "0.7.9"
    val discipline      = "1.0.2"
    val disciplineMUnit = "0.2.2"
  }

  object Libraries {
    import LibraryVersions._

    val ReactScalaJS = Def.setting(
      Seq(
        "com.github.japgolly.scalajs-react" %%% "core"             % scalajsReact,
        "com.github.japgolly.scalajs-react" %%% "extra"            % scalajsReact,
        "com.github.japgolly.scalajs-react" %%% "ext-monocle-cats" % scalajsReact,
        "com.github.japgolly.scalajs-react" %%% "ext-cats"         % scalajsReact
      )
    )

    val CatsJS = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "cats-core" % cats
      )
    )

    val CatsEffectJS = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "cats-effect" % catsEffect
      )
    )

    val Fs2JS = Def.setting(
      Seq[ModuleID](
        "co.fs2" %%% "fs2-core" % fs2
      )
    )

    val Monocle = Def.setting(
      Seq[ModuleID](
        "com.github.julien-truffaut" %%% "monocle-core" % monocle
      )
    )

    val Log4Cats = Def.setting(
      Seq[ModuleID](
        "io.chrisdavenport" %%% "log4cats-core" % log4Cats
      )
    )

    val MUnit = Def.setting(
      Seq[ModuleID](
        "org.scalameta" %%% "munit"            % mUnit,
        "org.scalameta" %%% "munit-scalacheck" % mUnit
      )
    )

    val Discipline = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "discipline-core" % discipline
      )
    )

    val DisciplineMUnit = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "discipline-munit" % disciplineMUnit
      )
    )

    val CatsLaws = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "cats-laws" % cats
      )
    )
  }

}
