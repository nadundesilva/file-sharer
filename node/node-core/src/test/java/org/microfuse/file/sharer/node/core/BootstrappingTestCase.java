package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.bootstrap.BootstrapServer;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageConstants;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test Case for testing interactions with the bootstrap server.
 */
public class BootstrappingTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BootstrappingTestCase.class);

    private int delay;
    private BootstrapServer bootstrapServer;
    private FileSharer fileSharer;
    private Node node;
    private Node node1;
    private Node node2;
    private Node node3;
    private Node node4;
    private Node node5;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Bootstrapping Test");

        String localhostIP = "127.0.0.1";

        delay = 1000;
        bootstrapServer = new BootstrapServer();
        fileSharer = new FileSharer();
        Whitebox.setInternalState(fileSharer, "serviceHolder", serviceHolder);

        node = new Node();
        node.setIp(serviceHolder.getConfiguration().getIp());
        node.setPort(serviceHolder.getConfiguration().getPeerListeningPort());

        node1 = new Node();
        node1.setIp(localhostIP);
        node1.setPort(9761);

        node2 = new Node();
        node2.setIp(localhostIP);
        node2.setPort(9452);

        node3 = new Node();
        node3.setIp(localhostIP);
        node3.setPort(9353);

        node4 = new Node();
        node4.setIp(localhostIP);
        node4.setPort(9644);

        node5 = new Node();
        node5.setIp(localhostIP);
        node5.setPort(9415);

        bootstrapServer.start();
        waitFor(delay);
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up Bootstrapping Test");

        bootstrapServer.shutdown();
        waitFor(delay);
    }

    @Test(priority = 1)
    public void testRegister() {
        logger.info("Running Bootstrapping Test 01 - Register in the bootstrap server");

        Message message = new Message();
        message.setType(MessageType.REG_OK);
        message.setData(MessageIndexes.REG_OK_NODES_COUNT, "0");

        serviceHolder.getOverlayNetworkManager();
        Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
        Assert.assertTrue(internalStateRouter instanceof Router);
        Router router = (Router) internalStateRouter;

        RouterListener routerListener = Mockito.mock(RouterListener.class);
        router.registerListener(routerListener);

        fileSharer.start();
        waitFor(delay);

        try {
            Assert.assertEquals(bootstrapServer.getAllNodes().size(), 1);
            Assert.assertEquals(bootstrapServer.getAllNodes().get(0).getIp(),
                    serviceHolder.getConfiguration().getIp());
            Assert.assertEquals(bootstrapServer.getAllNodes().get(0).getPort(),
                    serviceHolder.getConfiguration().getPeerListeningPort());
            Mockito.verify(routerListener, Mockito.times(1))
                    .onMessageReceived(serviceHolder.getConfiguration().getBootstrapServer(), message);
        } finally {
            fileSharer.leaveNetwork();
            waitFor(delay);
            fileSharer.shutdown();
            waitFor(delay);
        }
    }

    @Test(priority = 2)
    public void testUnregister() {
        logger.info("Running Bootstrapping Test 02 - Unregister from the bootstrap server");

        fileSharer.start();
        waitFor(delay);

        try {
            Message regOkMessage = new Message();
            regOkMessage.setType(MessageType.REG_OK);
            regOkMessage.setData(MessageIndexes.REG_OK_NODES_COUNT, "0");

            Message unRegOkMessage = new Message();
            unRegOkMessage.setType(MessageType.UNREG_OK);
            unRegOkMessage.setData(MessageIndexes.UNREG_OK_VALUE, MessageConstants.UNREG_OK_VALUE_SUCCESS);

            Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
            Assert.assertTrue(internalStateRouter instanceof Router);
            Router router = (Router) internalStateRouter;

            RouterListener routerListener = Mockito.mock(RouterListener.class);
            router.registerListener(routerListener);

            fileSharer.leaveNetwork();
            waitFor(delay);

            Assert.assertEquals(bootstrapServer.getAllNodes().size(), 0);
            Mockito.verify(routerListener, Mockito.times(1))
                    .onMessageReceived(serviceHolder.getConfiguration().getBootstrapServer(), unRegOkMessage);
        } finally {
            fileSharer.shutdown();
            waitFor(delay);
        }
    }

    @Test(priority = 3)
    public void testFirstNodeRegister() {
        logger.info("Running Bootstrapping Test 03 - Register first node");

        fileSharer.start();
        waitFor(delay);

        try {
            serviceHolder.getOverlayNetworkManager();
            Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
            Assert.assertTrue(internalStateRouter instanceof Router);
            Router router = (Router) internalStateRouter;

            Assert.assertEquals(serviceHolder.getPeerType(), PeerType.SUPER_PEER);

            RoutingTable routingTable = router.getRoutingTable();
            Assert.assertTrue(routingTable instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;

            Assert.assertEquals(superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 0);
            Assert.assertEquals(superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 0);
            Assert.assertEquals(superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes().size(), 0);
        } finally {
            fileSharer.leaveNetwork();
            waitFor(delay);
            fileSharer.shutdown();
            waitFor(delay);
        }
    }

    @Test(priority = 4)
    public void testSecondNodeRegister() {
        logger.info("Running Bootstrapping Test 04 - Register second node");

        FileSharer fileSharer1 = new FileSharer();
        Object internalStateServiceHolder1 = Whitebox.getInternalState(fileSharer1, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder1 instanceof ServiceHolder);
        ServiceHolder serviceHolder1 = (ServiceHolder) internalStateServiceHolder1;
        serviceHolder1.getConfiguration().setIp(node1.getIp());
        serviceHolder1.getConfiguration().setPeerListeningPort(node1.getPort());

        fileSharer1.start();
        waitFor(delay);

        fileSharer.start();
        waitFor(delay);

        try {
            {
                serviceHolder1.getOverlayNetworkManager();
                Object internalStateRouter1 = Whitebox.getInternalState(serviceHolder1, "router");
                Assert.assertTrue(internalStateRouter1 instanceof Router);
                Router router1 = (Router) internalStateRouter1;

                Assert.assertEquals(serviceHolder1.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable1 = router1.getRoutingTable();
                Assert.assertTrue(routingTable1 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable1 = (SuperPeerRoutingTable) routingTable1;

                Assert.assertEquals(superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node));

                Assert.assertEquals(superPeerRoutingTable1.getAllSuperPeerNetworkRoutingTableNodes().size(), 0);
            }
            {
                serviceHolder.getOverlayNetworkManager();
                Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
                Assert.assertTrue(internalStateRouter instanceof Router);
                Router router = (Router) internalStateRouter;

                Assert.assertEquals(serviceHolder.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable = router.getRoutingTable();
                Assert.assertTrue(routingTable instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;

                Assert.assertEquals(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node1));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node1);
            }
        } finally {
            fileSharer.leaveNetwork();
            waitFor(delay);
            fileSharer.shutdown();
            waitFor(delay);

            fileSharer1.leaveNetwork();
            waitFor(delay);
            fileSharer1.shutdown();
            waitFor(delay);
        }
    }

    @Test(priority = 5)
    public void testThirdNodeRegister() {
        logger.info("Running Bootstrapping Test 05 - Register third node");

        FileSharer fileSharer1 = new FileSharer();
        Object internalStateServiceHolder1 = Whitebox.getInternalState(fileSharer1, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder1 instanceof ServiceHolder);
        ServiceHolder serviceHolder1 = (ServiceHolder) internalStateServiceHolder1;
        serviceHolder1.getConfiguration().setIp(node1.getIp());
        serviceHolder1.getConfiguration().setPeerListeningPort(node1.getPort());

        FileSharer fileSharer2 = new FileSharer();
        Object internalStateServiceHolder2 = Whitebox.getInternalState(fileSharer2, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder2 instanceof ServiceHolder);
        ServiceHolder serviceHolder2 = (ServiceHolder) internalStateServiceHolder2;
        serviceHolder2.getConfiguration().setIp(node2.getIp());
        serviceHolder2.getConfiguration().setPeerListeningPort(node2.getPort());

        fileSharer1.start();
        waitFor(delay);

        fileSharer2.start();
        waitFor(delay);

        fileSharer.start();
        waitFor(delay);

        try {
            {
                serviceHolder1.getOverlayNetworkManager();
                Object internalStateRouter1 = Whitebox.getInternalState(serviceHolder1, "router");
                Assert.assertTrue(internalStateRouter1 instanceof Router);
                Router router1 = (Router) internalStateRouter1;

                Assert.assertEquals(serviceHolder1.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable1 = router1.getRoutingTable();
                Assert.assertTrue(routingTable1 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable1 = (SuperPeerRoutingTable) routingTable1;

                Assert.assertEquals(superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().size(), 2);
                Assert.assertTrue(superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node));
                Assert.assertTrue(superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node2));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node2));

                Assert.assertEquals(superPeerRoutingTable1.getAllSuperPeerNetworkRoutingTableNodes().size(), 0);
            }
            {
                serviceHolder2.getOverlayNetworkManager();
                Object internalStateRouter2 = Whitebox.getInternalState(serviceHolder2, "router");
                Assert.assertTrue(internalStateRouter2 instanceof Router);
                Router router2 = (Router) internalStateRouter2;

                Assert.assertEquals(serviceHolder2.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable2 = router2.getRoutingTable();
                Assert.assertTrue(routingTable2 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable2 = (OrdinaryPeerRoutingTable) routingTable2;

                Assert.assertEquals(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().size(), 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node));
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node1));

                Assert.assertEquals(ordinaryPeerRoutingTable2.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder.getOverlayNetworkManager();
                Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
                Assert.assertTrue(internalStateRouter instanceof Router);
                Router router = (Router) internalStateRouter;

                Assert.assertEquals(serviceHolder.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable = router.getRoutingTable();
                Assert.assertTrue(routingTable instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;

                Assert.assertEquals(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node1));
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node2));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node1);
            }
        } finally {
            fileSharer.leaveNetwork();
            waitFor(delay);
            fileSharer.shutdown();
            waitFor(delay);

            fileSharer2.leaveNetwork();
            waitFor(delay);
            fileSharer2.shutdown();
            waitFor(delay);

            fileSharer1.leaveNetwork();
            waitFor(delay);
            fileSharer1.shutdown();
            waitFor(delay);
        }
    }

    @Test(priority = 6)
    public void testForthNodeRegister() {
        logger.info("Running Bootstrapping Test 06 - Register forth node");

        FileSharer fileSharer1 = new FileSharer();
        Object internalStateServiceHolder1 = Whitebox.getInternalState(fileSharer1, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder1 instanceof ServiceHolder);
        ServiceHolder serviceHolder1 = (ServiceHolder) internalStateServiceHolder1;
        serviceHolder1.getConfiguration().setIp(node1.getIp());
        serviceHolder1.getConfiguration().setPeerListeningPort(node1.getPort());

        FileSharer fileSharer2 = new FileSharer();
        Object internalStateServiceHolder2 = Whitebox.getInternalState(fileSharer2, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder2 instanceof ServiceHolder);
        ServiceHolder serviceHolder2 = (ServiceHolder) internalStateServiceHolder2;
        serviceHolder2.getConfiguration().setIp(node2.getIp());
        serviceHolder2.getConfiguration().setPeerListeningPort(node2.getPort());

        FileSharer fileSharer3 = new FileSharer();
        Object internalStateServiceHolder3 = Whitebox.getInternalState(fileSharer3, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder3 instanceof ServiceHolder);
        ServiceHolder serviceHolder3 = (ServiceHolder) internalStateServiceHolder3;
        serviceHolder3.getConfiguration().setIp(node3.getIp());
        serviceHolder3.getConfiguration().setPeerListeningPort(node3.getPort());

        fileSharer1.start();
        waitFor(delay);

        fileSharer2.start();
        waitFor(delay);

        fileSharer3.start();
        waitFor(delay);

        fileSharer.start();
        waitFor(delay);

        try {
            {
                serviceHolder1.getOverlayNetworkManager();
                Object internalStateRouter1 = Whitebox.getInternalState(serviceHolder1, "router");
                Assert.assertTrue(internalStateRouter1 instanceof Router);
                Router router1 = (Router) internalStateRouter1;

                Assert.assertEquals(serviceHolder1.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable1 = router1.getRoutingTable();
                Assert.assertTrue(routingTable1 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable1 = (SuperPeerRoutingTable) routingTable1;

                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node3));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 3);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node3));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllSuperPeerNetworkRoutingTableNodes().size(), 0);
            }
            {
                serviceHolder2.getOverlayNetworkManager();
                Object internalStateRouter2 = Whitebox.getInternalState(serviceHolder2, "router");
                Assert.assertTrue(internalStateRouter2 instanceof Router);
                Router router2 = (Router) internalStateRouter2;

                Assert.assertEquals(serviceHolder2.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable2 = router2.getRoutingTable();
                Assert.assertTrue(routingTable2 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable2 = (OrdinaryPeerRoutingTable) routingTable2;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node3));

                Assert.assertEquals(ordinaryPeerRoutingTable2.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder3.getOverlayNetworkManager();
                Object internalStateRouter3 = Whitebox.getInternalState(serviceHolder3, "router");
                Assert.assertTrue(internalStateRouter3 instanceof Router);
                Router router3 = (Router) internalStateRouter3;

                Assert.assertEquals(serviceHolder3.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable3 = router3.getRoutingTable();
                Assert.assertTrue(routingTable3 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable3 = (OrdinaryPeerRoutingTable) routingTable3;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node2));

                Assert.assertEquals(ordinaryPeerRoutingTable3.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder.getOverlayNetworkManager();
                Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
                Assert.assertTrue(internalStateRouter instanceof Router);
                Router router = (Router) internalStateRouter;

                Assert.assertEquals(serviceHolder.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable = router.getRoutingTable();
                Assert.assertTrue(routingTable instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node3));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node1);
            }
        } finally {
            fileSharer.leaveNetwork();
            waitFor(delay);
            fileSharer.shutdown();
            waitFor(delay);

            fileSharer3.leaveNetwork();
            waitFor(delay);
            fileSharer3.shutdown();
            waitFor(delay);

            fileSharer2.leaveNetwork();
            waitFor(delay);
            fileSharer2.shutdown();
            waitFor(delay);

            fileSharer1.leaveNetwork();
            waitFor(delay);
            fileSharer1.shutdown();
            waitFor(delay);
        }
    }

    @Test(priority = 7)
    public void testSecondSuperPeerPromotion() {
        logger.info("Running Bootstrapping Test 07 - Register a node when the assigned ordinary peer count is maxed");

        FileSharer fileSharer1 = new FileSharer();
        Object internalStateServiceHolder1 = Whitebox.getInternalState(fileSharer1, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder1 instanceof ServiceHolder);
        ServiceHolder serviceHolder1 = (ServiceHolder) internalStateServiceHolder1;
        serviceHolder1.getConfiguration().setIp(node1.getIp());
        serviceHolder1.getConfiguration().setPeerListeningPort(node1.getPort());

        FileSharer fileSharer2 = new FileSharer();
        Object internalStateServiceHolder2 = Whitebox.getInternalState(fileSharer2, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder2 instanceof ServiceHolder);
        ServiceHolder serviceHolder2 = (ServiceHolder) internalStateServiceHolder2;
        serviceHolder2.getConfiguration().setIp(node2.getIp());
        serviceHolder2.getConfiguration().setPeerListeningPort(node2.getPort());

        FileSharer fileSharer3 = new FileSharer();
        Object internalStateServiceHolder3 = Whitebox.getInternalState(fileSharer3, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder3 instanceof ServiceHolder);
        ServiceHolder serviceHolder3 = (ServiceHolder) internalStateServiceHolder3;
        serviceHolder3.getConfiguration().setIp(node3.getIp());
        serviceHolder3.getConfiguration().setPeerListeningPort(node3.getPort());

        FileSharer fileSharer4 = new FileSharer();
        Object internalStateServiceHolder4 = Whitebox.getInternalState(fileSharer4, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder4 instanceof ServiceHolder);
        ServiceHolder serviceHolder4 = (ServiceHolder) internalStateServiceHolder4;
        serviceHolder4.getConfiguration().setIp(node4.getIp());
        serviceHolder4.getConfiguration().setPeerListeningPort(node4.getPort());

        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(3);
        fileSharer1.start();
        waitFor(delay);

        fileSharer2.start();
        waitFor(delay);

        fileSharer3.start();
        waitFor(delay);

        fileSharer4.start();
        waitFor(delay);

        serviceHolder.getConfiguration().setSerSuperPeerTimeout(delay);
        fileSharer.start();
        waitFor(delay * 2);

        try {
            {
                serviceHolder1.getOverlayNetworkManager();
                Object internalStateRouter1 = Whitebox.getInternalState(serviceHolder1, "router");
                Assert.assertTrue(internalStateRouter1 instanceof Router);
                Router router1 = (Router) internalStateRouter1;

                Assert.assertEquals(serviceHolder1.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable1 = router1.getRoutingTable();
                Assert.assertTrue(routingTable1 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable1 = (SuperPeerRoutingTable) routingTable1;

                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 3);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node3));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllSuperPeerNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable1.getAllSuperPeerNetworkRoutingTableNodes().contains(node));
            }
            {
                serviceHolder2.getOverlayNetworkManager();
                Object internalStateRouter2 = Whitebox.getInternalState(serviceHolder2, "router");
                Assert.assertTrue(internalStateRouter2 instanceof Router);
                Router router2 = (Router) internalStateRouter2;

                Assert.assertEquals(serviceHolder2.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable2 = router2.getRoutingTable();
                Assert.assertTrue(routingTable2 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable2 = (OrdinaryPeerRoutingTable) routingTable2;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node4));

                Assert.assertEquals(ordinaryPeerRoutingTable2.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder3.getOverlayNetworkManager();
                Object internalStateRouter3 = Whitebox.getInternalState(serviceHolder3, "router");
                Assert.assertTrue(internalStateRouter3 instanceof Router);
                Router router3 = (Router) internalStateRouter3;

                Assert.assertEquals(serviceHolder3.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable3 = router3.getRoutingTable();
                Assert.assertTrue(routingTable3 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable3 = (OrdinaryPeerRoutingTable) routingTable3;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node4));

                Assert.assertEquals(ordinaryPeerRoutingTable3.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder4.getOverlayNetworkManager();
                Object internalStateRouter4 = Whitebox.getInternalState(serviceHolder4, "router");
                Assert.assertTrue(internalStateRouter4 instanceof Router);
                Router router4 = (Router) internalStateRouter4;

                Assert.assertEquals(serviceHolder4.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable4 = router4.getRoutingTable();
                Assert.assertTrue(routingTable4 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable4 = (OrdinaryPeerRoutingTable) routingTable4;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node3));

                Assert.assertEquals(ordinaryPeerRoutingTable4.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder.getOverlayNetworkManager();
                Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
                Assert.assertTrue(internalStateRouter instanceof Router);
                Router router = (Router) internalStateRouter;

                Assert.assertEquals(serviceHolder.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable = router.getRoutingTable();
                Assert.assertTrue(routingTable instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;

                Assert.assertTrue(
                        superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                                superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                                superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                                superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 0);

                Assert.assertEquals(
                        superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes().contains(node1));
            }
        } finally {
            fileSharer.leaveNetwork();
            waitFor(delay);
            fileSharer.shutdown();
            waitFor(delay);

            fileSharer4.leaveNetwork();
            waitFor(delay);
            fileSharer4.shutdown();
            waitFor(delay);

            fileSharer3.leaveNetwork();
            waitFor(delay);
            fileSharer3.shutdown();
            waitFor(delay);

            fileSharer2.leaveNetwork();
            waitFor(delay);
            fileSharer2.shutdown();
            waitFor(delay);

            fileSharer1.leaveNetwork();
            waitFor(delay);
            fileSharer1.shutdown();
            waitFor(delay);
        }
    }

    @Test(priority = 8)
    public void testRegisterToSecondPromotedSuperPeer() {
        logger.info("Running Bootstrapping Test 08 - Register to the super peer promoted second");

        FileSharer fileSharer1 = new FileSharer();
        Object internalStateServiceHolder1 = Whitebox.getInternalState(fileSharer1, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder1 instanceof ServiceHolder);
        ServiceHolder serviceHolder1 = (ServiceHolder) internalStateServiceHolder1;
        serviceHolder1.getConfiguration().setIp(node1.getIp());
        serviceHolder1.getConfiguration().setPeerListeningPort(node1.getPort());

        FileSharer fileSharer2 = new FileSharer();
        Object internalStateServiceHolder2 = Whitebox.getInternalState(fileSharer2, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder2 instanceof ServiceHolder);
        ServiceHolder serviceHolder2 = (ServiceHolder) internalStateServiceHolder2;
        serviceHolder2.getConfiguration().setIp(node2.getIp());
        serviceHolder2.getConfiguration().setPeerListeningPort(node2.getPort());

        FileSharer fileSharer3 = new FileSharer();
        Object internalStateServiceHolder3 = Whitebox.getInternalState(fileSharer3, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder3 instanceof ServiceHolder);
        ServiceHolder serviceHolder3 = (ServiceHolder) internalStateServiceHolder3;
        serviceHolder3.getConfiguration().setIp(node3.getIp());
        serviceHolder3.getConfiguration().setPeerListeningPort(node3.getPort());

        FileSharer fileSharer4 = new FileSharer();
        Object internalStateServiceHolder4 = Whitebox.getInternalState(fileSharer4, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder4 instanceof ServiceHolder);
        ServiceHolder serviceHolder4 = (ServiceHolder) internalStateServiceHolder4;
        serviceHolder4.getConfiguration().setIp(node4.getIp());
        serviceHolder4.getConfiguration().setPeerListeningPort(node4.getPort());

        FileSharer fileSharer5 = new FileSharer();
        Object internalStateServiceHolder5 = Whitebox.getInternalState(fileSharer5, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder5 instanceof ServiceHolder);
        ServiceHolder serviceHolder5 = (ServiceHolder) internalStateServiceHolder5;
        serviceHolder5.getConfiguration().setIp(node5.getIp());
        serviceHolder5.getConfiguration().setPeerListeningPort(node5.getPort());

        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(3);
        fileSharer1.start();
        waitFor(delay);

        fileSharer2.start();
        waitFor(delay);

        fileSharer3.start();
        waitFor(delay);

        fileSharer4.start();
        waitFor(delay);

        serviceHolder5.getConfiguration().setSerSuperPeerTimeout(delay);
        fileSharer5.start();
        waitFor(delay * 2);

        fileSharer.start();
        waitFor(delay);

        try {
            {
                serviceHolder1.getOverlayNetworkManager();
                Object internalStateRouter1 = Whitebox.getInternalState(serviceHolder1, "router");
                Assert.assertTrue(internalStateRouter1 instanceof Router);
                Router router1 = (Router) internalStateRouter1;

                Assert.assertEquals(serviceHolder1.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable1 = router1.getRoutingTable();
                Assert.assertTrue(routingTable1 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable1 = (SuperPeerRoutingTable) routingTable1;

                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node4) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkRoutingTableNodes().contains(node5));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 3);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node3));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllSuperPeerNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable1.getAllSuperPeerNetworkRoutingTableNodes().contains(node5));
            }
            {
                serviceHolder2.getOverlayNetworkManager();
                Object internalStateRouter2 = Whitebox.getInternalState(serviceHolder2, "router");
                Assert.assertTrue(internalStateRouter2 instanceof Router);
                Router router2 = (Router) internalStateRouter2;

                Assert.assertEquals(serviceHolder2.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable2 = router2.getRoutingTable();
                Assert.assertTrue(routingTable2 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable2 = (OrdinaryPeerRoutingTable) routingTable2;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node4) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkRoutingTableNodes().contains(node5));

                Assert.assertEquals(ordinaryPeerRoutingTable2.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder3.getOverlayNetworkManager();
                Object internalStateRouter3 = Whitebox.getInternalState(serviceHolder3, "router");
                Assert.assertTrue(internalStateRouter3 instanceof Router);
                Router router3 = (Router) internalStateRouter3;

                Assert.assertEquals(serviceHolder3.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable3 = router3.getRoutingTable();
                Assert.assertTrue(routingTable3 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable3 = (OrdinaryPeerRoutingTable) routingTable3;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node4) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkRoutingTableNodes().contains(node5));

                Assert.assertEquals(ordinaryPeerRoutingTable3.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder4.getOverlayNetworkManager();
                Object internalStateRouter4 = Whitebox.getInternalState(serviceHolder4, "router");
                Assert.assertTrue(internalStateRouter4 instanceof Router);
                Router router4 = (Router) internalStateRouter4;

                Assert.assertEquals(serviceHolder4.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable4 = router4.getRoutingTable();
                Assert.assertTrue(routingTable4 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable4 = (OrdinaryPeerRoutingTable) routingTable4;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkRoutingTableNodes().contains(node5));

                Assert.assertEquals(ordinaryPeerRoutingTable4.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder5.getOverlayNetworkManager();
                Object internalStateRouter5 = Whitebox.getInternalState(serviceHolder5, "router");
                Assert.assertTrue(internalStateRouter5 instanceof Router);
                Router router5 = (Router) internalStateRouter5;

                Assert.assertEquals(serviceHolder5.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable5 = router5.getRoutingTable();
                Assert.assertTrue(routingTable5 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable5 = (SuperPeerRoutingTable) routingTable5;

                Assert.assertTrue(
                        superPeerRoutingTable5.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable5.getAllUnstructuredNetworkRoutingTableNodes().contains(node) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkRoutingTableNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable5.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(
                        superPeerRoutingTable5.getAllAssignedOrdinaryNetworkRoutingTableNodes().contains(node));

                Assert.assertEquals(
                        superPeerRoutingTable5.getAllSuperPeerNetworkRoutingTableNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable5.getAllSuperPeerNetworkRoutingTableNodes().contains(node1));
            }
            {
                serviceHolder.getOverlayNetworkManager();
                Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
                Assert.assertTrue(internalStateRouter instanceof Router);
                Router router = (Router) internalStateRouter;

                Assert.assertEquals(serviceHolder.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable = router.getRoutingTable();
                Assert.assertTrue(routingTable instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node1) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node2) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node3) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node4) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().contains(node5));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node5);
            }
        } finally {
            fileSharer.leaveNetwork();
            waitFor(delay);
            fileSharer.shutdown();
            waitFor(delay);

            fileSharer5.leaveNetwork();
            waitFor(delay);
            fileSharer5.shutdown();
            waitFor(delay);

            fileSharer4.leaveNetwork();
            waitFor(delay);
            fileSharer4.shutdown();
            waitFor(delay);

            fileSharer3.leaveNetwork();
            waitFor(delay);
            fileSharer3.shutdown();
            waitFor(delay);

            fileSharer2.leaveNetwork();
            waitFor(delay);
            fileSharer2.shutdown();
            waitFor(delay);

            fileSharer1.leaveNetwork();
            waitFor(delay);
            fileSharer1.shutdown();
            waitFor(delay);
        }
    }
}
