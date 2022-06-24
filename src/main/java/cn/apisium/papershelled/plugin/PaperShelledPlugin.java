package cn.apisium.papershelled.plugin;

import java.util.jar.JarFile;

/**
 * PaperShelled
 * cn.apisium.papershelled.plugin.PaperShelledPlugin
 *
 * @author 坏黑
 * @since 2022/6/24 18:46
 */
public class PaperShelledPlugin {

    public final String name;
    public final JarFile jar;

    public PaperShelledPlugin(String name, JarFile jar) {
        this.name = name;
        this.jar = jar;
    }

    public String getName() {
        return name;
    }

    public JarFile getJar() {
        return jar;
    }
}
