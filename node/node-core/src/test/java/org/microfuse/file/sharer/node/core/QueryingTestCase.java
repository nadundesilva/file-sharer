package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.bootstrap.BootstrapServer;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test Case for querying.
 */
public class QueryingTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(QueryingTestCase.class);

    private int delay;
    private int fileSharer1Port;
    private String localhostIP;
    private BootstrapServer bootstrapServer;
    private FileSharer[] fileSharers;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Querying Test");

        delay = 1000;
        fileSharer1Port = 9871;
        localhostIP = "127.0.0.1";
        bootstrapServer = new BootstrapServer();

        fileSharers = new FileSharer[10];
        for (int i = 0; i < fileSharers.length; i++) {
            fileSharers[i] = new FileSharer();
            fileSharers[i].getServiceHolder().getConfiguration().setIp(localhostIP);
            fileSharers[i].getServiceHolder().getConfiguration().setPeerListeningPort(fileSharer1Port + i);
            fileSharers[i].getServiceHolder().getConfiguration().setTimeToLive(10);
            fileSharers[i].getServiceHolder().getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
            fileSharers[i].getServiceHolder().getConfiguration().setNetworkHandlerType(NetworkHandlerType.TCP_SOCKET);
        }

        // Registering resources
        fileSharers[0].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("Lord of the Rings 2"), new OwnedResource("Cars"),
                new OwnedResource("Iron Man")
        ));
        fileSharers[1].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("Lord of the Rings"), new OwnedResource("Iron Man 2"),
                new OwnedResource("Spider Man")
        ));
        fileSharers[2].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("Hotel Transylvania"), new OwnedResource("How to train your Dragon"),
                new OwnedResource("The Bounty Hunter")
        ));
        fileSharers[3].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("Leap Year"), new OwnedResource("Leap Year"),
                new OwnedResource("Two weeks Notice")
        ));
        fileSharers[4].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("Me Before You"), new OwnedResource("Endless Love"),
                new OwnedResource("Life as we know it")
        ));
        fileSharers[5].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("How do you know"), new OwnedResource("The Last Song"),
                new OwnedResource("Thor")
        ));
        fileSharers[6].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("X-Men Origins"), new OwnedResource("Cars"),
                new OwnedResource("Captain America")
        ));
        fileSharers[7].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("22 Jump Street"), new OwnedResource("Iron Man 3"),
                new OwnedResource("Lord of the Rings")
        ));
        fileSharers[8].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("James Bond Sky fall"), new OwnedResource("Suicide Squad"),
                new OwnedResource("Fast and Furious")
        ));
        fileSharers[9].getServiceHolder().getResourceIndex().addAllOwnedResources(Arrays.asList(
                new OwnedResource("Teenage Mutant Ninja Turtles"), new OwnedResource("Underworld"),
                new OwnedResource("Despicable Me 3")
        ));

        bootstrapServer.start();
        waitFor(delay);

        for (int i = 0; i < fileSharers.length; i++) {
            fileSharers[i].getServiceHolder().getConfiguration().setMaxAssignedOrdinaryPeerCount(5);
            fileSharers[i].start();
            waitFor(delay);
            fileSharers[i].getServiceHolder().getOverlayNetworkManager().disableHeartBeat();
            fileSharers[i].getServiceHolder().getOverlayNetworkManager().disableGossiping();
        }

        // Fixing the network
        {
            fileSharers[0].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[0].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 1));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 3));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 6));
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 1));
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));

            fileSharers[0].getServiceHolder().getOverlayNetworkManager().gossip();
        }
        {
            fileSharers[1].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[1].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));
            ordinaryPeerRoutingTable.setAssignedSuperPeer(
                    new Node(localhostIP, fileSharer1Port));
        }
        {
            fileSharers[2].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[2].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 1));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 3));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 5));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 8));
            ordinaryPeerRoutingTable.setAssignedSuperPeer(new Node(localhostIP, fileSharer1Port));
        }
        {
            fileSharers[3].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[3].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 4));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 7));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 9));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 6));
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 4));
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 5));

            ResourceIndex resourceIndex = fileSharers[3].getServiceHolder().getResourceIndex();
            Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
            SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[4].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    new Node(
                            fileSharers[4].getServiceHolder().getConfiguration().getIp(),
                            fileSharers[4].getServiceHolder().getConfiguration().getPeerListeningPort()
                    )
            );
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[5].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    new Node(
                            fileSharers[5].getServiceHolder().getConfiguration().getIp(),
                            fileSharers[5].getServiceHolder().getConfiguration().getPeerListeningPort()
                    )
            );
        }
        {
            fileSharers[4].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[4].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 3));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 5));
            ordinaryPeerRoutingTable.setAssignedSuperPeer(
                    new Node(localhostIP, fileSharer1Port + 3));
        }
        {
            fileSharers[5].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[5].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 4));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 7));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 8));
            ordinaryPeerRoutingTable.setAssignedSuperPeer(
                    new Node(localhostIP, fileSharer1Port + 3));
        }
        {
            fileSharers[6].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[6].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 7));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 3));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 3));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 9));
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 7));
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 8));

            ResourceIndex resourceIndex = fileSharers[6].getServiceHolder().getResourceIndex();
            Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
            SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[7].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    new Node(
                            fileSharers[7].getServiceHolder().getConfiguration().getIp(),
                            fileSharers[7].getServiceHolder().getConfiguration().getPeerListeningPort()
                    )
            );
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[8].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    new Node(
                            fileSharers[8].getServiceHolder().getConfiguration().getIp(),
                            fileSharers[8].getServiceHolder().getConfiguration().getPeerListeningPort()
                    )
            );
        }
        {
            fileSharers[7].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[7].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 3));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 5));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 6));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 8));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 9));
            ordinaryPeerRoutingTable.setAssignedSuperPeer(
                    new Node(localhostIP, fileSharer1Port + 6));
        }
        {
            fileSharers[8].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[8].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 5));
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 7));
            ordinaryPeerRoutingTable.setAssignedSuperPeer(
                    new Node(localhostIP, fileSharer1Port + 6));
        }
        {
            fileSharers[9].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[9].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 2));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 3));
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 7));
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                    new Node(localhostIP, fileSharer1Port + 6));
        }
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up Querying Test");

        for (int i = 9; i >= 0; i--) {
            fileSharers[i].shutdown();
            waitFor(delay);
        }

        bootstrapServer.shutdown();
        waitFor(delay);
    }

    @Test(priority = 1)
    public void testQueryForExistingResource() {
        logger.info("Running Querying Test 01 - Query for existing resource");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Spider Man");
        waitFor(delay);

        List<AggregatedResource> resources =
                fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Spider Man");
        Assert.assertEquals(resources.size(), 1);
        Assert.assertEquals(resources.get(0).getName(), "Spider Man");
        Assert.assertEquals(resources.get(0).getNodeCount(), 1);
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 1)));
    }

    @Test(priority = 2)
    public void testQueryForExistingResourceWithHalfMatches() {
        logger.info("Running Querying Test 02 - Query for existing resource with half match");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Spider");
        waitFor(delay);

        List<AggregatedResource> resources =
                fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Spider");
        Assert.assertEquals(resources.size(), 1);
        Assert.assertEquals(resources.get(0).getName(), "Spider Man");
        Assert.assertEquals(resources.get(0).getNodeCount(), 1);
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 1)));
    }

    @Test(priority = 3)
    public void testQueryForExistingResourceWithMultipleCopies() {
        logger.info("Running Querying Test 03 - Query for existing resource with multiple copies");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Cars");
        waitFor(delay);

        List<AggregatedResource> resources =
                fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Cars");
        Assert.assertEquals(resources.size(), 1);
        Assert.assertEquals(resources.get(0).getName(), "Cars");
        Assert.assertEquals(resources.get(0).getNodeCount(), 2);
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
    }

    @Test(priority = 4)
    public void testMultipleQueriesForExistingResources() {
        logger.info("Running Querying Test 03 - Multiple queries for existing resources");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Cars");
        fileSharers[5].getServiceHolder().getQueryManager().query("Spider Man");
        fileSharers[5].getServiceHolder().getQueryManager().query("X-Men");
        fileSharers[5].getServiceHolder().getQueryManager().query("Lord");
        fileSharers[5].getServiceHolder().getQueryManager().query("Endless Love");
        fileSharers[5].getServiceHolder().getQueryManager().query("What happens in Rome stays in Rome");
        fileSharers[5].getServiceHolder().getQueryManager().query("Hotel Transylvania");
        fileSharers[5].getServiceHolder().getQueryManager().query("Lord of the Rings");
        fileSharers[5].getServiceHolder().getQueryManager().query("Captain America");
        fileSharers[5].getServiceHolder().getQueryManager().query("Iron Man");
        waitFor(delay);

        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Cars");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Cars");
            Assert.assertEquals(resources.get(0).getNodeCount(), 2);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Spider Man");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Spider Man");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 1)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("X-Men");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "X-Men Origins");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Lord");
            Assert.assertEquals(resources.size(), 2);
            Assert.assertEquals(resources.get(0).getName(), "Lord of the Rings");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 7)));
            Assert.assertEquals(resources.get(1).getName(), "Lord of the Rings 2");
            Assert.assertEquals(resources.get(1).getNodeCount(), 1);
            Assert.assertTrue(resources.get(1).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Endless Love");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Endless Love");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 4)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("What happens in Rome stays in Rome");
            Assert.assertEquals(resources.size(), 0);
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Hotel Transylvania");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Hotel Transylvania");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 2)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Lord of the Rings");
            Assert.assertEquals(resources.size(), 2);
            Assert.assertEquals(resources.get(0).getName(), "Lord of the Rings");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 7)));
            Assert.assertEquals(resources.get(1).getName(), "Lord of the Rings 2");
            Assert.assertEquals(resources.get(1).getNodeCount(), 1);
            Assert.assertTrue(resources.get(1).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Captain America");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Captain America");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Iron Man");
            Assert.assertEquals(resources.size(), 2);
            Assert.assertEquals(resources.get(0).getName(), "Iron Man 3");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 7)));
            Assert.assertEquals(resources.get(1).getName(), "Iron Man");
            Assert.assertEquals(resources.get(1).getNodeCount(), 1);
            Assert.assertTrue(resources.get(1).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
        }
    }
}
