package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerVm;

import java.util.List;
import java.util.Set;

/**
 *  Created by sareh fotuhi Piraghaj on 16/12/15.
 *  For writing any container placement policies this class should be extend.
 */

public abstract class ContainerPlacementPolicy {
    /**
     * Gets the VM List, and the excluded VMs
     *
     * @param vmList the host
     * @return the destination vm to place container
     */
    public String policyType_t;

    public abstract ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList);

    public  ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList,List<Container> containerList){
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

    public abstract String getPolicyType_t();

}
