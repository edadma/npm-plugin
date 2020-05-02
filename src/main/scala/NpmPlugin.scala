package xyz.hyperreal.npm_plugin

import java.nio.file.{Files, Paths, StandardCopyOption}

import collection.JavaConverters._

import play.api.libs.json._
import sbt._

object NpmPlugin extends AutoPlugin {

  object autoImport {
    val npmPackage = taskKey[Unit]("Create NPM package")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    npmPackage := {
      val nameval = Keys.name.value
      val versionval = Keys.version.value
      val descriptionval = Keys.description.value
      val licenceval = Keys.licenses.value.head._1
      val target = Keys.target.value.toPath

      val src =
        Files.walk(target).iterator.asScala find { p =>
          p.toString endsWith "-opt.js"
        } match {
          case None =>
            sys.error(
              "couldn't find a compiler output file ending in '-opt.js'")
          case Some(js) => js
        }
      val deps =
        Files.walk(target).iterator.asScala find { p =>
          p.toString endsWith "package.json"
        } match {
          case None =>
            sys.error("couldn't find a 'package.json' file")
          case Some(pkg) =>
            (Json.parse(Files.readString(pkg)) \ "dependencies").get
              .asInstanceOf[JsObject]
              .fields map {
              case (k, v) => (k, v.as[String])
            } toList
        }

      val dst = target resolve "npm"

      if (Files.exists(dst) && !Files.isDirectory(dst))
        sys.error(
          s"destination path already exists but is not a directory: $dst")

      if (!Files.exists(dst))
        Files.createDirectories(dst)

      val packageJson = dst resolve "package.json"

      def depsmap(d: List[(String, String)]) =
        d map { case (p, v) => s""""$p": "$v"""" } mkString ",\n    "

      val depsMembers = depsmap(deps)
      val contents =
        s"""
         |{
         |  "name": "$nameval",
         |  "version": "$versionval",
         |  "description": "$descriptionval",
         |  "main": "index.js",
         |  "scripts": {
         |    "test": "echo \\"Error: no test specified\\" && exit 1"
         |  },
         |  "author": "",
         |  "license": "$licenceval",
         |  "dependencies": {
         |    $depsMembers
         |  },
         |  "devDependencies": {
         |
         |  }
         |}
         |""".trim.stripMargin

      Files.writeString(packageJson, contents)
      Files.copy(src,
                 dst resolve "index.js",
                 StandardCopyOption.REPLACE_EXISTING)
    }
  )

}
