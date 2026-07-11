package com.recursivepineapple.appliedthermal.integration.appflux;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.Upgrades;
import appeng.api.upgrades.IUpgradeableObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ReflectiveAppFluxBridge implements AppFluxBridge {

    private static final Logger LOG = LogManager.getLogger();
    private static final String APPFLUX_ITEM_AND_BLOCK = "com.glodblock.github.appflux.common.AFItemAndBlock";
    private static final String APPFLUX_FLUX_KEY = "com.glodblock.github.appflux.common.me.key.FluxKey";
    private static final String APPFLUX_ENERGY_TYPE = "com.glodblock.github.appflux.common.me.key.type.EnergyType";

    private final AEKey fluxFeKey;
    private final ItemLike inductionCard;
    private final Map<Class<?>, Optional<EnergyAccessor>> accessorCache = new ConcurrentHashMap<Class<?>, Optional<EnergyAccessor>>();

    private ReflectiveAppFluxBridge(AEKey fluxFeKey, ItemLike inductionCard) {
        this.fluxFeKey = fluxFeKey;
        this.inductionCard = inductionCard;
    }

    static Optional<AppFluxBridge> create() {
        try {
            Class<?> itemAndBlockClass = Class.forName(APPFLUX_ITEM_AND_BLOCK);
            Field inductionCardField = itemAndBlockClass.getField("INDUCTION_CARD");
            Object inductionCard = inductionCardField.get(null);
            if (!(inductionCard instanceof ItemLike itemLike)) {
                return Optional.empty();
            }

            Class<?> fluxKeyClass = Class.forName(APPFLUX_FLUX_KEY);
            Class<?> energyTypeClass = Class.forName(APPFLUX_ENERGY_TYPE);
            Object feEnum = Enum.valueOf((Class<? extends Enum>) energyTypeClass.asSubclass(Enum.class), "FE");
            Method ofMethod = fluxKeyClass.getMethod("of", energyTypeClass);
            Object fluxKey = ofMethod.invoke(null, feEnum);
            if (!(fluxKey instanceof AEKey aeKey)) {
                return Optional.empty();
            }

            return Optional.of(new ReflectiveAppFluxBridge(aeKey, itemLike));
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOG.warn("AppFlux integration unavailable: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public void registerUpgrade(ItemLike machineIcon) {
        if (machineIcon == null) {
            return;
        }

        Upgrades.add(inductionCard, machineIcon, 1);
    }

    @Override
    public boolean hasInductionCard(Object providerLogic) {
        return providerLogic instanceof IUpgradeableObject upgradeable
                && upgradeable.getUpgrades().isInstalled(inductionCard);
    }

    @Override
    public long transferEnergy(Object machine, MEStorage storage, IActionSource source) {
        Object energyStorage = findEnergyStorage(machine);
        if (energyStorage == null) {
            return 0L;
        }

        int room = invokeInt(energyStorage, "receiveEnergy", Integer.MAX_VALUE, true);
        if (room <= 0) {
            return 0L;
        }

        long simulated = storage.extract(fluxFeKey, room, Actionable.SIMULATE, source);
        if (simulated <= 0L) {
            return 0L;
        }

        long extracted = storage.extract(fluxFeKey, simulated, Actionable.MODULATE, source);
        if (extracted <= 0L) {
            return 0L;
        }

        int accepted = invokeInt(energyStorage, "receiveEnergy", (int) Math.min(extracted, Integer.MAX_VALUE), false);
        long refund = Math.max(0L, extracted - accepted);
        if (refund > 0L) {
            long returned = storage.insert(fluxFeKey, refund, Actionable.MODULATE, source);
            if (returned > 0L) {
                LOG.debug("Refunded {} FE to ME network after Thermal machine accepted only {} FE", returned, accepted);
            }
        }

        return accepted;
    }

    private Object findEnergyStorage(Object machine) {
        if (machine == null) {
            return null;
        }

        if (machine instanceof BlockEntity blockEntity) {
            Object storage = blockEntity.getCapability(ForgeCapabilities.ENERGY, null).orElse(null);
            if (isEnergyStorage(storage)) {
                return storage;
            }
        }

        Optional<EnergyAccessor> cached = accessorCache.computeIfAbsent(machine.getClass(),
                ReflectiveAppFluxBridge::discoverAccessor);
        if (cached.isEmpty()) {
            return null;
        }

        try {
            Object value = cached.get().get(machine);
            return isEnergyStorage(value) ? value : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static Optional<EnergyAccessor> discoverAccessor(Class<?> type) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            for (Method method : cursor.getDeclaredMethods()) {
                if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                String name = method.getName().toLowerCase(Locale.ROOT);
                if (!name.contains("energy")) {
                    continue;
                }
                method.setAccessible(true);
                return Optional.of(new MethodAccessor(method));
            }

            for (Field field : cursor.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                String name = field.getName().toLowerCase(Locale.ROOT);
                String typeName = field.getType().getSimpleName().toLowerCase(Locale.ROOT);
                if (!name.contains("energy") && !typeName.contains("energy")) {
                    continue;
                }

                field.setAccessible(true);
                return Optional.of(new FieldAccessor(field));
            }
        }

        return Optional.empty();
    }

    private static boolean isEnergyStorage(Object value) {
        if (value == null) {
            return false;
        }

        Class<?> type = value.getClass();
        return hasMethod(type, "receiveEnergy", int.class, boolean.class)
                && hasMethod(type, "getEnergyStored")
                && hasMethod(type, "getMaxEnergyStored");
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            type.getMethod(name, parameterTypes);
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private static int invokeInt(Object target, String methodName, int amount, boolean simulate) {
        try {
            Method method = target.getClass().getMethod(methodName, int.class, boolean.class);
            Object result = method.invoke(target, amount, simulate);
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException ex) {
            LOG.warn("Failed to invoke {} on {}: {}", methodName, target.getClass().getName(), ex.getMessage());
        }
        return 0;
    }

    private interface EnergyAccessor {
        Object get(Object machine) throws ReflectiveOperationException;
    }

    private static final class FieldAccessor implements EnergyAccessor {
        private final Field field;

        private FieldAccessor(Field field) {
            this.field = field;
        }

        @Override
        public Object get(Object machine) throws IllegalAccessException {
            return field.get(machine);
        }
    }

    private static final class MethodAccessor implements EnergyAccessor {
        private final Method method;

        private MethodAccessor(Method method) {
            this.method = method;
        }

        @Override
        public Object get(Object machine) throws InvocationTargetException, IllegalAccessException {
            return method.invoke(machine);
        }
    }
}
