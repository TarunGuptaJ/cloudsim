package org.cloudbus.cloudsim.container.resourceAllocators;

import org.cloudbus.cloudsim.container.containerPlacementPolicies.ContainerPlacementPolicy;
import org.cloudbus.cloudsim.container.core.Container;

import java.util.List;
import java.util.Map;

/**
 * Created by sareh on 16/07/15.
 */
public class PowerContainerAllocationPolicySimple extends PowerContainerAllocationPolicy {



    public PowerContainerAllocationPolicySimple() {
        super();
    }

    public PowerContainerAllocationPolicySimple(ContainerPlacementPolicy containerPlacementPolicy1) {
        super(containerPlacementPolicy1);
    }
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Container> containerList) {
        return null;
    }
}
