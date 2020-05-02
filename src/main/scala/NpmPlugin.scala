package xyz.hyperreal.npm_plugin

import java.nio.file.{Files, StandardCopyOption}
import sys.process._
import collection.JavaConverters._

import play.api.libs.json._
import sbt._

object NpmPlugin extends AutoPlugin {

  object autoImport {
    val npmPublish = taskKey[Unit]("Publish NPM package")
    val npmPackage = taskKey[Unit]("Create NPM package")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    npmPublish := {
      npmPackage.value

      val dst = Keys.target.value.toPath resolve "npm"

      Process("npm publish", dst.toFile) !
    },
    npmPackage := {
      val name = Keys.name.value
      val src = Keys.sourceDirectory.value
      val version = Keys.version.value
      val description = Keys.description.value
      val licence = Keys.licenses.value.head._1
      val target = Keys.target.value.toPath
      val out = s"$name-opt.js"
      val emitted =
        Files.walk(target).iterator.asScala find { p =>
          p.getFileName.toString == out
        } match {
          case None =>
            sys.error(s"Couldn't find compiler output file '$out'")
          case Some(js) =>
            println(s"Found apparent compiler output file '$js'")
            js
        }
      val emittedFolder = emitted.getParent
      val dst = target resolve "npm"
      val emittedPkg = emittedFolder resolve "package.json"

      if (!Files.exists(emittedPkg))
        sys.error(
          s"Compiler generated 'package.json' file not found under '$emittedFolder'")

      val deps =
        (Json.parse(Files.readString(emittedPkg)) \ "dependencies").get
          .asInstanceOf[JsObject]
          .fields map {
          case (k, v) => (k, v.as[String])
        } toList

      if (Files.exists(dst) && !Files.isDirectory(dst))
        sys.error(
          s"Destination path already exists but is not a directory: $dst")

      val exists = Files.exists(dst)

      if (!exists)
        Files.createDirectories(dst)

      val packageJson = dst resolve "package.json"
      val indexjs = dst resolve "index.js"

      if (!Files.exists(indexjs) || Files
            .getLastModifiedTime(indexjs)
            .compareTo(Files.getLastModifiedTime(emitted)) < 0) {
        println(
          s"${if (exists) "Updating" else "Creating"} NPM package at '$dst'")

        val depsMembers = deps map { case (p, v) => s""""$p": "$v"""" } mkString ",\n    "
        val contents =
          s"""
         |{
         |  "name": "$name",
         |  "version": "$version",
         |  "description": "$description",
         |  "main": "index.js",
         |  "scripts": {
         |    "test": "echo \\"Error: no test specified\\" && exit 1"
         |  },
         |  "author": "",
         |  "license": "$licence",
         |  "dependencies": {
         |    $depsMembers
         |  },
         |  "devDependencies": {}
         |}
         |""".trim.stripMargin

        Files.writeString(packageJson, contents)
        Files.copy(emitted, indexjs, StandardCopyOption.REPLACE_EXISTING)
        Files.copy(src.toPath resolve "index.d.ts",
                   dst resolve "index.d.ts",
                   StandardCopyOption.REPLACE_EXISTING)
      } else
        println(s"NPM package at '$dst' is up-to-date")
    }
  )

}
