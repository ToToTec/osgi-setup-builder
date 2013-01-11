package de.tototec.osgi.setupbuilder

import java.io.File
import java.util.jar.Manifest
import java.util.jar.JarFile
import java.util.jar.Attributes

case class BundleConfig(symbolicName: String, startLevel: Option[Int] = None, autoStart: Boolean = false)

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

case class OsgiSetup(
  val bundles: Seq[File] = Seq(),
  val frameworkBundle: String = null,
  val frameworkSettings: Map[String, String] = Map(),
  val bundleConfigs: Seq[BundleConfig] = Seq())
