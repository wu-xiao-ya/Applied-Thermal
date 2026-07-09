package com.recursivepineapple.appliedthermal.integration.ae2uel;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.tile.misc.TileInterface;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public final class InterfaceTerminalNodes {

    private InterfaceTerminalNodes() {
    }

    public static IMachineSet collect(IGrid grid, Class<? extends IGridHost> machineClass) {
        Set<IGridNode> nodes = new LinkedHashSet<>();
        for (IGridNode node : grid.getMachines(machineClass)) {
            nodes.add(node);
        }
        if (machineClass == TileInterface.class) {
            for (IGridNode node : grid.getNodes()) {
                if (node.getMachine() instanceof AppliedThermalMachine) {
                    AppliedThermalMachine machine = (AppliedThermalMachine) node.getMachine();
                    if (machine.appliedthermal$hasPatternProviderAugment()) {
                        nodes.add(node);
                    }
                }
            }
        }
        return new CombinedMachineSet(machineClass, nodes);
    }

    private static final class CombinedMachineSet implements IMachineSet {

        private final Class<? extends IGridHost> machineClass;
        private final Set<IGridNode> nodes;

        private CombinedMachineSet(Class<? extends IGridHost> machineClass, Set<IGridNode> nodes) {
            this.machineClass = machineClass;
            this.nodes = nodes;
        }

        @Override
        public Class<? extends IGridHost> getMachineClass() {
            return machineClass;
        }

        @Override
        public Iterator<IGridNode> iterator() {
            return nodes.iterator();
        }

        @Override
        public int size() {
            return nodes.size();
        }

        @Override
        public boolean isEmpty() {
            return nodes.isEmpty();
        }

        @Override
        public boolean contains(Object node) {
            return nodes.contains(node);
        }
    }
}
