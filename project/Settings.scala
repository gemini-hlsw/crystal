import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement._

object Settings {

  object LibraryVersions {
    val scalajsReact                = "1.6.0"
    val cats                        = "2.0.0"
    val fs2                         = "2.2.1"
  }

  object Libraries {
    val ReactScalaJS = Def.setting(Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % LibraryVersions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % LibraryVersions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle-cats" % LibraryVersions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats" % LibraryVersions.scalajsReact
    ))

    val CatsJS = Def.setting(Seq[ModuleID](
      "org.typelevel" %%% "cats-core" % LibraryVersions.cats
    ))

    val CatsEffectJS = Def.setting(Seq[ModuleID](
      "org.typelevel" %%% "cats-effect" % LibraryVersions.cats      
    ))

    val Fs2JS = Def.setting(Seq[ModuleID](
      "co.fs2" %%% "fs2-core" % LibraryVersions.fs2
    ))
  }

}