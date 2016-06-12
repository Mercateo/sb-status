package com.mercateo.ops.tools.springbootstatus;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Properties;

public class MavenVersionAgent {
    static final String PREFIX = MavenVersionAgent.class.getSimpleName() + ".";

    public static void agentmain(String agentArgs,
            Instrumentation instrumentation) {
        String jarLauncher = "org.springframework.boot.loader.JarLauncher";
        try {
            Properties props = Util.findPomProperties(jarLauncher);
            props.forEach((k, v) -> {
                System.setProperty(PREFIX + k, v.toString());
            });
        }
        catch (ClassNotFoundException E) {
            System.setProperty(
                    PREFIX + "error",
                    "Class "
                            + jarLauncher
                            + " not found. Perhaps the Boot application is not being run from a jar?");
        }
        catch (IOException e) {
            System.setProperty(
                    PREFIX + "error",
                    "Cannot read pom.properties File."
                            + " Perhaps the Boot application is not being run from a jar, built with maven ?");
        }
    }

}
