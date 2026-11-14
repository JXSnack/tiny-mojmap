package me.jxsnack.tinymojmap;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        String version = args.length > 0 ? args[0] : "1.21.1";
        Path out = Path.of("build", "mappings");

        new MojmapGenerator(Path.of("build", "mapping-cache"), out).generate(version, "2024.11.17");
        TinyJar.pack(out.resolve("mappings.tiny"), out.resolve("mojmap-" + version + ".jar"));
    }
}