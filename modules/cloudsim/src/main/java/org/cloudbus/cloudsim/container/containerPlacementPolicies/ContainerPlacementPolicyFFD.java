package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerVm;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ContainerPlacementPolicyFFD extends ContainerPlacementPolicy{

    @Override
    public ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList, List<Container> containerList) {


        Comparator<Container> compareByRam = (Container c1, Container c2) -> c1.getCurrentAllocatedRam().compareTo(c2.getCurrentAllocatedRam());
        vmList.sort()

        ContainerVm containerVm = null;
        for (ContainerVm containerVm1 : vmList) {
            if (excludedVmList.contains(containerVm1)) {
                continue;
            }
            containerVm = containerVm1;
            break;
        }
        return containerVm;
    }



    @Override
    public ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList) {

        ContainerVm containerVm = null;
        for (ContainerVm containerVm1 : vmList) {
            if (excludedVmList.contains(containerVm1)) {
                continue;
            }
            containerVm = containerVm1;
            break;
        }
        return containerVm;
    }

    @Override
    public  String getPolicyType_t(){
        return "FirstFitDecreasing";
    }
}
