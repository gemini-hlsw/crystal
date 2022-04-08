import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement._

object Settings {

  object LibraryVersions {
    val scalajsReact    = "2.0.1"
    val cats            = "2.7.0"
    val catsEffect      = "3.3.11"
    val fs2             = "3.2.7"
    val monocle         = "3.1.0"
    val log4Cats        = "2.2.0"
    val mUnit           = "0.7.29"
    val mUnitCatsEffect = "1.0.7"
    val discipline      = "1.4.0"
    val disciplineMUnit = "1.0.9"
  }

  object Libraries {
    import LibraryVersions._

    val ReactScalaJS = Def.setting(
      Seq(
        "com.github.japgolly.scalajs-react" %%% "core-bundle-cb_io"  % scalajsReact,
        "com.github.japgolly.scalajs-react" %%% "extra"              % scalajsReact,
        "com.github.japgolly.scalajs-react" %%% "extra-ext-monocle3" % scalajsReact
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
        "dev.optics" %%% "monocle-core" % monocle
      )
    )

    val MonocleMacro = Def.setting(
      Seq[ModuleID](
        "dev.optics" %%% "monocle-macro" % monocle
      )
    )

    val Log4Cats = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "log4cats-core" % log4Cats
      )
    )

    val MUnit = Def.setting(
      Seq[ModuleID](
        "org.scalameta" %%% "munit"               % mUnit,
        "org.scalameta" %%% "munit-scalacheck"    % mUnit,
        "org.typelevel" %%% "munit-cats-effect-3" % mUnitCatsEffect
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
