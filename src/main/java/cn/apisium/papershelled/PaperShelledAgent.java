package cn.apisium.papershelled;

import cn.apisium.papershelled.services.MixinService;
import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.tools.agent.MixinAgent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.logging.Logger;

public final class PaperShelledAgent {

    public final static Logger LOGGER = PaperShelledLogger.getLogger(null);

    private static boolean initialized;
    private static Instrumentation instrumentation;
    private static Path serverJar;

    @NotNull
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    @SuppressWarnings("unused")
    @Nullable
    public static Path getServerJar() {
        return serverJar;
    }

    @Nullable
    public static InputStream getResourceAsStream(String name) {
        return ClassLoader.getSystemResourceAsStream(name);
    }

    @Nullable
    public static InputStream getClassAsStream(String name) {
        return getResourceAsStream(name.replace('.', '/') + ".class");
    }

    public static byte[] getClassAsByteArray(String name) throws IOException {
        try (InputStream is = getClassAsStream(name)) {
            if (is == null) {
                throw new FileNotFoundException("Class not found: " + name);
            }
            return ByteStreams.toByteArray(is);
        }
    }

    private final static class Transformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] data) {
            if ("cn/dreeam/leaper/QuantumLeaper".equals(className)) {
                LOGGER.info("Injecting cn/dreeam/leaper/QuantumLeaper");
                ClassReader cr = new ClassReader(data);
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        MethodVisitor v = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return name.equals("main") ? new MethodVisitor(Opcodes.ASM9) {

                            /**
                             * What I want is as follows:
                             * {@code
                             *
                             * public static void main(String[] args){
                             *     cn.apisium.papershelled.launcher.Launcher.launch(args);
                             * }
                             * }
                             * And this is the ASM way to make it.
                             */
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                Label l0 = new Label();
                                v.visitLabel(l0);
                                v.visitLineNumber(0, l0);//Trick JVM
                                v.visitVarInsn(Opcodes.ALOAD, 0);
                                v.visitMethodInsn(Opcodes.INVOKESTATIC, "cn/apisium/papershelled/launcher/Launcher", "launch", "([Ljava/lang/String;)V", false);
                                Label l1 = new Label();
                                v.visitLabel(l1);
                                v.visitLineNumber(0, l1);
                                v.visitInsn(Opcodes.RETURN);
                                Label l2 = new Label();
                                v.visitLabel(l2);
                                v.visitLocalVariable("args", "[Ljava/lang/String;", null, l0, l2, 0);
                                v.visitMaxs(1, 1);
                                v.visitEnd();
                            }
                        } : v;
                    }
                }, ClassReader.EXPAND_FRAMES);
                return cw.toByteArray();
            } else if ("org/bukkit/craftbukkit/Main".equals(className)) {
                LOGGER.info("Injecting org/bukkit/craftbukkit/Main");
                data = inject(data, "main", "cn/apisium/papershelled/PaperShelledAgent", "init");
                URL url = Objects.requireNonNull(loader.getResource("org/bukkit/craftbukkit/Main.class"));
                try {
                    serverJar = Paths.get(new URI(url.getFile().split("!", 2)[0]));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            if (!initialized) {
                return data;
            }
            IMixinTransformer transformer = MixinService.getTransformer();
            return transformer == null ? null : transformer.transformClass(MixinEnvironment.getDefaultEnvironment(), className.replace('/', '.'), data);
        }
    }

    private static void initPaperShelled(Instrumentation instrumentation) {
//        Package pkg = PaperShelledAgent.class.getPackage();
//        LOGGER.info(pkg.getImplementationTitle() + " (" + pkg.getImplementationVendor() + ")");
//        LOGGER.info("You can get the latest updates from: https://github.com/Apisium/PaperShelled");
        System.setProperty("mixin.env.remapRefMap", "true");
        PaperShelledAgent.instrumentation = instrumentation;
        instrumentation.addTransformer(new Transformer(), true);
        MixinBootstrap.init();
        MixinBootstrap.getPlatform().inject();
        System.setProperty("papershelled.enable", "true");
    }

    public static void premain(String arg, Instrumentation instrumentation) {
        System.setProperty("mixin.hotSwap", "true");
        initPaperShelled(instrumentation);
        MixinAgent.premain(arg, instrumentation);
    }

    public static void agentmain(String arg, Instrumentation instrumentation) {
        initPaperShelled(instrumentation);
        MixinAgent.agentmain(arg, instrumentation);
    }

    @SuppressWarnings("unused")
    public static void init() throws Throwable {
        LOGGER.info("Initializing");
        initialized = true;
        PaperShelled.init();
        PaperShelledLogger.restore();
    }

    private static byte[] inject(byte[] arr, String method0, String clazz, String method1) {
        ClassReader cr = new ClassReader(arr);
        ClassWriter cw = new ClassWriter(0);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return method0.equals(name) ? new MethodVisitor(api, mv) {

                    @Override
                    public void visitCode() {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, clazz, method1, "()V", false);
                        super.visitCode();
                    }
                } : mv;
            }
        }, ClassReader.SKIP_DEBUG);
        return cw.toByteArray();
    }
}
