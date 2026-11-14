package me.jxsnack.tinymojmap;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class TinyJar {
    public static void pack(Path tiny, Path jar) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (
                OutputStream out = Files.newOutputStream(jar);
                JarOutputStream jos = new JarOutputStream(out, manifest)
        ) {
            jos.putNextEntry(new JarEntry("mappings/mappings.tiny"));
            Files.copy(tiny, jos);
            jos.closeEntry();
        }
    }
}