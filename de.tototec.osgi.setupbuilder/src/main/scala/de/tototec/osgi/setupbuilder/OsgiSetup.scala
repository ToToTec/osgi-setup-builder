package de.tototec.osgi.setupbuilder

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.jar.Attributes
import java.util.jar.JarException
import java.util.jar.JarInputStream
import java.util.jar.Manifest

/**
 * Configuration for an OSGi setup.
 *
 * @param bundles All bundles (JARs) that will be part of the target OSGi Setup.
 *   All given JARs must be valid OSGi Bundles.
 * @param frameworkBundle The symbolic name of the framework bundle. The framework bundle must be contained in the `bundles` parameter.
 * @param frameworkSettings Map of additional framework settings, that will be given to the OSGi Framework.
 * @param bundleConfigs Additional bundle specific configurations, see [[BundleConfig]].
 *   This allows to specify startup behavior and start level of a bundle.
 * @param doNotCopyBundles If `true`, do not copy the bundles into the framework specific location but leave them in the original place (as given with parameter `bundles`).
 */
case class OsgiSetup(
  val bundles: Seq[File] = Seq(),
  val frameworkBundle: String = null,
  val frameworkSettings: Map[String, String] = Map(),
  val bundleConfigs: Seq[BundleConfig] = Seq(),
  val doNotCopyBundles: Boolean = false)

/**
 * Bundle specific configuration.
 *
 * @param sysmbolicName The symbolic name of the bundle.
 * @param startLevel The optional desired start level of that bundle.
 *   An integer greater zero.
 *   If not given (`None`), the bundle will inherit the frameworks default start level (`osgi.defaultStartLevel`).
 * @param autoStart Set this to `true` if this bundle should be automatically started, once the start level is reached.
 */
case class BundleConfig(
  symbolicName: String,
  startLevel: Option[Int] = None,
  autoStart: Boolean = false)

class Bundle(val file: File) {

  val manifest: Manifest = {
    val stream = new JarInputStream(new BufferedInputStream(new FileInputStream(file)))
    try {
      stream.getManifest() match {
        case null => throw new JarException(s"""The supposed bundle file "${file}" does not contain a manifest (META-INF/MANIFEST.MF).""")
        case m => m
      }
    } finally stream.close()
  }

  implicit class RichAttributes(attributes: Attributes) {
    def getValueOrDefault(name: String, default: String): String = attributes.getValue(name) match {
      case null => default
      case value => value
    }
  }

  val symbolicName: String = {
    val value = manifest.getMainAttributes.getValue("Bundle-SymbolicName")
    if (value == null)
      throw new IllegalArgumentException(s"File ${file} is not a bundle. Bundle-SymbolicName is not defined in manifest.")
    value.split(";").head
  }

  val version: String = manifest.getMainAttributes.getValueOrDefault("Bundle-Version", "0.0.0").split(";").head

  override def toString = getClass.getSimpleName + "(" + symbolicName + "-" + version + ")"

}

