package com.mercateo.ops.tools.springbootstatus;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

class Util {

    static Manifest loadManifestFrom(Class<?> c) throws ClassNotFoundException,
            IOException {
        URLClassLoader cl = (URLClassLoader) c.getClassLoader();
        URL url = cl.findResource("META-INF/MANIFEST.MF");
        Manifest manifest = new Manifest(url.openStream());
        return manifest;
    }

    static Manifest loadManifestFrom(String cn) throws ClassNotFoundException,
            IOException {
        return loadManifestFrom(Class.forName(cn));
    }

    static JarFile findJarContaining(Class<?> c) throws ClassNotFoundException,
            IOException {
        URLClassLoader cl = (URLClassLoader) c.getClassLoader();
        String cn = c.getCanonicalName();
        URL url = cl.findResource(cn.replace(".", "/") + ".class");
        String s = url.getFile().substring(url.getProtocol().length() + 2);
        String jar = s.substring(0, s.lastIndexOf("!"));
        return new JarFile(jar);
    }

    public static JarFile findJarContaining(String cn)
            throws ClassNotFoundException, IOException {
        return findJarContaining(Class.forName(cn));
    }

    public static Properties findPomProperties(Class<?> c)
            throws ClassNotFoundException, IOException {
        return getPomProperties(findJarContaining(c));
    }

    public static Properties findPomProperties(String c)
            throws ClassNotFoundException, IOException {
        return getPomProperties(findJarContaining(c));
    }

    private static Properties getPomProperties(JarFile findJarContaining)
            throws IOException {
        Properties p = new Properties();
        Optional<JarEntry> e = findJarContaining
                .stream()
                .filter(n -> n.getName().startsWith("META-INF/maven/")
                        && n.getName().endsWith("/pom.properties"))

                .findFirst();
        if (e.isPresent()) {

            p.load(findJarContaining.getInputStream(e.get()));

        }
        return p;
    }
}
