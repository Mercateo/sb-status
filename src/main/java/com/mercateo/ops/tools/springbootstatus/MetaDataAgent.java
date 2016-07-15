package com.mercateo.ops.tools.springbootstatus;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.Manifest;

public class MetaDataAgent {
	static final String MF_PREFIX = MetaDataAgent.class.getSimpleName() + ".manifest.";
	static final String ERR_PREFIX = MetaDataAgent.class.getSimpleName() + ".error.";

	public static void agentmain(String agentArgs, Instrumentation instrumentation) {
		String jarLauncher = "org.springframework.boot.loader.JarLauncher";
		try {
			copyManifestParameters(jarLauncher);
		} catch (ClassNotFoundException E) {
			System.setProperty(ERR_PREFIX + "msg",
					"Class " + jarLauncher + " not found. Perhaps the Boot application is not being run from a jar?");
		} catch (IOException e) {
			System.setProperty(ERR_PREFIX + "msg", "Cannot read pom.properties File."
					+ " Perhaps the Boot application is not being run from a jar, built with maven ?");
		}
	}

	private static void copyManifestParameters(String jarLauncher) throws ClassNotFoundException, IOException {
		Manifest manifest = Util.loadManifestFrom(jarLauncher);
		manifest.getMainAttributes().entrySet().forEach(e -> {
			System.setProperty(MF_PREFIX + e.getKey(), e.getValue().toString());
		});
	}

}
