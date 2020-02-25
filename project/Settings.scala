import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement._

object Settings {

  object LibraryVersions {
    val scalajsReact                = "1.6.0"
    val cats                        = "2.1.1"
    val catsEffect                  = "2.1.1"
    val fs2                         = "2.2.2"
    val monocle                     = "2.0.2"
    val log4Cats                    = "1.0.1"
    val log4CatsLog4s               = "0.4.0-M1"
  }

  object Libraries {
    import LibraryVersions._

    val ReactScalaJS = Def.setting(Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle-cats" % scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats" % scalajsReact
    ))

    val CatsJS = Def.setting(Seq[ModuleID](
      "org.typelevel" %%% "cats-core" % cats
    ))

    val CatsEffectJS = Def.setting(Seq[ModuleID](
      "org.typelevel" %%% "cats-effect" % catsEffect
    ))

    val Fs2JS = Def.setting(Seq[ModuleID](
      "co.fs2" %%% "fs2-core" % fs2
    ))

    val Monocle = Def.setting(Seq[ModuleID](
      "com.github.julien-truffaut" %%% "monocle-core" % monocle
    ))

    val Log4Cats = Def.setting(Seq[ModuleID](
      "io.chrisdavenport" %%% "log4cats-core" % log4Cats,
      "io.chrisdavenport" %%% "log4cats-log4s" % log4CatsLog4s
    ))

  }

}