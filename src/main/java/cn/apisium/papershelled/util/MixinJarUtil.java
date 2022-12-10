package cn.apisium.papershelled.util;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class MixinJarUtil {

    @NotNull
    public static File createMixinJarFile(@NotNull File file, @NotNull String mixinConfig, @NotNull String prefix) throws IOException {
        Map<String, byte[]> mixinBytes = getMixinBytes(file, mixinConfig);
        if (mixinBytes.isEmpty()) throw new FileNotFoundException("Class or config not found");
//        mixinBytes.keySet().forEach(PaperShelledAgent.LOGGER::severe);
        return createJarFile(prefix, mixinBytes);
    }

    @NotNull
    private static Map<String, byte[]> getMixinBytes(@NotNull File file, @NotNull String mixinConfig) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file + " does not exist");
        }
        if (Strings.isNullOrEmpty(mixinConfig)) {
            throw new IllegalArgumentException("The mixinConfig cannot be null or empty");
        }

        Map<String, byte[]> mixinBytes = new HashMap<>();

        try (JarFile jar = new JarFile(file)) {
            JarEntry configEntry = jar.getJarEntry(mixinConfig);

            if (configEntry != null) {
                List<String> mixinClasses = new ArrayList<>();
                byte[] configBytes;
                String refMapperConfig = null;

                try (InputStream is = jar.getInputStream(configEntry)) {
                    JsonObject config = new Gson().fromJson(new InputStreamReader(is), JsonObject.class);
                    configBytes = config.toString().getBytes();

                    if (config.has("package")) {
                        String mixinPackage = config.get("package").getAsString() + ".";
                        if (config.has("mixins"))
                            config.getAsJsonArray("mixins").forEach(element -> mixinClasses.add(mixinPackage + element.getAsString()));
                        if (config.has("client"))
                            config.getAsJsonArray("client").forEach(element -> mixinClasses.add(mixinPackage + element.getAsString()));
                        if (config.has("server"))
                            config.getAsJsonArray("server").forEach(element -> mixinClasses.add(mixinPackage + element.getAsString()));
                        if (config.has("refmap"))
                            refMapperConfig = config.get("refmap").getAsString();
                    }
                }

                for (String mixinClass : mixinClasses) {
                    String mixinClassPath = mixinClass.replace('.', '/') + ".class";
                    JarEntry classEntry = jar.getJarEntry(mixinClassPath);

                    if (classEntry != null) {
                        try (InputStream is = jar.getInputStream(classEntry)) {
                            byte[] classBytes = ByteStreams.toByteArray(is);
                            mixinBytes.put(mixinClassPath, classBytes);
                        }
                    }
                }

                if (!mixinBytes.isEmpty()) {
                    mixinBytes.put(mixinConfig, configBytes);

                    if (refMapperConfig != null) {
                        JarEntry refmapEntry = jar.getJarEntry(refMapperConfig);

                        if (refmapEntry != null) {
                            try (InputStream is = jar.getInputStream(refmapEntry)) {
                                byte[] refmapBytes = ByteStreams.toByteArray(is);
                                mixinBytes.put(refMapperConfig, refmapBytes);
                            }
                        }
                    }
                }
            }
        }

        return mixinBytes;
    }

    private static File createJarFile(String prefix, Map<String, byte[]> files) throws IOException {
        File file = File.createTempFile(prefix, ".jar", getPaperShelledDir());
        file.deleteOnExit();

        try (JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(file), new Manifest())) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                JarEntry jarEntry = new JarEntry(entry.getKey());
                jarStream.putNextEntry(jarEntry);
                jarStream.write(entry.getValue());
                jarStream.closeEntry();
            }
        }

        return file;
    }

    private static File getPaperShelledDir() throws IOException {
        Path temp = Paths.get("papershelled");
        Files.createDirectories(temp);
        return temp.toFile();
    }
}
