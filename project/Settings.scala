import sbt.Def
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.librarymanagement.ModuleID

object Settings {

  object LibraryVersions {
    val scalajsReact  = "1.5.0-RC2"
    val cats          = "2.0.0"
    val fs2           = "2.0.0"
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
      "org.typelevel" %%% "cats-effect" % LibraryVersions.cats
    ))

    val Fs2JS = Def.setting(Seq[ModuleID](
      "co.fs2" %%% "fs2-core" % LibraryVersions.fs2
    ))
  }

}