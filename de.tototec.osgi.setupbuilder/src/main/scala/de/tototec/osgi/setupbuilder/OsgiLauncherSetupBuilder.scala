package de.tototec.osgi.osgisetup

impi

class OsgiLauncherSetupBuilder(setup: OsgiSetup) {

  val bundles = setup.bundles.map { file => new Bundle(file) }

  // FIXME: incomplete

}


