package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import org.cloudbus.cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import org.cloudbus.cloudsim.container.lists.PowerContainerList;
import org.cloudbus.cloudsim.container.lists.PowerContainerVmList;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;
import org.cloudbus.cloudsim.Log;
// import org.cloudbus.cloudsim.lists.VmList;

import java.util.*;

/*
 * Created by sareh on 11/08/15.
 */

class VMCompare implements Comparator<ContainerVm> {
    public int compare(ContainerVm v1, ContainerVm v2) {
        if (v1.getRam() > v2.getRam()) {
            return -1;
        } else {
            return 1;
        }
    }
}

class HostCompare implements Comparator<PowerContainerHost> {
    public int compare(PowerContainerHost v1, PowerContainerHost v2) {
        if (v1.getRam() > v2.getRam()) {
            return -1;
        } else {
            return 1;
        }
    }
}

public abstract class PowerContainerVmAllocationPolicyMigrationAbstractContainerHostSelection
        extends PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded {

    private HostSelectionPolicy hostSelectionPolicy;

    public PowerContainerVmAllocationPolicyMigrationAbstractContainerHostSelection(
            List<? extends ContainerHost> hostList, PowerContainerVmSelectionPolicy vmSelectionPolicy,
            PowerContainerSelectionPolicy containerSelectionPolicy, HostSelectionPolicy hostSelectionPolicy,
            int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
        super(hostList, vmSelectionPolicy, containerSelectionPolicy, numberOfVmTypes, vmPes, vmRam, vmBw, vmSize,
                vmMips);
        setHostSelectionPolicy(hostSelectionPolicy);
    }

    @Override
    public Map<String, Object> findHostForContainer(Container container, Set<? extends ContainerHost> excludedHosts,
            boolean checkForVM) {

        PowerContainerHost allocatedHost = null;
        ContainerVm allocatedVm = null;
        Map<String, Object> map = new HashMap<>();
        Set<ContainerHost> excludedHost1 = new HashSet<>();
        if (excludedHosts.size() == getContainerHostList().size()) {
            return map;
        }
        excludedHost1.addAll(excludedHosts);
        while (true) {
            if (getContainerHostList().size() == 0) {
                return map;
            }
            ContainerHost host = getHostSelectionPolicy().getHost(getContainerHostList(), container, excludedHost1);
            boolean findVm = false;
            List<ContainerVm> vmList = host.getVmList();
            PowerContainerVmList.sortByCpuUtilization(vmList);
            for (int i = 0; i < vmList.size(); i++) {
                ContainerVm vm = vmList.get(vmList.size() - 1 - i);
                if (checkForVM) {
                    if (vm.isInWaiting()) {

                        continue;
                    }

                }
                if (vm.isSuitableForContainer(container)) {

                    // if vm is overutilized or host would be overutilized after the allocation,
                    // this host is not chosen!
                    if (!isVmOverUtilized(vm)) {
                        continue;
                    }
                    if (getUtilizationOfCpuMips((PowerContainerHost) host) != 0
                            && isHostOverUtilizedAfterContainerAllocation((PowerContainerHost) host, vm, container)) {
                        continue;
                    }
                    vm.containerCreate(container);
                    allocatedVm = vm;
                    findVm = true;
                    allocatedHost = (PowerContainerHost) host;
                    break;

                }
            }
            if (findVm) {

                map.put("vm", allocatedVm);
                map.put("host", allocatedHost);
                map.put("container", container);
                excludedHost1.clear();
                return map;

            } else {
                excludedHost1.add(host);
                if (getContainerHostList().size() == excludedHost1.size()) {
                    excludedHost1.clear();
                    return map;
                }
            }
        }

    }

    static Integer max(Integer a, Integer b) {
        return a > b ? a : b;
    }

    @Override
    protected Collection<? extends Map<String, Object>> getContainerMigrationMapFromUnderUtilizedHosts(
            List<PowerContainerHostUtilizationHistory> overUtilizedHosts, List<Map<String, Object>> previouseMap) {

        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        List<PowerContainerHost> switchedOffHosts = getSwitchedOffHosts();

        // over-utilized hosts + hosts that are selected to migrate VMs to from
        // over-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingUnderUtilizedHost = new HashSet<>();
        excludedHostsForFindingUnderUtilizedHost.addAll(overUtilizedHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(switchedOffHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(previouseMap));

        // over-utilized + under-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingNewContainerPlacement = new HashSet<PowerContainerHost>();
        excludedHostsForFindingNewContainerPlacement.addAll(overUtilizedHosts);
        excludedHostsForFindingNewContainerPlacement.addAll(switchedOffHosts);

        int numberOfHosts = getContainerHostList().size();

        while (true) {
            if (numberOfHosts == excludedHostsForFindingUnderUtilizedHost.size()) {
                break;
            }

            PowerContainerHost underUtilizedHost = getUnderUtilizedHost(excludedHostsForFindingUnderUtilizedHost);
            if (underUtilizedHost == null) {
                break;
            }

            Log.printConcatLine("Under-utilized host: host #", underUtilizedHost.getId(), "\n");

            excludedHostsForFindingUnderUtilizedHost.add(underUtilizedHost);
            excludedHostsForFindingNewContainerPlacement.add(underUtilizedHost);

            List<? extends Container> containersToMigrateFromUnderUtilizedHost = getContainersToMigrateFromUnderUtilizedHost(
                    underUtilizedHost);
            if (containersToMigrateFromUnderUtilizedHost.isEmpty()) {
                continue;
            }

            Log.print("Reallocation of Containers from the under-utilized host: ");
            if (!Log.isDisabled()) {
                for (Container container : containersToMigrateFromUnderUtilizedHost) {
                    Log.print(container.getId() + " ");
                }
            }
            Log.printLine();

            List<Map<String, Object>> newContainerPlacement = getNewContainerPlacementFromUnderUtilizedHost(
                    containersToMigrateFromUnderUtilizedHost, excludedHostsForFindingNewContainerPlacement);
            // Sareh
            if (newContainerPlacement == null) {
                // Add the host to the placement founder option
                excludedHostsForFindingNewContainerPlacement.remove(underUtilizedHost);

            }

            excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(newContainerPlacement));
            // The migration map does not have a value for container since the whole vm
            // would be migrated.
            migrationMap.addAll(newContainerPlacement);
            Log.printLine();
        }

        switchedOffHosts.clear();
        excludedHostsForFindingUnderUtilizedHost.clear();
        excludedHostsForFindingNewContainerPlacement.clear();

        // Migration Map Optimization for Reducing Network Datafootprint
        List<PowerContainerHost> Hosts_t = new LinkedList<PowerContainerHost>();
        Set<PowerContainerHost> Hosts_temp_t = new HashSet<PowerContainerHost>();
        List<Container> Containers_t = new LinkedList<Container>();
        Set<ContainerVm> VMs_temp_t = new HashSet<ContainerVm>();
        List<ContainerVm> VMs_t = new LinkedList<ContainerVm>();
        List<Container> NotMigratedYet = new LinkedList<Container>();

        // DP optimized : Migration Map
        List<Map<String, Object>> DPmigrationMap = new LinkedList<Map<String, Object>>();

        // Obtaining the set of containers
        for (Map<String, Object> temp : migrationMap) {
            Hosts_temp_t.add((PowerContainerHost) temp.get("host"));
            Containers_t.add((Container) temp.get("container"));
            NotMigratedYet.add((Container) temp.get("container"));
            VMs_temp_t.add((ContainerVm) temp.get("vm"));
        }

        // Obtaining the set of VMs
        for (Object temp : VMs_temp_t) {
            VMs_t.add((ContainerVm) temp);
        }

        // Obtaining the set of Hosts
        for( Object temp: Hosts_temp_t) {
            Hosts_t.add((PowerContainerHost) temp);
        }

        // Sorting the Host list in descending order of their RAM Capacity
        Collections.sort(Hosts_t, new HostCompare());

        // Host based + Sized sorting of VMs
        List<ContainerVm> NEW_VMs_t = new LinkedList<ContainerVm>();

        for(PowerContainerHost i : Hosts_t) {
            Set<ContainerVm> temporaryVMs = new HashSet<ContainerVm>();
            for(Map<String, Object> tempIter : migrationMap) {

                if(tempIter.get("host") == i) {
                    temporaryVMs.add((ContainerVm) tempIter.get("vm"));
                }
            }
            List<ContainerVm> temporaryVmList = new LinkedList<ContainerVm>();
            for(ContainerVm j : temporaryVMs) {
                temporaryVmList.add((ContainerVm) j);
            }
            Collections.sort(temporaryVmList,new VMCompare());

            for(ContainerVm j : temporaryVmList) {
                NEW_VMs_t.add((ContainerVm) j );
            }
        }


        for (ContainerVm temp : NEW_VMs_t) {
            Integer W = Math.round(temp.getRam());
            List<Integer> weights = new LinkedList<Integer>();
            List<Integer> values = new LinkedList<Integer>();
            for (Container i : NotMigratedYet) {
                weights.add(Math.round(i.getRam()));
                values.add(Math.round((float) i.getWorkloadMips()));
            }
            List<List<Integer>> DP_Matrix = new LinkedList<List<Integer>>();
            // Initializing the matrix with zeroes
            for (int j = 0; j <= values.size(); ++j) {
                DP_Matrix.add(new LinkedList<Integer>());
                for (int k = 0; k <= W; ++k) {
                    DP_Matrix.get(j).add(0);
                }
            }

            for (int j = 0; j <= values.size(); ++j) {
                for (int k = 0; k <= W; ++k) {
                    if (j == 0 || k == 0)
                        DP_Matrix.get(j).set(k, 0);
                    else if (weights.get(j - 1) <= k) {
                        DP_Matrix.get(j).set(k,
                                max(values.get(j - 1) + DP_Matrix.get(j - 1).get(k - weights.get(j - 1)),
                                        DP_Matrix.get(j - 1).get(k)));
                    } else {
                        DP_Matrix.get(j).set(k, DP_Matrix.get(j - 1).get(k));
                    }
                }

            }


            int i_t = values.size();
            int j_t = W;
            while (i_t > 0 && j_t >= 0) {
                if (DP_Matrix.get(i_t).get(j_t) == DP_Matrix.get(i_t - 1).get(j_t)) {
                    --i_t;
                } else {


                    // Getting the already existing host details and VM details
                    Container tempContainer = Containers_t.get(i_t - 1);

                    for(Map<String, Object> tempIter : migrationMap) {
                        Map<String, Object> map = new HashMap<>();
                        if(temp == tempIter.get("vm")) {
                            map.put("host", tempIter.get("host") );
                            map.put("container",tempContainer);
                            map.put("vm",temp);
                            DPmigrationMap.add(map);
                        }


                        break;
                    }
//                    for (Map<String, Object> tempIter : migrationMap) {
//                        if (tempIter.get("container") == tempContainer) {
//                            Map<String, Object> map = new HashMap<>();
//                            for (Map<String, Object> tempIter1 : migrationMap) {
//                                if (temp == tempIter1.get("vm")) {
//                                    map.put("host", tempIter1.get("host"));
//                                }
//                            }
//                            map.put("vm", temp);
//                            // map.put("host", tempIter.get("host"));
//                            map.put("container", tempContainer);
//                            DPmigrationMap.add(map);
//                            break;
//
//                        }
//                    }

                    int tempValueforJ = DP_Matrix.get(i_t).get(j_t) - values.get(i_t - 1);
                    --i_t;
                    while (DP_Matrix.get(i_t).get(j_t) != tempValueforJ && j_t >= 0) {
                        --j_t;
                    }
                }
            }

            for (Map<String, Object> tempIter : DPmigrationMap) {
                NotMigratedYet.remove(tempIter.get("container"));
            }

        }
        /*
        // ------------------------------------------------------------------------------------------------------------------------
        // DP Migration VM and Host
        List<ContainerVm> VMH_VMs_t = new LinkedList<ContainerVm>();
        Set<PowerContainerHost> VMH_Hosts_temp_t = new HashSet<PowerContainerHost>();
        List<PowerContainerHost> VMH_Hosts_t = new LinkedList<PowerContainerHost>();
        List<ContainerVm> VMH_NotMigratedYet = new LinkedList<ContainerVm>();

        List<Map<String, Object>> VMH_DPmigrationMap = new LinkedList<Map<String, Object>>();

        System.out.println("HEllo1");

        // Obtaining the set of containers
        for (Map<String, Object> temp : DPmigrationMap) {

            VMH_VMs_t.add((ContainerVm) temp.get("vm"));
            VMH_NotMigratedYet.add((ContainerVm) temp.get("vm"));
            VMH_Hosts_temp_t.add((PowerContainerHost) temp.get("host"));
        }

        // Obtaining the set of VMs
        for (Object temp : VMH_Hosts_temp_t) {
            VMH_Hosts_t.add((PowerContainerHost) temp);
        }

        // Sorting the VM list in descending order of their RAM Capacity
        System.out.println("HEllo2");
        if(VMH_Hosts_t.size() > 1) {
            Collections.sort(VMH_Hosts_t, new HostCompare());
        }

        for (PowerContainerHost temp : VMH_Hosts_t) {

            System.out.println("HEllo3");

            Integer W = Math.round(temp.getRam());

            System.out.println(W);
            List<Integer> weights = new LinkedList<Integer>();
            List<Integer> values = new LinkedList<Integer>();
            for (ContainerVm i : VMH_NotMigratedYet) {
                weights.add(Math.round(i.getRam()));
                values.add(Math.round((float) i.getTotalMips()));
            }
            List<List<Integer>> DP_Matrix = new LinkedList<List<Integer>>();
            // Initializing the matrix with zeroes
            for (int j = 0; j <= values.size(); ++j) {
                DP_Matrix.add(new LinkedList<Integer>());
                for (int k = 0; k <= W; ++k) {
                    DP_Matrix.get(j).add(0);
                }
            }

            for (int j = 0; j <= values.size(); ++j) {
                for (int k = 0; k <= W; ++k) {
                    if (j == 0 || k == 0)
                        DP_Matrix.get(j).set(k, 0);
                    else if (weights.get(j - 1) <= k) {
                        DP_Matrix.get(j).set(k,
                                max(values.get(j - 1) + DP_Matrix.get(j - 1).get(k - weights.get(j - 1)),
                                        DP_Matrix.get(j - 1).get(k)));
                    } else {
                        DP_Matrix.get(j).set(k, DP_Matrix.get(j - 1).get(k));
                    }
                }

            }


            int i_t = values.size();
            int j_t = W;
            while (i_t > 0 && j_t >= 0) {
                System.out.println("Hehe");
                if (DP_Matrix.get(i_t).get(j_t) == DP_Matrix.get(i_t - 1).get(j_t)) {
                    --i_t;
                } else {

                    ContainerVm tempVM = VMH_VMs_t.get(i_t - 1);

//                    for (Map<String, Object> tempIter : DPmigrationMap) {
//                        if (tempIter.get("vm") == tempVM) {
//                            Map<String, Object> map = new HashMap<>();
//                            for (Map<String, Object> tempIter1 : migrationMap) {
//                                if (tempIter.get("vm") == tempIter1.get("vm")) {
//                                    map.put("container", tempIter1.get("container"));
//                                }
//                            }
//                            map.put("host", temp);
//                            // map.put("host", tempIter.get("host"));
//                            map.put("container", tempVM);
//                            DPmigrationMap.add(map);
//                            break;
//
//                        }
//                    }

                    for(Map<String, Object> tempIter : DPmigrationMap) {
                        if(tempIter.get("vm") == tempVM) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("vm",tempVM);
                            map.put("host",temp);
                            map.put("container", tempIter.get("container"));

                            VMH_DPmigrationMap.add(map);
                        }
                    }

                    int tempValueforJ = DP_Matrix.get(i_t).get(j_t) - values.get(i_t - 1);
                    --i_t;
                    while (DP_Matrix.get(i_t).get(j_t) != tempValueforJ && j_t >= 0) {
                        --j_t;
                    }
                }
            }

            for (Map<String, Object> tempIter : VMH_DPmigrationMap) {
                NotMigratedYet.remove(tempIter.get("container"));
            }

        }
        System.out.println("HEllo");
        */
        return DPmigrationMap;
        // return migrationMap;

    }

    /*
     * Gets the vms to migrate from under utilized host.
     *
     * @param host the host
     *
     * @return the vms to migrate from under utilized host
     */
    protected List<? extends Container> getContainersToMigrateFromUnderUtilizedHost(PowerContainerHost host) {
        List<Container> containersToMigrate = new LinkedList<>();
        for (ContainerVm vm : host.getVmList()) {
            if (!vm.isInMigration()) {
                for (Container container : vm.getContainerList()) {
                    if (!container.isInMigration()) {
                        containersToMigrate.add(container);
                    }
                }
            }
        }
        return containersToMigrate;
    }

    /**
     * Gets the new vm placement from under utilized host.
     *
     * @param containersToMigrate the vms to migrate
     * @param excludedHosts       the excluded hosts
     * @return the new vm placement from under utilized host
     */
    protected List<Map<String, Object>> getNewContainerPlacementFromUnderUtilizedHost(
            List<? extends Container> containersToMigrate, Set<? extends ContainerHost> excludedHosts) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        PowerContainerList.sortByCpuUtilization(containersToMigrate);
        for (Container container : containersToMigrate) {
            Map<String, Object> allocatedMap = findHostForContainer(container, excludedHosts, true);
            if (allocatedMap.get("vm") != null && allocatedMap.get("host") != null) {

                Log.printConcatLine("Container# ", container.getId(), "allocated to VM # ",
                        ((ContainerVm) allocatedMap.get("vm")).getId(), " on host# ",
                        ((ContainerHost) allocatedMap.get("host")).getId());
                migrationMap.add(allocatedMap);
            } else {
                Log.printLine("Not all Containers can be reallocated from the host, reallocation cancelled");
                allocatedMap.clear();
                migrationMap.clear();
                break;
            }
        }
        return migrationMap;
    }

    @Override
    protected Map<String, Object> findAvailableHostForContainer(Container container,
            List<Map<String, Object>> createdVm) {
        PowerContainerHost allocatedHost = null;
        ContainerVm allocatedVm = null;
        Map<String, Object> map = new HashMap<>();
        Set<ContainerHost> excludedHost1 = new HashSet<>();
        List<ContainerHost> underUtilizedHostList = new ArrayList<>();
        for (Map<String, Object> map1 : createdVm) {
            ContainerHost host = (ContainerHost) map1.get("host");
            if (!underUtilizedHostList.contains(host)) {
                underUtilizedHostList.add(host);
            }

        }

        while (true) {

            ContainerHost host = getHostSelectionPolicy().getHost(underUtilizedHostList, container, excludedHost1);
            List<ContainerVm> vmList = new ArrayList<>();

            for (Map<String, Object> map2 : createdVm) {
                if (map2.get("host") == host) {
                    vmList.add((ContainerVm) map2.get("vm"));
                }

            }

            boolean findVm = false;

            PowerContainerVmList.sortByCpuUtilization(vmList);
            for (int i = 0; i < vmList.size(); i++) {

                ContainerVm vm = vmList.get(vmList.size() - 1 - i);
                if (vm.isSuitableForContainer(container)) {

                    // if vm is overutilized or host would be overutilized after the allocation,
                    // this host is not chosen!
                    if (!isVmOverUtilized(vm)) {
                        continue;
                    }

                    vm.containerCreate(container);
                    allocatedVm = vm;
                    findVm = true;
                    allocatedHost = (PowerContainerHost) host;
                    break;

                }
            }
            if (findVm) {

                map.put("vm", allocatedVm);
                map.put("host", allocatedHost);
                map.put("container", container);
                excludedHost1.clear();
                return map;

            } else {
                if (host != null) {
                    excludedHost1.add(host);
                }
                if (underUtilizedHostList.size() == excludedHost1.size()) {
                    excludedHost1.clear();
                    return map;
                }
            }

        }

    }

    public void setHostSelectionPolicy(HostSelectionPolicy hostSelectionPolicy) {
        this.hostSelectionPolicy = hostSelectionPolicy;
    }

    public HostSelectionPolicy getHostSelectionPolicy() {
        return hostSelectionPolicy;
    }
}