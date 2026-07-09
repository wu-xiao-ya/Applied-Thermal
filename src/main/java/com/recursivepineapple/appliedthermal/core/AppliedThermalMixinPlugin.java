package com.recursivepineapple.appliedthermal.core;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class AppliedThermalMixinPlugin implements IMixinConfigPlugin {

    private static final String MOUSE_TWEAKS_COMPAT_MIXIN =
        "com.recursivepineapple.appliedthermal.mixin.MixinMouseTweaksGuiContainer2";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (MOUSE_TWEAKS_COMPAT_MIXIN.equals(mixinClassName)) {
            return "yalter.mousetweaks.api.IMTModGuiContainer2".equals(targetClassName)
                && !targetClassHasMethod(targetClassName, "MT_isMouseTweaksDisabled", "()Z");
        }
        if ("ae2.core.gui.AEGuiHandler".equals(targetClassName)) {
            return true;
        }
        return "cofh.thermalexpansion.block.machine.TileMachineBase".equals(targetClassName)
            || "cofh.core.block.TileCore".equals(targetClassName)
            || "cofh.core.block.TilePowered".equals(targetClassName)
            || "cofh.thermalexpansion.block.machine.BlockMachine".equals(targetClassName)
            || "cofh.core.gui.container.ContainerTileAugmentable".equals(targetClassName)
            || "cofh.thermalexpansion.gui.client.GuiPoweredBase".equals(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean hasMethod(ClassNode targetClass, String name, String desc) {
        for (MethodNode method : targetClass.methods) {
            if (name.equals(method.name) && desc.equals(method.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean targetClassHasMethod(String targetClassName, String name, String desc) {
        String resource = targetClassName.replace('.', '/') + ".class";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader == null
            ? ClassLoader.getSystemResourceAsStream(resource)
            : loader.getResourceAsStream(resource)) {
            if (stream == null) {
                return false;
            }
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return hasMethod(node, name, desc);
        } catch (Exception ignored) {
            return false;
        }
    }
}
