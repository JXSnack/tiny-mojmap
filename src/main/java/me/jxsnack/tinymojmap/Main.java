package me.jxsnack.tinymojmap;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        new MojmapGenerator(Path.of("build", "mapping-cache"), Path.of("build", "mappings"))
                .generate(args.length > 0 ? args[0] : "1.21.1");
    }
}