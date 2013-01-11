package de.tototec.osgi.setupbuilder

class OsgiLauncherSetupBuilder(setup: OsgiSetup) {

  val bundles = setup.bundles.map { file => new Bundle(file) }

  // FIXME: incomplete

}


