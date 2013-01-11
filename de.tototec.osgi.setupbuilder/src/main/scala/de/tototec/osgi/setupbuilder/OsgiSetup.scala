package de.tototec.osgi.setupbuilder

import java.io.File
import java.util.jar.Manifest
import java.util.jar.JarFile
import java.util.jar.Attributes

case class OsgiSetup(
  /**
   * All bundles that will be part of the target OSGi Setup.
   * All given JARs must be valid OSGi Bundles.
   */
  val bundles: Seq[File] = Seq(),
  /** The symbolic name of the framework bundle. */
  val frameworkBundle: String = null,
  /** Additional settings, that will be given to the OSGi Framework. */
  val frameworkSettings: Map[String, String] = Map(),
  /**
 * Special settings per bundle, if required.
 * This allows to specify startup behavior and start level of a bundle.
 */
  val bundleConfigs: Seq[BundleConfig] = Seq())

case class BundleConfig(
  /** The Bundle-SymbolicName to configure. */
  symbolicName: String,
  /**
 * Optionally specify the start level of this Bundle. An integer greater zero.
 * If not given (None), the bundle will inherit the frameworks default start level (osgi.defaultStartLevel).
 */
  startLevel: Option[Int] = None,
  /** Set this to <code>true</code> if this bundle should be automatically started, once the start level is reached. */
  autoStart: Boolean = false)

class Bundle(val file: File) {

  val manifest: Manifest = new JarFile(file).getManifest

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

