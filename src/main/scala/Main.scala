package xyz.hyperreal.npm_plugin

import java.nio.file.{Files, Paths, StandardCopyOption}

object Main extends App {

  def makeNpmPackage(dstpath: String,
                     name: String,
                     version: String,
                     description: String,
                     licence: String,
                     deps: List[(String, String)],
                     devDeps: List[(String, String)],
                     jspath: String) = {
    val dst = Paths.get(dstpath)

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

    Files.write(packageJson, contents.getBytes(io.Codec.UTF8.charSet))
    Files.copy(Paths.get(jspath),
               dst resolve "index.js",
               StandardCopyOption.REPLACE_EXISTING)
  }

  makeNpmPackage("./target/npm",
                 "test",
                 "1.2.3",
                 "test package",
                 "ISC",
                 List("shortid" -> "2.2.15"),
                 List("@types/shortid" -> "0.0.29"),
                 "index.js")

}
