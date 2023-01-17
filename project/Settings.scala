import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement._

object Settings {

  object LibraryVersions {
    val cats            = "2.9.0"
    val catsEffect      = "3.4.5"
    val discipline      = "1.5.1"
    val disciplineMUnit = "1.0.9"
    val fs2             = "3.5.0"
    val log4Cats        = "2.5.0"
    val monocle         = "3.2.0"
    val mUnit           = "0.7.29"
    val mUnitCatsEffect = "1.0.7"
    val reactCommon     = "0.24.0"
    val lucumaReact     = "0.24.0"
    val scalajsReact    = "2.1.1"
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
        "org.scalameta" %%% "munit"               % mUnit,
        "org.scalameta" %%% "munit-scalacheck"    % mUnit,
        "org.typelevel" %%% "munit-cats-effect-3" % mUnitCatsEffect
      )
    )

    val ReactCommon = Def.setting(
      Seq[ModuleID](
        "io.github.cquiroz.react" %%% "common" % reactCommon,
        "io.github.cquiroz.react" %%% "cats"   % reactCommon
      )
    )

    val LucumaReact = Def.setting(
      Seq[ModuleID](
        "edu.gemini" %%% "lucuma-react-common" % lucumaReact
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
