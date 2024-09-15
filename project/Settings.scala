import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement._

object Settings {

  object LibraryVersions {
    val cats            = "2.12.0"
    val catsEffect      = "3.5.4"
    val discipline      = "1.7.0"
    val disciplineMUnit = "2.0.0"
    val fs2             = "3.11.0"
    val log4Cats        = "2.7.0"
    val lucumaReact     = "0.71.1"
    val monocle         = "3.3.0"
    val mUnit           = "1.0.2"
    val mUnitScalacheck = "1.0.0"
    val mUnitCatsEffect = "2.0.0"
    val scalaCheck      = "1.18.1"
    val scalajsReact    = "3.0.0-beta6"
  }

  object Libraries {
    import LibraryVersions._

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

    val CatsEffectTestkit = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "cats-effect-testkit" % catsEffect
      )
    )

    val CatsLaws = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "cats-laws" % cats
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

    val Fs2JS = Def.setting(
      Seq[ModuleID](
        "co.fs2" %%% "fs2-core" % fs2
      )
    )

    val Log4Cats = Def.setting(
      Seq[ModuleID](
        "org.typelevel" %%% "log4cats-core" % log4Cats
      )
    )

    val Monocle = Def.setting(
      Seq[ModuleID](
        "dev.optics" %%% "monocle-core" % monocle
      )
    )

    val MonocleLaw = Def.setting(
      Seq[ModuleID](
        "dev.optics" %%% "monocle-law" % monocle
      )
    )

    val MonocleMacro = Def.setting(
      Seq[ModuleID](
        "dev.optics" %%% "monocle-macro" % monocle
      )
    )

    val MUnit = Def.setting(
      Seq[ModuleID](
        "org.scalameta" %%% "munit"             % mUnit,
        "org.scalameta" %%% "munit-scalacheck"  % mUnitScalacheck,
        "org.typelevel" %%% "munit-cats-effect" % mUnitCatsEffect
      )
    )

    val LucumaReact = Def.setting(
      Seq[ModuleID](
        "edu.gemini" %%% "lucuma-react-common" % lucumaReact
      )
    )

    val ScalaCheck = Def.setting(
      Seq[ModuleID](
        "org.scalacheck" %%% "scalacheck" % scalaCheck
      )
    )

    val ScalaJSReact = Def.setting(
      Seq[ModuleID](
        "com.github.japgolly.scalajs-react" %%% "core-bundle-cb_io"  % scalajsReact,
        "com.github.japgolly.scalajs-react" %%% "extra"              % scalajsReact,
        "com.github.japgolly.scalajs-react" %%% "extra-ext-monocle3" % scalajsReact
      )
    )

  }

}
