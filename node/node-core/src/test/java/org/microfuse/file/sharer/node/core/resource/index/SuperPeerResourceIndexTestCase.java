package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex class.
 * <p>
 * Cannot mock classes since hashCode() and equals() methods are used in tests.
 */
public class SuperPeerResourceIndexTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerResourceIndexTestCase.class);

    private String resourceName1;
    private String resourceName2;
    private String resourceName3;
    private String resourceName4;
    private SuperPeerResourceIndex superPeerResourceIndex;

    private RoutingTable routingTable;
    private Node node1;
    private Node node2;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Super Peer Resource Index Test");

        serviceHolder.getConfiguration().setMaxUnstructuredPeerCount(100);
        serviceHolder.getConfiguration().setMaxAssignedOrdinaryPeerCount(100);
        serviceHolder.getConfiguration().setMaxSuperPeerCount(100);

        node1 = new Node("192.168.1.1", 6001);
        node2 = new Node("192.168.1.2", 6002);
        Node node3 = new Node("192.168.1.3", 6003);
        Node node4 = new Node("192.168.1.4", 6004);
        Node node5 = new Node("192.168.1.5", 6005);
        Node node6 = new Node("192.168.1.6", 6006);

        routingTable = serviceHolder.getRouter().getRoutingTable();
        routingTable.addUnstructuredNetworkRoutingTableEntry(node1.getIp(), node1.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node2.getIp(), node2.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node3.getIp(), node3.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node4.getIp(), node4.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node5.getIp(), node5.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node6.getIp(), node6.getPort());

        resourceName1 = "Lord of the Rings";
        resourceName2 = "Cars";
        resourceName3 = "Iron Man";
        resourceName4 = "Iron Man 2";

        superPeerResourceIndex = new SuperPeerResourceIndex(serviceHolder);

        superPeerResourceIndex.addAggregatedResource(resourceName1, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(resourceName1, node2.getIp(), node2.getPort());

        superPeerResourceIndex.addAggregatedResource(resourceName2, node4.getIp(), node4.getPort());
        superPeerResourceIndex.addAggregatedResource(resourceName2, node5.getIp(), node5.getPort());
        superPeerResourceIndex.addAggregatedResource(resourceName2, node6.getIp(), node6.getPort());

        superPeerResourceIndex.addAggregatedResource(resourceName3, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(resourceName3, node3.getIp(), node3.getPort());

        superPeerResourceIndex.addAggregatedResource(resourceName4, node2.getIp(), node2.getPort());
        superPeerResourceIndex.addAggregatedResource(resourceName4, node3.getIp(), node3.getPort());
        superPeerResourceIndex.addAggregatedResource(resourceName4, node6.getIp(), node6.getPort());
    }

    @Test(priority = 1)
    public void testAddResource() {
        logger.info("Running Super Peer Resource Index Test 01 - Add resource");

        String newAggreResourceResourceName = "Spider Man";
        Node newNode = new Node("192.168.1.1", 4067);
        serviceHolder.getRouter().getRoutingTable()
                .addUnstructuredNetworkRoutingTableEntry(newNode.getIp(), newNode.getPort());
        superPeerResourceIndex.addAggregatedResource(newAggreResourceResourceName, newNode.getIp(), newNode.getPort());

        Object ownedResourcesInternalState = Whitebox.getInternalState(superPeerResourceIndex, "aggregatedResources");
        Assert.assertTrue(ownedResourcesInternalState instanceof Set<?>);
        Set<?> resourceIndexOwnedResources = (Set<?>) ownedResourcesInternalState;
        Assert.assertEquals(resourceIndexOwnedResources.size(), 5);
        Assert.assertTrue(resourceIndexOwnedResources.contains(new AggregatedResource(newAggreResourceResourceName)));
    }

    @Test(priority = 2)
    public void testFindAggregatedResources() {
        logger.info("Running Super Peer Resource Index Test 02 - Find aggregated resources");

        Set<AggregatedResource> ironManResources =
                superPeerResourceIndex.findAggregatedResources(resourceName3);

        Assert.assertEquals(ironManResources.size(), 2);
        Assert.assertFalse(ironManResources.contains(new AggregatedResource(resourceName1)));
        Assert.assertFalse(ironManResources.contains(new AggregatedResource(resourceName2)));
        Assert.assertTrue(ironManResources.contains(new AggregatedResource(resourceName3)));
        Assert.assertTrue(ironManResources.contains(new AggregatedResource(resourceName4)));
    }

    @Test(priority = 3)
    public void testFindAggregatedResourcesWithNoMatches() {
        logger.info("Running Super Peer Resource Index Test 03 - Find aggregated resources with no matches");

        Set<AggregatedResource> spiderManResources =
                superPeerResourceIndex.findAggregatedResources("Spider Man");

        Assert.assertEquals(spiderManResources.size(), 0);
    }

    @Test(priority = 3)
    public void testFindAggregatedResourcesWithDuplicates() {
        logger.info("Running Super Peer Resource Index Test 04 - Find aggregated resources with duplicates");

        Node newNode = new Node("192.168.1.1", 4067);
        serviceHolder.getRouter().getRoutingTable()
                .addUnstructuredNetworkRoutingTableEntry(newNode.getIp(), newNode.getPort());
        superPeerResourceIndex.addAggregatedResource(resourceName2, newNode.getIp(), newNode.getPort());

        Set<AggregatedResource> carsResources = superPeerResourceIndex.findAggregatedResources(resourceName2);
        AggregatedResource carsResource = carsResources.stream().findAny().orElse(null);

        Assert.assertEquals(carsResources.size(), 1);
        Assert.assertNotNull(carsResource);
        Assert.assertEquals(carsResource.getNodeCount(), 4);
        Assert.assertTrue(carsResource.getAllNodes().contains(newNode));
    }

    @Test(priority = 1)
    public void testRemoveAggregatedResources() {
        logger.info("Running Super Peer Resource Index Test 05 - Remove aggregated resources");

        Node newNode = new Node("192.168.1.1", 4067);
        serviceHolder.getRouter().getRoutingTable()
                .addUnstructuredNetworkRoutingTableEntry(newNode.getIp(), newNode.getPort());
        String newResourceName = "Wonder Woman";

        superPeerResourceIndex.addAggregatedResource(newResourceName, newNode.getIp(), newNode.getPort());
        Set<AggregatedResource> wonderWomanResources = superPeerResourceIndex.findAggregatedResources(newResourceName);
        AggregatedResource wonderWomanResource = wonderWomanResources.stream().findAny().orElse(null);

        Assert.assertEquals(wonderWomanResources.size(), 1);
        Assert.assertNotNull(wonderWomanResource);
        Assert.assertEquals(wonderWomanResource.getNodeCount(), 1);
        Assert.assertTrue(wonderWomanResource.getAllNodes().contains(newNode));

        superPeerResourceIndex.removeAggregatedResource(newResourceName, newNode.getIp(), newNode.getPort());
        Set<AggregatedResource> updatedWonderWomanResources =
                superPeerResourceIndex.findAggregatedResources(newResourceName);

        Assert.assertEquals(wonderWomanResource.getNodeCount(), 0);
        Assert.assertEquals(updatedWonderWomanResources.size(), 0);
    }

    @Test(priority = 4)
    public void testGarbageCollectionWithInactiveNodesInAggregatedResources() {
        logger.info("Running Super Peer Resource Index Test 06 - " +
                "Garbage collection with inactive nodes in aggregated resources");

        routingTable.get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);
        routingTable.get(node2.getIp(), node2.getPort()).setState(NodeState.INACTIVE);
        superPeerResourceIndex.collectGarbage();

        Assert.assertEquals(superPeerResourceIndex.findAggregatedResources(resourceName1).size(), 0);
    }
}
