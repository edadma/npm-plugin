package xyz.hyperreal.npm_plugin

import java.nio.file.{Files, Paths, StandardCopyOption}

import collection.JavaConverters._

object Main extends App {

  def makeNpmPackage(name: String,
                     version: String,
                     description: String,
                     licence: String,
                     deps: List[(String, String)],
                     devDeps: List[(String, String)],
                     jspath: String) = {
    val target = Paths.get("target")
    val src =
      Files.walk(target).iterator.asScala find { p =>
        p.toString endsWith "-opt.js"
      } match {
        case None =>
          sys.error("couldn't find a compiler output file ending in '-opt.js'")
        case Some(js) => js
      }

    val dst = target resolve "npm"

    if (Files.exists(dst) && !Files.isDirectory(dst))
      sys.error(s"destination path already exists but is not a directory: $dst")

    if (!Files.exists(dst))
      Files.createDirectories(dst)

    val packageJson = dst resolve "package.json"

    def depsmap(d: List[(String, String)]) =
      d map { case (p, v) => s""""$p": "^$v"""" } mkString ",\n"

    val depsMembers = depsmap(deps)
    val devDepsMembers = depsmap(devDeps)
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
         |  "devDependencies": {
         |    $devDepsMembers
         |  }
         |}
         |""".trim.stripMargin

    Files.writeString(packageJson, contents)
    Files.copy(src, dst resolve "index.js", StandardCopyOption.REPLACE_EXISTING)
  }

  makeNpmPackage("test",
                 "1.2.3",
                 "test package",
                 "ISC",
                 List("shortid" -> "2.2.15"),
                 List("@types/shortid" -> "0.0.29"),
                 "index.js")

}
