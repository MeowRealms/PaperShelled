package cn.apisium.papershelled;

import cn.apisium.papershelled.plugin.PaperShelledDescription;
import cn.apisium.papershelled.plugin.PaperShelledPlugin;
import cn.apisium.papershelled.util.MixinJarUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixins;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public final class PaperShelled {

    private static JsonObject versionJson;
    private static final Map<String, PaperShelledPlugin> pluginsMap = new HashMap<>();

    @SuppressWarnings("unused")
    @Nullable
    public static JsonObject getVersionJson() {
        return versionJson;
    }

    @NotNull
    public static Map<String,PaperShelledPlugin> getPluginsMap() {
        return pluginsMap;
    }

    @SuppressWarnings("ProtectedMemberInFinalClass")
    protected static void init() throws Throwable {
        try (InputStream is = PaperShelledAgent.getResourceAsStream("version.json")) {
            if (is != null) {
                versionJson = new JsonParser().parse(new InputStreamReader(is)).getAsJsonObject();
            }
        }
        Path patch = Paths.get("plugins");
        Files.createDirectories(patch);
        loadMixins(patch);
    }

    @SuppressWarnings("resource")
    private static void loadMixins(Path path) throws Throwable {
        for (Iterator<Path> iter = Files.list(path).filter(it -> it.toString().endsWith(".jar") && Files.isRegularFile(it)).iterator(); iter.hasNext(); ) {
            File file = iter.next().toFile();
            PaperShelledDescription description;
            try {
                description = getPaperShelledDescription(file);
            } catch (InvalidDescriptionException ex) {
                ex.printStackTrace();
                PaperShelledAgent.LOGGER.log(Level.SEVERE, "Could not load '" + file.getPath() + "'", ex);
                continue;
            }

            if (description.getMixins().isEmpty()) continue;

            try {
                loadMixin(file, description);
            } catch (InvalidPluginException ex) {
                ex.printStackTrace();
                PaperShelledAgent.LOGGER.log(Level.SEVERE, "Could not load '" + file.getPath() + "'", ex);
            }
        }
    }

    private static void loadMixin(@NotNull final File file, @NotNull final PaperShelledDescription description) throws InvalidPluginException {
        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(file + " does not exist"));
        }
        try {
            for (String mixin : description.getMixins()) {
                File mixinJar = MixinJarUtil.createMixinJarFile(file, mixin, com.google.common.io.Files.getNameWithoutExtension(file.getName()) + "-", description.getAdditions());
                PaperShelledPlugin plugin = new PaperShelledPlugin(mixinJar.getName(), new JarFile(mixinJar));
                pluginsMap.put(mixinJar.getName(), plugin);
                PaperShelledAgent.getInstrumentation().appendToSystemClassLoaderSearch(plugin.getJar());
                Mixins.addConfiguration(mixinJar.getName() + "|" + mixin);
                PaperShelledAgent.LOGGER.log(Level.INFO, "Loading " + file.getName() + "/" + mixin);
            }
        } catch (IOException ex) {
            throw new InvalidPluginException(ex);
        }
    }

    @NotNull
    public static PaperShelledDescription getPaperShelledDescription(@NotNull File file) throws InvalidDescriptionException {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("papershelled.json");
            if (entry == null) {
                return new PaperShelledDescription();
            }
            try (InputStream is = jar.getInputStream(entry)) {
                return new Gson().fromJson(new InputStreamReader(is), PaperShelledDescription.class);
            } catch (IOException | YAMLException ex) {
                throw new InvalidDescriptionException(ex);
            }
        } catch (Throwable e) {
            throw new InvalidDescriptionException(e);
        }
    }
}
