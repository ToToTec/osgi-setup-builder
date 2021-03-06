package de.tototec.osgi.setupbuilder

import java.io.File
import java.util.jar.Manifest
import java.util.jar.JarFile
import java.util.jar.Attributes
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.Properties

/**
 * Create a typical Eclipse Equinox setup.
 * Based on a target directory, all bundles will be copied into a sub directory "plugins" with the equinox specific name pattern ($symbolicName_$version.jar).
 * A "config.ini" file will be created in the "configuration" sub-directory.
 *
 */
class EquinoxSetupBuilder(
    /** The OSGiSetup. */
    setup: OsgiSetup,
    /** The target directory, where the framework setup will be created. Existing files will be overridden without notice. */
    targetDir: File) {

  protected lazy val log = new {
    def debug(msg: => String, throwable: Throwable = null) { println(msg) }
  }

  /** Actually create the framework setup into the target directory. */
  def build {
    targetDir.mkdirs

    val pluginDir = new File(targetDir, "plugins")
    pluginDir.mkdirs

    val configDir = new File(targetDir, "configuration")
    configDir.mkdirs

    var equinoxConfig: Map[String, String] = Map()

    setup.bundles.map { file =>
      // the Bundle 
      val bundle = new Bundle(file)
      val isFrameworkBundle = setup.frameworkBundle.exists(fb => fb == bundle.symbolicName)
      val bundleString = if (isFrameworkBundle) "framework bundle" else "bundle"
      val copyBundle = !setup.doNotCopyBundles

      // the target location of the bundle
      val bundleFile = if (!copyBundle) {
        log.debug(s"Using ${bundleString} ${bundle.symbolicName} from source location.")
        file

      } else {
        val bundleFile = new File(pluginDir, s"${bundle.symbolicName}_${bundle.version}.jar")

        // copy bundle to target location
        log.debug(s"Copying ${bundleString} ${bundle.symbolicName} to plugins directory.")
        val out = new FileOutputStream(bundleFile)
        val in = new FileInputStream(file)
        try {
          out.getChannel.transferFrom(in.getChannel, 0, Long.MaxValue)
        } finally {
          in.close
          out.close
        }

        bundleFile
      }

      // Add the bundle to config, either as framework bundle or as plugin with optional start level and start configuration
      if (isFrameworkBundle) {
        equinoxConfig += ("osgi.framework" -> {
          if (copyBundle) s"file:plugins/${bundleFile.getName}"
          else s"file:${bundleFile.getAbsolutePath}"
        }
        )
      } else {
        // the suffix indicates the start level and the start state
        // <URL | simple bundle location>[@ [<start-level>] [":start"]]

        val suffix = setup.bundleConfigs.find(config => config.symbolicName == bundle.symbolicName) match {
          case Some(BundleConfig(_, startLevel, autoStart)) =>
            "@" +
              startLevel.map(level => level.toString).getOrElse("") +
              (if (autoStart) ":start" else "")
          case None => ""
        }

        var bundles = equinoxConfig.get("osgi.bundles").toSeq
        bundles ++= Seq(
          if (copyBundle) s"reference:file:${bundleFile.getName}${suffix}"
          else s"reference:file:${bundleFile.getAbsolutePath}${suffix}"
        )

        equinoxConfig += ("osgi.bundles" -> bundles.mkString(","))
      }

    }

    // copy frameworkSettings info equinox config
    equinoxConfig ++= setup.frameworkSettings

    // write equinox config
    val props = new Properties()
    equinoxConfig.foreach {
      case (key, value) => props.setProperty(key, value)
    }
    val out = new FileOutputStream(new File(configDir, "config.ini"))
    try {
      props.store(out, "Equinox OSGi configuration generated by " + getClass.getName)
    } finally {
      out.close
    }
  }

}
