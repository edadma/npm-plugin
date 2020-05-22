package xyz.hyperreal.npm_plugin

import java.nio.file.{Files, Path, StandardCopyOption}

import sys.process._
import collection.JavaConverters._
import play.api.libs.json._
import sbt._

object NpmPlugin extends AutoPlugin {

  object autoImport {
    val npmPublish = taskKey[Unit]("Publish an NPM package")
    val npmPack = taskKey[Unit]("Pack an NPM package")
    val npmBuild = taskKey[Unit]("Build an NPM package")
  }

  import autoImport._

  override def trigger = allRequirements

  def search(dir: Path, filename: String) =
    Files.walk(dir).iterator.asScala find { p =>
      p.getFileName.toString == filename
    }

  def replace(s: String) = s.replace('@', '-').replace('/', '-')

  override lazy val projectSettings = Seq(
    npmPublish := {
      npmBuild.value

      val name = replace(Keys.name.value)
      val dst = Keys.target.value.toPath resolve "npm"
      val tarballs =
        Files.walk(dst).iterator.asScala filter { p =>
          val filename = p.getFileName.toString

          filename.startsWith(name) && filename.endsWith(".tgz")
        }

      for (t <- tarballs) {
        println(s"removing package tarball '$t'")
        Files.delete(t)
      }

      Process("npm publish", dst.toFile) !
    },
    npmPack := {
      npmBuild.value

      val dst = Keys.target.value.toPath resolve "npm"

      Process("npm pack", dst.toFile) !
    },
    npmBuild := {
      val name = replace(Keys.name.value)
      val src = Keys.sourceDirectory.value.toPath
      val build = src.getParent resolve "build.sbt"
      val version = Keys.version.value
      val description = Keys.description.value
      val licence = Keys.licenses.value.head._1
      val target = Keys.target.value.toPath
      val fastOpt = search(target, s"$name-fastopt.js")
      val fullOpt = search(target, s"$name-opt.js")

      if (fastOpt.isEmpty && fullOpt.isEmpty)
        sys.error(s"Couldn't find a compiler output file")

      val emitted =
        if (fastOpt.nonEmpty && fullOpt.nonEmpty) {
          if (Files
                .getLastModifiedTime(fastOpt.get)
                .compareTo(Files.getLastModifiedTime(fullOpt.get)) > 0)
            fastOpt.get
          else
            fullOpt.get
        } else {
          if (fastOpt.nonEmpty)
            fastOpt.get
          else
            fullOpt.get
        }

      println(s"""Found a compiler output file '$emitted'""")

      val declarationFile = src resolve "index.d.ts"

      if (!Files.exists(declarationFile))
        sys.error(
          s"Couldn't find TypeScript type declaration file 'index.d.ts' in directory '$declarationFile'")

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
            .compareTo(Files.getLastModifiedTime(emitted)) < 0 || Files
            .getLastModifiedTime(packageJson)
            .compareTo(Files.getLastModifiedTime(build)) < 0) {
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
        Files.copy(declarationFile,
                   dst resolve "index.d.ts",
                   StandardCopyOption.REPLACE_EXISTING)
      } else
        println(s"NPM package at '$dst' is up-to-date")
    }
  )

}
