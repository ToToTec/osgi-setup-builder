import de.tototec.sbuild._
import de.tototec.sbuild.TargetRefs._
import de.tototec.sbuild.ant._
import de.tototec.sbuild.ant.tasks._

@version("0.4.0")
@classpath("http://repo1.maven.org/maven2/org/apache/ant/ant/1.8.4/ant-1.8.4.jar")
class SBuild(implicit project: Project) {

  val namespace = "de.tototec.osgi.setupbuilder"
  val version = "0.0.2.9000"

  val scalaVersion = "2.10.2"

  val compileCp =
    s"mvn:org.scala-lang:scala-library:${scalaVersion}"

  ExportDependencies("eclipse.classpath", compileCp)

  val compilerCp =
    s"mvn:org.scala-lang:scala-library:${scalaVersion}" ~
      s"mvn:org.scala-lang:scala-compiler:${scalaVersion}" ~
      s"mvn:org.scala-lang:scala-reflect:${scalaVersion}"

  val bndCp = "mvn:biz.aQute:bndlib:1.50.0"

  val jar = s"target/${namespace}-${version}.jar"

  Target("phony:all") dependsOn "clean" ~ jar

  Target("phony:clean").evictCache exec {
    AntDelete(dir = Path("target"))
  }

  Target("phony:compile").cacheable dependsOn compileCp ~ compilerCp ~ "scan:src/main/scala" exec { ctx: TargetContext =>
    addons.scala.Scalac(
      compilerClasspath = compilerCp.files,
      classpath = compileCp.files,
      sources = "scan:src/main/scala".files,
      destDir = Path("target/classes"),
      deprecation = true,
      debugInfo = "vars"
    )
  }

  Target("scan:target/classes") dependsOn "compile"

  Target(jar) dependsOn "scan:target/classes" ~ compileCp ~ bndCp exec { ctx: TargetContext =>
    addons.bnd.BndJar(
      destFile = ctx.targetFile.get,
      bndClasspath = bndCp.files,
      classpath = compileCp.files ++ Seq(Path("target/classes")),
      props = Map(
        "Bundle-SymbolicName" -> namespace,
        "Bundle-Version" -> version,
        "Implementation-Version" -> version,
        "Export-Package" -> s"""${namespace};version="${version}"""",
        "Import-Package" -> s"""scala.*;version="[${scalaVersion},2.11),"
                                *""",
        "Include-Resource" -> "../LICENSE.txt",
        "Scala-Version" -> scalaVersion
      )
    )
  }

}
