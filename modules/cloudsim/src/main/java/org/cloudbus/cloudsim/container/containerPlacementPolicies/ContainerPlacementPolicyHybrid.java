package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerVm;

import java.util.List;
import java.util.Set;

public class ContainerPlacementPolicyHybrid extends ContainerPlacementPolicy{
    @Override
    public ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList, List<Container> containerList) {

        ContainerVm selectedVM = null;
        for (ContainerVm i : vmList) {
            if (excludedVmList.contains(i)) {
                continue;
            }
            selectedVM = i;
            break;
        }
        return selectedVM;
    }

    //first fit
    @Override
    public ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList) {

        ContainerVm selectedVM = null;
        for (ContainerVm i : vmList) {
            if (excludedVmList.contains(i)) {
                continue;
            }
            selectedVM = i;
            break;
        }
        return selectedVM;
    }

}
