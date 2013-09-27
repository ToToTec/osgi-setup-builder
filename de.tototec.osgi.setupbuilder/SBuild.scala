import de.tototec.sbuild._
import de.tototec.sbuild.TargetRefs._
import de.tototec.sbuild.ant._
import de.tototec.sbuild.ant.tasks._

@version("0.4.0")
@classpath("http://repo1.maven.org/maven2/org/apache/ant/ant/1.8.4/ant-1.8.4.jar")
class SBuild(implicit project: Project) {

  val namespace = "de.tototec.osgi.setupbuilder"
  val version = "0.0.3"

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
  val sourcesJar = s"target/${namespace}-${version}-sources.jar"
  val javadocJar = s"target/${namespace}-${version}-javadoc.jar"

  Target("phony:all") dependsOn jar

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

  Target("phony:scaladoc").cacheable dependsOn compileCp ~ compilerCp ~ "scan:src/main/scala" exec { ctx: TargetContext =>
    addons.scala.Scaladoc(
      scaladocClasspath = compilerCp.files,
      classpath = compileCp.files,
      sources = "scan:src/main/scala".files,
      destDir = Path("target/scaladoc"),
      deprecation = true
    )
  }

  Target(jar) dependsOn "scan:target/classes" ~ compileCp ~ bndCp ~ "compile" exec { ctx: TargetContext =>
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
        "Include-Resource" -> "LICENSE.txt",
        "Scala-Version" -> scalaVersion
      )
    )
  }

  Target(sourcesJar) dependsOn "scan:src/main/scala" exec { ctx: TargetContext =>
    AntJar(destFile = ctx.targetFile.get, fileSet = AntFileSet(dir = Path("src/main/scala")))
  }

  Target(javadocJar) dependsOn "scaladoc" ~~ "scan:target/scaladoc" exec { ctx: TargetContext =>
    AntJar(destFile = ctx.targetFile.get, fileSet = AntFileSet(dir = Path("target/scaladoc")))
  }

  Target("phony:prepareMvnStaging") dependsOn sourcesJar ~ jar ~ javadocJar exec {
    AntMkdir(dir = Path("target/mvn"))

    val pom = s"""<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>de.tototec</groupId>
  <artifactId>de.tototec.osgi.setupbuilder</artifactId>
  <packaging>jar</packaging>
  <version>${version}</version>
  <name>OSGi Setup Builder</name>
  <description>Build OSGi Framework setups</description>
  <url>https://github.com/ToToTec/osgi-setup-builder</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/ToToTec/osgi-setup-builder.git</url>
    <connection>https://github.com/ToToTec/osgi-setup-builder.git</connection>
  </scm>
  <developers>
    <developer>
      <id>TobiasRoeser</id>
      <name>Tobias Roeser</name>
      <email>tobias.roeser@tototec.de</email>
    </developer>
  </developers>
</project>"""

    AntEcho(message = pom, file = Path("target/mvn/pom.xml"))

    val script = s"""#!/bin/sh

echo "Please edit settings.xml with propper connection details."
read

echo "Uploading jar"
mvn -s ./settings.xml gpg:sign-and-deploy-file -DpomFile=pom.xml -Dfile=${jar.files.head} -Durl=http://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging

echo "Uploading sources"
mvn -s ./settings.xml gpg:sign-and-deploy-file -DpomFile=pom.xml -Dfile=${sourcesJar.files.head} -Dclassifier=sources -Durl=http://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging

echo "Uploading javadoc"
mvn -s ./settings.xml gpg:sign-and-deploy-file -DpomFile=pom.xml -Dfile=${javadocJar.files.head} -Dclassifier=javadoc -Durl=http://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging

"""
    AntEcho(message = script, file = Path("target/mvn/script.sh"))

    val settings = """<settings>
  <servers>
    <server>
      <id>sonatype-nexus-staging</id>
      <username>your-username</username>
      <password>your-password</password>
    </server>
  </servers>
</settings>
"""

    AntEcho(message = settings, file = Path("target/mvn/settings.xml"))

  }

}
