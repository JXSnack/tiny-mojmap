package me.jxsnack.tinymojmap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parchment extends ForwardingMappingVisitor {
    private static final Pattern CLASS_REF = Pattern.compile("L([^;]+);");

    private final int namedNs;
    private final Map<String, String> classNames = new HashMap<>();
    private final Map<String, Map<Integer, String>> params = new HashMap<>();

    private String namedClass;
    private String srcMethod;
    private String namedMethod;
    private String srcDesc;

    public Parchment(MappingVisitor next, MemoryMappingTree tree, JsonObject parchment) {
        super(next);
        this.namedNs = tree.getNamespaceId("named");

        for (MappingTree.ClassMapping c : tree.getClasses()) {
            String named = c.getName(namedNs);
            classNames.put(c.getSrcName(), named != null ? named : c.getSrcName());
        }

        for (JsonElement ce : parchment.getAsJsonArray("classes")) {
            JsonObject cls = ce.getAsJsonObject();
            if (!cls.has("methods")) continue;

            for (JsonElement me : cls.getAsJsonArray("methods")) {
                JsonObject m = me.getAsJsonObject();
                if (!m.has("parameters")) continue;

                Map<Integer, String> slots = new HashMap<>();
                for (JsonElement pe : m.getAsJsonArray("parameters")) {
                    JsonObject p = pe.getAsJsonObject();
                    if (p.has("name") && !p.get("name").isJsonNull()) {
                        slots.put(p.get("index").getAsInt(), p.get("name").getAsString());
                    }
                }
                params.put(cls.get("name").getAsString() + "." + m.get("name").getAsString() + m.get("descriptor").getAsString(), slots);
            }
        }
    }

    @Override
    public boolean visitClass(String srcName) throws java.io.IOException {
        namedClass = classNames.getOrDefault(srcName, srcName);
        return super.visitClass(srcName);
    }

    @Override
    public boolean visitMethod(String srcName, String srcDesc) throws java.io.IOException {
        this.srcMethod = srcName;
        this.namedMethod = srcName;
        this.srcDesc = srcDesc;
        return super.visitMethod(srcName, srcDesc);
    }

    @Override
    public void visitDstName(MappedElementKind kind, int namespace, String name) throws java.io.IOException {
        if (kind == MappedElementKind.METHOD && namespace == namedNs && name != null) namedMethod = name;
        super.visitDstName(kind, namespace, name);
    }

    @Override
    public boolean visitElementContent(MappedElementKind kind) throws java.io.IOException {
        boolean content = super.visitElementContent(kind);

        if (content && kind == MappedElementKind.METHOD && namedClass != null) {
            Map<Integer, String> slots = params.get(namedClass + "." + namedMethod + toNamedDesc(srcDesc));

            if (slots != null) {
                for (var e : slots.entrySet()) {
                    super.visitMethodArg(-1, e.getKey(), null);
                    super.visitDstName(MappedElementKind.METHOD_ARG, namedNs, e.getValue());
                    super.visitElementContent(MappedElementKind.METHOD_ARG);
                }
            }
        }

        return content;
    }

    private String toNamedDesc(String desc) {
        Matcher m = CLASS_REF.matcher(desc);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("L" + classNames.getOrDefault(m.group(1), m.group(1)) + ";"));
        }
        return m.appendTail(sb).toString();
    }
}