OSGi Setup Builder
==================

Build OSGi Framework setups. 

Currently supported OSGi Launcher:
* Eclipse Equinox Launcher

Planned supported OSGi Launchers:
* ToToTec OSGi Launcher

The OSGi Setup Builder API is designed to be easily integrated into build tools like [SBuild](http://sbuild.tototec.de).

License: [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

Creating a Eclipse Equinox Setup
--------------------------------

```scala
import de.tototec.osgi.setupbuilder._
import java.io.File

val bundles = Seq(
  "org.eclipse.osgi_3.8.1.v20120830-144521.jar",
  "org.eclipse.equinox.common_3.6.100.v20120522-1841.jar",
  "org.eclipse.equinox.console_1.0.0.v20120522-1841.jar"
  "org.apache.felix.gogo.command-0.12.0.jar",
  "org.apache.felix.gogo.runtime-0.10.0.jar",
  "org.apache.felix.gogo.shell-0.10.0.jar",
  "org.apache.felix.configadmin-1.6.0.jar"
).map(f => new File("jars/" + f))

val setup = OsgiSetup(
  bundles = bundles, 
  frameworkBundle = "org.eclipse.core",
  frameworkSettings = Map(
    "osgi.clean" -> "true",
    "osgi.console" -> "",
    "eclipse.application.launchDefault" -> "false",
    "eclipse.consoleLog" -> "true",
    "osgi.bundles.defaultStartLevel" -> "4",
    "osgi.startLevel" -> "6"
  ),
  bundleConfigs = Seq(
    BundleConfig(symbolicName = "org.apache.felix.configadmin", autoStart = true)
  )
)

val builder = new EquinoxSetupBuilder(setup = setup, targetDir = new File("target/equinox"))
builder.build
```

Download from Maven Central
---------------------------

You can download from http://central.maven.org/maven2/de/tototec/de.tototec.osgi.setupbuilder/[Maven Central]:

Maven coordinates:
```xml
<dependency>
  <groupId>de.tototec</groupId>
  <artifactId>de.tototec.osgi.setupbuilder</artifactId>
  <version>0.0.3</version>
</dependency>
```

SBuild: <code>mvn:de.tototec:de.tototec.osgi.setupbuilder:0.0.3</code>
