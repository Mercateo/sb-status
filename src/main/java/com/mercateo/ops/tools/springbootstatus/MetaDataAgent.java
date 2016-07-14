package com.mercateo.ops.tools.springbootstatus;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Properties;
import java.util.jar.Manifest;

public class MetaDataAgent {
	static final String POM_PREFIX = MetaDataAgent.class.getSimpleName() + ".pom.";
	static final String MF_PREFIX = MetaDataAgent.class.getSimpleName() + ".manifest.";
	static final String PREFIX = MetaDataAgent.class.getSimpleName() + ".error.";

	public static void agentmain(String agentArgs, Instrumentation instrumentation) {
		String jarLauncher = "org.springframework.boot.loader.JarLauncher";
		try {
			copyPomProperties(jarLauncher);
			copyManifestParameters(jarLauncher);
		} catch (ClassNotFoundException E) {
			System.setProperty(POM_PREFIX + "error",
					"Class " + jarLauncher + " not found. Perhaps the Boot application is not being run from a jar?");
		} catch (IOException e) {
			System.setProperty(PREFIX + "msg", "Cannot read pom.properties File."
					+ " Perhaps the Boot application is not being run from a jar, built with maven ?");
		}
	}

	private static void copyManifestParameters(String jarLauncher) throws ClassNotFoundException, IOException {
		Manifest manifest = Util.loadManifestFrom(jarLauncher);
		manifest.getMainAttributes().entrySet().forEach(e -> {
			System.setProperty(POM_PREFIX + e.getKey(), e.getValue().toString());
		});
	}

	private static void copyPomProperties(String jarLauncher) throws ClassNotFoundException, IOException {
		Properties props = Util.findPomProperties(jarLauncher);
		props.forEach((k, v) -> {
			System.setProperty(POM_PREFIX + k, v.toString());
		});
	}

}
