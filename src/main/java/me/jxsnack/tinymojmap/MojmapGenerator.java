package me.jxsnack.tinymojmap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipFile;

public class MojmapGenerator {
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String INTERMEDIARY = "https://maven.fabricmc.net/net/fabricmc/intermediary/%1$s/intermediary-%1$s-v2.jar";
    private static final String PARCHMENT = "https://maven.parchmentmc.org/org/parchmentmc/data/parchment-%1$s/%2$s/parchment-%1$s-%2$s.zip";

    private final Path cache;
    private final Path out;

    public MojmapGenerator(Path cache, Path out) {
        this.cache = cache;
        this.out = out;
    }

    public void generate(String version, String parchmentVersion) throws Exception {
        Files.createDirectories(cache);
        Files.createDirectories(out);

        // find mappings
        JsonObject manifest = JsonParser.parseString(Files.readString(download(MANIFEST, "manifest.json"))).getAsJsonObject();
        String versionUrl = null;
        for (var v : manifest.getAsJsonArray("versions")) {
            if (v.getAsJsonObject().get("id").getAsString().equals(version)) {
                versionUrl = v.getAsJsonObject().get("url").getAsString();
            }
        }
        if (versionUrl == null) throw new IllegalArgumentException("Unknown version: " + version);

        JsonObject downloads = JsonParser.parseString(Files.readString(download(versionUrl, version + ".json")))
                .getAsJsonObject()
                .getAsJsonObject("downloads");

        Path client = download(
                downloads.getAsJsonObject("client_mappings")
                        .get("url").getAsString(),
                "client.txt"
        );
        Path server = download(
                downloads.getAsJsonObject("server_mappings")
                        .get("url").getAsString(),
                "server.txt"
        );
        Path jar = download(
                INTERMEDIARY.formatted(version),
                "intermediary-" + version + ".jar"
        );

        // official -> intermediary
        MemoryMappingTree tree = new MemoryMappingTree();
        try (
                ZipFile zip = new ZipFile(jar.toFile());
                Reader r = new java.io.InputStreamReader(zip.getInputStream(zip.getEntry("mappings/mappings.tiny")))
        ) {
            MappingReader.read(r, MappingFormat.TINY_2_FILE, tree);
        }

        // add mojang names
        for (Path p : new Path[]{client, server}) {
            try (Reader r = Files.newBufferedReader(p)) {
                ProGuardFileReader.read(r, "named", "official", new MappingSourceNsSwitch(tree, "official"));
            }
        }

        // parchment parameter names
        JsonObject parchment = null;
        if (parchmentVersion != null) {
            Path zipPath = download(PARCHMENT.formatted(version, parchmentVersion), "parchment-" + version + "-" + parchmentVersion + ".zip");
            try (
                    ZipFile zip = new ZipFile(zipPath.toFile());
                    Reader r = new java.io.InputStreamReader(zip.getInputStream(zip.getEntry("parchment.json")))
            ) {
                parchment = JsonParser.parseReader(r).getAsJsonObject();
            }
        }

        // write and fall back to intermediary
        try (
                Writer w = Files.newBufferedWriter(out.resolve("mappings.tiny"));
                Tiny2FileWriter writer = new Tiny2FileWriter(w, false)
        ) {
            MappingVisitor sink = new MappingNsCompleter(writer, Map.of("named", "intermediary"));
            tree.accept(parchment != null ? new Parchment(sink, tree, parchment) : sink);
        }
    }

    private Path download(String url, String name) throws Exception {
        Path file = cache.resolve(name);
        if (!Files.exists(file)) {
            try (var in = URI.create(url).toURL().openStream()) {
                Files.copy(in, file);
            }
        }
        return file;
    }
}