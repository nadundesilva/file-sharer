package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.bootstrap.BootstrapServer;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageConstants;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Test Case for testing interactions with the bootstrap server.
 */
public class BootstrappingTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BootstrappingTestCase.class);

    private int delay;
    private int shutDownDelay;
    private BootstrapServer bootstrapServer;
    private FileSharer fileSharer;
    private Node node;
    private Node node1;
    private Node node2;
    private Node node3;
    private Node node4;
    private Node node5;
    private Node node6;
    private Node node7;
    private Node node8;
    private Node node9;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Bootstrapping Test");

        String localhostIP = "127.0.0.1";

        delay = 1000;
        shutDownDelay = delay + Constants.TASK_INTERVAL + Constants.THREAD_DISABLE_TIMEOUT;
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
        node2.setPort(9762);

        node3 = new Node();
        node3.setIp(localhostIP);
        node3.setPort(9763);

        node4 = new Node();
        node4.setIp(localhostIP);
        node4.setPort(9764);

        node5 = new Node();
        node5.setIp(localhostIP);
        node5.setPort(9765);

        node6 = new Node();
        node6.setIp(localhostIP);
        node6.setPort(9766);

        node7 = new Node();
        node7.setIp(localhostIP);
        node7.setPort(9767);

        node8 = new Node();
        node8.setIp(localhostIP);
        node8.setPort(9768);

        node9 = new Node();
        node9.setIp(localhostIP);
        node9.setPort(9769);

        bootstrapServer.startInThread();
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
            fileSharer.shutdown();
            waitFor(shutDownDelay);
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

            fileSharer.shutdown();
            waitFor(shutDownDelay);

            Assert.assertEquals(bootstrapServer.getAllNodes().size(), 0);
            Mockito.verify(routerListener, Mockito.times(1))
                    .onMessageReceived(serviceHolder.getConfiguration().getBootstrapServer(), unRegOkMessage);
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);
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

            Assert.assertEquals(superPeerRoutingTable.getAllUnstructuredNetworkNodes().size(), 0);
            Assert.assertEquals(superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().size(), 0);
            Assert.assertEquals(superPeerRoutingTable.getAllSuperPeerNetworkNodes().size(), 0);
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);
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
        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(3);

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

                Assert.assertEquals(superPeerRoutingTable1.getAllUnstructuredNetworkNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().size(), 1);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node));

                Assert.assertEquals(superPeerRoutingTable1.getAllSuperPeerNetworkNodes().size(), 0);
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
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().size(), 1);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node1));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node1);
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
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
        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(3);

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

                Assert.assertEquals(superPeerRoutingTable1.getAllUnstructuredNetworkNodes().size(), 2);
                Assert.assertTrue(superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node));
                Assert.assertTrue(superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node2));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().size(), 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node2));

                Assert.assertEquals(superPeerRoutingTable1.getAllSuperPeerNetworkNodes().size(), 0);
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
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().size(), 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node));
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node1));

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
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().size(), 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node1));
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node2));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node1);
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer2.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
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
        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(3);

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
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node) ||
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node2) ||
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node3));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().size(), 3);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node3));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllSuperPeerNetworkNodes().size(), 0);
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
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node) ||
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node1) ||
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node3));

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
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node) ||
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node1) ||
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node2));

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
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node1) ||
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node2) ||
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node3));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node1);
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer3.shutdown();
            waitFor(shutDownDelay);

            fileSharer2.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
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
        waitFor(delay * 3);

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
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node2) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node3) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().size(), 3);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node3));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllSuperPeerNetworkNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable1.getAllSuperPeerNetworkNodes().contains(node));
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
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node1) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node3) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node4));

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
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node1) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node2) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node4));

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
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node1) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node2) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node3));

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
                        superPeerRoutingTable.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node1) ||
                                superPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node2) ||
                                superPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node3) ||
                                superPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().size(), 0);

                Assert.assertEquals(
                        superPeerRoutingTable.getAllSuperPeerNetworkNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable.getAllSuperPeerNetworkNodes().contains(node1));
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer4.shutdown();
            waitFor(shutDownDelay);

            fileSharer3.shutdown();
            waitFor(shutDownDelay);

            fileSharer2.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
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
        waitFor(delay * 3);

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
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node2) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node3) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node4) ||
                                superPeerRoutingTable1.getAllUnstructuredNetworkNodes().contains(node5));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().size(), 3);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node3));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllSuperPeerNetworkNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable1.getAllSuperPeerNetworkNodes().contains(node5));
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
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node1) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node3) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node4) ||
                            ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().contains(node5));

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
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node1) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node2) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node4) ||
                            ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().contains(node5));

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
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node1) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node2) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node3) ||
                            ordinaryPeerRoutingTable4.getAllUnstructuredNetworkNodes().contains(node5));

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
                        superPeerRoutingTable5.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        superPeerRoutingTable5.getAllUnstructuredNetworkNodes().contains(node) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkNodes().contains(node1) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkNodes().contains(node2) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkNodes().contains(node3) ||
                                superPeerRoutingTable5.getAllUnstructuredNetworkNodes().contains(node4));

                Assert.assertEquals(
                        superPeerRoutingTable5.getAllAssignedOrdinaryNetworkNodes().size(), 1);
                Assert.assertTrue(
                        superPeerRoutingTable5.getAllAssignedOrdinaryNetworkNodes().contains(node));

                Assert.assertEquals(
                        superPeerRoutingTable5.getAllSuperPeerNetworkNodes().size(), 1);
                Assert.assertTrue(superPeerRoutingTable5.getAllSuperPeerNetworkNodes().contains(node1));
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
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().size() >= 2);
                Assert.assertTrue(
                        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node1) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node2) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node3) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node4) ||
                            ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes().contains(node5));

                Assert.assertEquals(ordinaryPeerRoutingTable.getAssignedSuperPeer(), node5);
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer5.shutdown();
            waitFor(shutDownDelay);

            fileSharer4.shutdown();
            waitFor(shutDownDelay);

            fileSharer3.shutdown();
            waitFor(shutDownDelay);

            fileSharer2.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
        }
    }

    @Test(priority = 9)
    public void testThirdSuperPeerPromotion() {
        logger.info("Running Bootstrapping Test 09 - Register a node when the assigned ordinary peer count is maxed " +
                "for the second time");

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

        FileSharer fileSharer6 = new FileSharer();
        Object internalStateServiceHolder6 = Whitebox.getInternalState(fileSharer6, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder6 instanceof ServiceHolder);
        ServiceHolder serviceHolder6 = (ServiceHolder) internalStateServiceHolder6;
        serviceHolder6.getConfiguration().setIp(node6.getIp());
        serviceHolder6.getConfiguration().setPeerListeningPort(node6.getPort());

        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        fileSharer1.start();
        waitFor(delay);

        fileSharer2.start();
        waitFor(delay);

        fileSharer3.start();
        waitFor(delay);

        serviceHolder4.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        serviceHolder4.getConfiguration().setSerSuperPeerTimeout(delay);
        fileSharer4.start();
        waitFor(delay * 3);

        fileSharer5.start();
        waitFor(delay);

        fileSharer6.start();
        waitFor(delay);

        serviceHolder.getConfiguration().setSerSuperPeerTimeout(delay);
        fileSharer.start();
        waitFor(delay * 3);

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
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().size(), 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node3));

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllSuperPeerNetworkNodes().size(), 2);
                Assert.assertTrue(superPeerRoutingTable1.getAllSuperPeerNetworkNodes().contains(node4));
                Assert.assertTrue(superPeerRoutingTable1.getAllSuperPeerNetworkNodes().contains(node));
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
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().size() >= 2);

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
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable3.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder4.getOverlayNetworkManager();
                Object internalStateRouter4 = Whitebox.getInternalState(serviceHolder4, "router");
                Assert.assertTrue(internalStateRouter4 instanceof Router);
                Router router4 = (Router) internalStateRouter4;

                Assert.assertEquals(serviceHolder4.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable4 = router4.getRoutingTable();
                Assert.assertTrue(routingTable4 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable4 = (SuperPeerRoutingTable) routingTable4;

                Assert.assertTrue(
                        superPeerRoutingTable4.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(
                        superPeerRoutingTable4.getAllAssignedOrdinaryNetworkNodes().size(), 2);
                Assert.assertTrue(
                        superPeerRoutingTable4.getAllAssignedOrdinaryNetworkNodes().contains(node5));
                Assert.assertTrue(
                        superPeerRoutingTable4.getAllAssignedOrdinaryNetworkNodes().contains(node6));

                Assert.assertEquals(
                        superPeerRoutingTable4.getAllSuperPeerNetworkNodes().size(), 2);
                Assert.assertTrue(superPeerRoutingTable4.getAllSuperPeerNetworkNodes().contains(node1));
                Assert.assertTrue(superPeerRoutingTable4.getAllSuperPeerNetworkNodes().contains(node));
            }
            {
                serviceHolder5.getOverlayNetworkManager();
                Object internalStateRouter5 = Whitebox.getInternalState(serviceHolder5, "router");
                Assert.assertTrue(internalStateRouter5 instanceof Router);
                Router router5 = (Router) internalStateRouter5;

                Assert.assertEquals(serviceHolder5.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable5 = router5.getRoutingTable();
                Assert.assertTrue(routingTable5 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable5 = (OrdinaryPeerRoutingTable) routingTable5;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable5.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable5.getAssignedSuperPeer(), node4);
            }
            {
                serviceHolder6.getOverlayNetworkManager();
                Object internalStateRouter6 = Whitebox.getInternalState(serviceHolder6, "router");
                Assert.assertTrue(internalStateRouter6 instanceof Router);
                Router router6 = (Router) internalStateRouter6;

                Assert.assertEquals(serviceHolder6.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable6 = router6.getRoutingTable();
                Assert.assertTrue(routingTable6 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable6 = (OrdinaryPeerRoutingTable) routingTable6;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable6.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable6.getAssignedSuperPeer(), node4);
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
                        superPeerRoutingTable.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(
                        superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().size(), 0);

                Assert.assertEquals(
                        superPeerRoutingTable.getAllSuperPeerNetworkNodes().size(), 2);
                Assert.assertTrue(superPeerRoutingTable.getAllSuperPeerNetworkNodes().contains(node1));
                Assert.assertTrue(superPeerRoutingTable.getAllSuperPeerNetworkNodes().contains(node4));
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer6.shutdown();
            waitFor(shutDownDelay);

            fileSharer5.shutdown();
            waitFor(shutDownDelay);

            fileSharer4.shutdown();
            waitFor(shutDownDelay);

            fileSharer3.shutdown();
            waitFor(shutDownDelay);

            fileSharer2.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
        }
    }

    @Test(priority = 10)
    public void testForthSuperPeerPromotion() {
        logger.info("Running Bootstrapping Test 10 - Register a node when the assigned ordinary peer count is maxed " +
                "for the third time");

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

        FileSharer fileSharer6 = new FileSharer();
        Object internalStateServiceHolder6 = Whitebox.getInternalState(fileSharer6, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder6 instanceof ServiceHolder);
        ServiceHolder serviceHolder6 = (ServiceHolder) internalStateServiceHolder6;
        serviceHolder6.getConfiguration().setIp(node6.getIp());
        serviceHolder6.getConfiguration().setPeerListeningPort(node6.getPort());

        FileSharer fileSharer7 = new FileSharer();
        Object internalStateServiceHolder7 = Whitebox.getInternalState(fileSharer7, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder7 instanceof ServiceHolder);
        ServiceHolder serviceHolder7 = (ServiceHolder) internalStateServiceHolder7;
        serviceHolder7.getConfiguration().setIp(node7.getIp());
        serviceHolder7.getConfiguration().setPeerListeningPort(node7.getPort());

        FileSharer fileSharer8 = new FileSharer();
        Object internalStateServiceHolder8 = Whitebox.getInternalState(fileSharer8, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder8 instanceof ServiceHolder);
        ServiceHolder serviceHolder8 = (ServiceHolder) internalStateServiceHolder8;
        serviceHolder8.getConfiguration().setIp(node8.getIp());
        serviceHolder8.getConfiguration().setPeerListeningPort(node8.getPort());

        FileSharer fileSharer9 = new FileSharer();
        Object internalStateServiceHolder9 = Whitebox.getInternalState(fileSharer9, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder9 instanceof ServiceHolder);
        ServiceHolder serviceHolder9 = (ServiceHolder) internalStateServiceHolder9;
        serviceHolder9.getConfiguration().setIp(node9.getIp());
        serviceHolder9.getConfiguration().setPeerListeningPort(node9.getPort());

        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        fileSharer1.start();
        waitFor(delay);

        fileSharer2.start();
        waitFor(delay);

        fileSharer3.start();
        waitFor(delay);

        serviceHolder4.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        serviceHolder4.getConfiguration().setSerSuperPeerTimeout(delay);
        fileSharer4.start();
        waitFor(delay * 3);

        fileSharer5.start();
        waitFor(delay);

        fileSharer6.start();
        waitFor(delay);

        serviceHolder7.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        serviceHolder7.getConfiguration().setSerSuperPeerTimeout(delay);
        fileSharer7.start();
        waitFor(delay * 3);

        fileSharer8.start();
        waitFor(delay);

        fileSharer9.start();
        waitFor(delay);

        serviceHolder.getConfiguration().setSerSuperPeerTimeout(delay);
        fileSharer.start();
        waitFor(delay * 3);

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
                        superPeerRoutingTable1.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().size(), 2);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node2));
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllAssignedOrdinaryNetworkNodes().contains(node3));

                Assert.assertTrue(superPeerRoutingTable1.getAllSuperPeerNetworkNodes().size() > 0);
                Assert.assertTrue(
                        superPeerRoutingTable1.getAllSuperPeerNetworkNodes().contains(node4) ||
                        superPeerRoutingTable1.getAllSuperPeerNetworkNodes().contains(node7) ||
                        superPeerRoutingTable1.getAllSuperPeerNetworkNodes().contains(node));
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
                        ordinaryPeerRoutingTable2.getAllUnstructuredNetworkNodes().size() >= 2);

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
                        ordinaryPeerRoutingTable3.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable3.getAssignedSuperPeer(), node1);
            }
            {
                serviceHolder4.getOverlayNetworkManager();
                Object internalStateRouter4 = Whitebox.getInternalState(serviceHolder4, "router");
                Assert.assertTrue(internalStateRouter4 instanceof Router);
                Router router4 = (Router) internalStateRouter4;

                Assert.assertEquals(serviceHolder4.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable4 = router4.getRoutingTable();
                Assert.assertTrue(routingTable4 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable4 = (SuperPeerRoutingTable) routingTable4;

                Assert.assertTrue(
                        superPeerRoutingTable4.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(
                        superPeerRoutingTable4.getAllAssignedOrdinaryNetworkNodes().size(), 2);
                Assert.assertTrue(
                        superPeerRoutingTable4.getAllAssignedOrdinaryNetworkNodes().contains(node5));
                Assert.assertTrue(
                        superPeerRoutingTable4.getAllAssignedOrdinaryNetworkNodes().contains(node6));

                Assert.assertTrue(superPeerRoutingTable4.getAllSuperPeerNetworkNodes().size() > 0);
                Assert.assertTrue(superPeerRoutingTable4.getAllSuperPeerNetworkNodes().contains(node1) ||
                        superPeerRoutingTable4.getAllSuperPeerNetworkNodes().contains(node7) ||
                        superPeerRoutingTable4.getAllSuperPeerNetworkNodes().contains(node));
            }
            {
                serviceHolder5.getOverlayNetworkManager();
                Object internalStateRouter5 = Whitebox.getInternalState(serviceHolder5, "router");
                Assert.assertTrue(internalStateRouter5 instanceof Router);
                Router router5 = (Router) internalStateRouter5;

                Assert.assertEquals(serviceHolder5.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable5 = router5.getRoutingTable();
                Assert.assertTrue(routingTable5 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable5 = (OrdinaryPeerRoutingTable) routingTable5;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable5.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable5.getAssignedSuperPeer(), node4);
            }
            {
                serviceHolder6.getOverlayNetworkManager();
                Object internalStateRouter6 = Whitebox.getInternalState(serviceHolder6, "router");
                Assert.assertTrue(internalStateRouter6 instanceof Router);
                Router router6 = (Router) internalStateRouter6;

                Assert.assertEquals(serviceHolder6.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable6 = router6.getRoutingTable();
                Assert.assertTrue(routingTable6 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable6 = (OrdinaryPeerRoutingTable) routingTable6;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable6.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable6.getAssignedSuperPeer(), node4);
            }
            {
                serviceHolder7.getOverlayNetworkManager();
                Object internalStateRouter7 = Whitebox.getInternalState(serviceHolder7, "router");
                Assert.assertTrue(internalStateRouter7 instanceof Router);
                Router router7 = (Router) internalStateRouter7;

                Assert.assertEquals(serviceHolder7.getPeerType(), PeerType.SUPER_PEER);

                RoutingTable routingTable7 = router7.getRoutingTable();
                Assert.assertTrue(routingTable7 instanceof SuperPeerRoutingTable);
                SuperPeerRoutingTable superPeerRoutingTable7 = (SuperPeerRoutingTable) routingTable7;

                Assert.assertTrue(
                        superPeerRoutingTable7.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(
                        superPeerRoutingTable7.getAllAssignedOrdinaryNetworkNodes().size(), 2);
                Assert.assertTrue(
                        superPeerRoutingTable7.getAllAssignedOrdinaryNetworkNodes().contains(node8));
                Assert.assertTrue(
                        superPeerRoutingTable7.getAllAssignedOrdinaryNetworkNodes().contains(node9));

                Assert.assertTrue(superPeerRoutingTable7.getAllSuperPeerNetworkNodes().size() > 0);
                Assert.assertTrue(superPeerRoutingTable7.getAllSuperPeerNetworkNodes().contains(node1) ||
                        superPeerRoutingTable7.getAllSuperPeerNetworkNodes().contains(node4) ||
                        superPeerRoutingTable7.getAllSuperPeerNetworkNodes().contains(node));
            }
            {
                serviceHolder8.getOverlayNetworkManager();
                Object internalStateRouter8 = Whitebox.getInternalState(serviceHolder8, "router");
                Assert.assertTrue(internalStateRouter8 instanceof Router);
                Router router8 = (Router) internalStateRouter8;

                Assert.assertEquals(serviceHolder8.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable8 = router8.getRoutingTable();
                Assert.assertTrue(routingTable8 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable8 = (OrdinaryPeerRoutingTable) routingTable8;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable8.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable8.getAssignedSuperPeer(), node7);
            }
            {
                serviceHolder9.getOverlayNetworkManager();
                Object internalStateRouter9 = Whitebox.getInternalState(serviceHolder9, "router");
                Assert.assertTrue(internalStateRouter9 instanceof Router);
                Router router9 = (Router) internalStateRouter9;

                Assert.assertEquals(serviceHolder9.getPeerType(), PeerType.ORDINARY_PEER);

                RoutingTable routingTable9 = router9.getRoutingTable();
                Assert.assertTrue(routingTable9 instanceof OrdinaryPeerRoutingTable);
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable9 = (OrdinaryPeerRoutingTable) routingTable9;

                Assert.assertTrue(
                        ordinaryPeerRoutingTable9.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(ordinaryPeerRoutingTable9.getAssignedSuperPeer(), node7);
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
                        superPeerRoutingTable.getAllUnstructuredNetworkNodes().size() >= 2);

                Assert.assertEquals(
                        superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().size(), 0);

                Assert.assertTrue(superPeerRoutingTable.getAllSuperPeerNetworkNodes().size() > 0);
                Assert.assertTrue(superPeerRoutingTable.getAllSuperPeerNetworkNodes().contains(node1) ||
                        superPeerRoutingTable.getAllSuperPeerNetworkNodes().contains(node4) ||
                        superPeerRoutingTable.getAllSuperPeerNetworkNodes().contains(node7));
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer9.shutdown();
            waitFor(shutDownDelay);

            fileSharer8.shutdown();
            waitFor(shutDownDelay);

            fileSharer7.shutdown();
            waitFor(shutDownDelay);

            fileSharer6.shutdown();
            waitFor(shutDownDelay);

            fileSharer5.shutdown();
            waitFor(shutDownDelay);

            fileSharer4.shutdown();
            waitFor(shutDownDelay);

            fileSharer3.shutdown();
            waitFor(shutDownDelay);

            fileSharer2.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
        }
    }

    @Test(priority = 11)
    public void testResourcePopulation() {
        logger.info("Running Bootstrapping Test 11 - Resource index population");

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

        FileSharer fileSharer6 = new FileSharer();
        Object internalStateServiceHolder6 = Whitebox.getInternalState(fileSharer6, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder6 instanceof ServiceHolder);
        ServiceHolder serviceHolder6 = (ServiceHolder) internalStateServiceHolder6;
        serviceHolder6.getConfiguration().setIp(node6.getIp());
        serviceHolder6.getConfiguration().setPeerListeningPort(node6.getPort());

        FileSharer fileSharer7 = new FileSharer();
        Object internalStateServiceHolder7 = Whitebox.getInternalState(fileSharer7, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder7 instanceof ServiceHolder);
        ServiceHolder serviceHolder7 = (ServiceHolder) internalStateServiceHolder7;
        serviceHolder7.getConfiguration().setIp(node7.getIp());
        serviceHolder7.getConfiguration().setPeerListeningPort(node7.getPort());

        FileSharer fileSharer8 = new FileSharer();
        Object internalStateServiceHolder8 = Whitebox.getInternalState(fileSharer8, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder8 instanceof ServiceHolder);
        ServiceHolder serviceHolder8 = (ServiceHolder) internalStateServiceHolder8;
        serviceHolder8.getConfiguration().setIp(node8.getIp());
        serviceHolder8.getConfiguration().setPeerListeningPort(node8.getPort());

        FileSharer fileSharer9 = new FileSharer();
        Object internalStateServiceHolder9 = Whitebox.getInternalState(fileSharer9, "serviceHolder");
        Assert.assertTrue(internalStateServiceHolder9 instanceof ServiceHolder);
        ServiceHolder serviceHolder9 = (ServiceHolder) internalStateServiceHolder9;
        serviceHolder9.getConfiguration().setIp(node9.getIp());
        serviceHolder9.getConfiguration().setPeerListeningPort(node9.getPort());

        // Registering resources
        ResourceIndex resourceIndices1 = fileSharer1.getServiceHolder().getResourceIndex();
        resourceIndices1.addOwnedResource("Lord of the Rings 2", null);
        resourceIndices1.addOwnedResource("Cars", null);
        resourceIndices1.addOwnedResource("Iron Man", null);

        ResourceIndex resourceIndices2 = fileSharer2.getServiceHolder().getResourceIndex();
        resourceIndices2.addOwnedResource("Lord of the Rings", null);
        resourceIndices2.addOwnedResource("Iron Man 2", null);
        resourceIndices2.addOwnedResource("Spider Man", null);

        ResourceIndex resourceIndices3 = fileSharer3.getServiceHolder().getResourceIndex();
        resourceIndices3.addOwnedResource("Hotel Transylvania", null);
        resourceIndices3.addOwnedResource("How to train your Dragon", null);
        resourceIndices3.addOwnedResource("Lord of the Rings", null);

        ResourceIndex resourceIndices4 = fileSharer4.getServiceHolder().getResourceIndex();
        resourceIndices4.addOwnedResource("Leap Year", null);
        resourceIndices4.addOwnedResource("Leap Year", null);
        resourceIndices4.addOwnedResource("Two weeks Notice", null);

        ResourceIndex resourceIndices5 = fileSharer5.getServiceHolder().getResourceIndex();
        resourceIndices5.addOwnedResource("Me Before You", null);
        resourceIndices5.addOwnedResource("Endless Love", null);
        resourceIndices5.addOwnedResource("Life as we know it", null);

        ResourceIndex resourceIndices6 = fileSharer6.getServiceHolder().getResourceIndex();
        resourceIndices6.addOwnedResource("How do you know", null);
        resourceIndices6.addOwnedResource("The Last Song", null);
        resourceIndices6.addOwnedResource("Thor", null);

        ResourceIndex resourceIndices7 = fileSharer7.getServiceHolder().getResourceIndex();
        resourceIndices7.addOwnedResource("X-Men Origins", null);
        resourceIndices7.addOwnedResource("Cars", null);
        resourceIndices7.addOwnedResource("Captain America", null);

        ResourceIndex resourceIndices8 = fileSharer8.getServiceHolder().getResourceIndex();
        resourceIndices8.addOwnedResource("22 Jump Street", null);
        resourceIndices8.addOwnedResource("Iron Man 3", null);
        resourceIndices8.addOwnedResource("Lord of the Rings", null);

        ResourceIndex resourceIndices9 = fileSharer9.getServiceHolder().getResourceIndex();
        resourceIndices9.addOwnedResource("James Bond Sky fall", null);
        resourceIndices9.addOwnedResource("Suicide Squad", null);
        resourceIndices9.addOwnedResource("Fast and Furious", null);

        ResourceIndex resourceIndices = fileSharer.getServiceHolder().getResourceIndex();
        resourceIndices.addOwnedResource("Teenage Mutant Ninja Turtles", null);
        resourceIndices.addOwnedResource("Underworld", null);
        resourceIndices.addOwnedResource("Despicable Me 3", null);

        serviceHolder1.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        serviceHolder1.getConfiguration().setGossipingInterval(delay * 100);
        serviceHolder1.getConfiguration().setHeartbeatInterval(delay);
        fileSharer1.start();
        waitFor(delay * 2);

        serviceHolder2.getConfiguration().setGossipingInterval(delay * 100);
        fileSharer2.start();
        waitFor(delay);

        serviceHolder3.getConfiguration().setGossipingInterval(delay * 100);
        fileSharer3.start();
        waitFor(delay);

        serviceHolder4.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        serviceHolder4.getConfiguration().setSerSuperPeerTimeout(delay);
        serviceHolder4.getConfiguration().setGossipingInterval(delay * 100);
        serviceHolder4.getConfiguration().setHeartbeatInterval(delay);
        fileSharer4.start();
        waitFor(delay * 3);

        serviceHolder5.getConfiguration().setGossipingInterval(delay * 100);
        fileSharer5.start();
        waitFor(delay);

        serviceHolder6.getConfiguration().setGossipingInterval(delay * 100);
        fileSharer6.start();
        waitFor(delay);

        serviceHolder7.getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
        serviceHolder7.getConfiguration().setSerSuperPeerTimeout(delay);
        serviceHolder7.getConfiguration().setGossipingInterval(delay * 100);
        serviceHolder7.getConfiguration().setHeartbeatInterval(delay);
        fileSharer7.start();
        waitFor(delay * 3);

        serviceHolder8.getConfiguration().setGossipingInterval(delay * 100);
        fileSharer8.start();
        waitFor(delay);

        serviceHolder9.getConfiguration().setGossipingInterval(delay * 100);
        fileSharer9.start();
        waitFor(delay);

        serviceHolder.getConfiguration().setSerSuperPeerTimeout(delay);
        serviceHolder.getConfiguration().setGossipingInterval(delay * 100);
        serviceHolder.getConfiguration().setHeartbeatInterval(delay);
        fileSharer.start();
        waitFor(delay * 3);

        try {
            {
                AggregatedResource aggregatedResource1 = new AggregatedResource("Lord of the Rings");
                AggregatedResource aggregatedResource2 = new AggregatedResource("Iron Man 2");
                AggregatedResource aggregatedResource3 = new AggregatedResource("Spider Man");
                AggregatedResource aggregatedResource4 = new AggregatedResource("Hotel Transylvania");
                AggregatedResource aggregatedResource5 = new AggregatedResource("How to train your Dragon");

                ResourceIndex resourceIndex1 = fileSharer1.getServiceHolder().getResourceIndex();
                Assert.assertTrue(resourceIndex1 instanceof SuperPeerResourceIndex);
                SuperPeerResourceIndex superPeerResourceIndex1 = (SuperPeerResourceIndex) resourceIndex1;
                Assert.assertEquals(superPeerResourceIndex1.getAllAggregatedResources().size(), 5);

                Assert.assertTrue(superPeerResourceIndex1.getAllAggregatedResources().contains(aggregatedResource1));
                AggregatedResource storedAggregatedResource1 =
                        new ArrayList<>(superPeerResourceIndex1.findAggregatedResources(aggregatedResource1.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource1.getAllNodes().size(), 2);
                Assert.assertTrue(storedAggregatedResource1.getAllNodes().contains(node2));
                Assert.assertTrue(storedAggregatedResource1.getAllNodes().contains(node3));

                Assert.assertTrue(superPeerResourceIndex1.getAllAggregatedResources().contains(aggregatedResource2));
                AggregatedResource storedAggregatedResource2 =
                        new ArrayList<>(superPeerResourceIndex1.findAggregatedResources(aggregatedResource2.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource2.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource2.getAllNodes().contains(node2));

                Assert.assertTrue(superPeerResourceIndex1.getAllAggregatedResources().contains(aggregatedResource3));
                AggregatedResource storedAggregatedResource3 =
                        new ArrayList<>(superPeerResourceIndex1.findAggregatedResources(aggregatedResource3.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource3.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource3.getAllNodes().contains(node2));

                Assert.assertTrue(superPeerResourceIndex1.getAllAggregatedResources().contains(aggregatedResource4));
                AggregatedResource storedAggregatedResource4 =
                        new ArrayList<>(superPeerResourceIndex1.findAggregatedResources(aggregatedResource4.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource4.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource4.getAllNodes().contains(node3));

                Assert.assertTrue(superPeerResourceIndex1.getAllAggregatedResources().contains(aggregatedResource5));
                AggregatedResource storedAggregatedResource5 =
                        new ArrayList<>(superPeerResourceIndex1.findAggregatedResources(aggregatedResource5.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource5.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource5.getAllNodes().contains(node3));
            }
            {
                AggregatedResource aggregatedResource1 = new AggregatedResource("Me Before You");
                AggregatedResource aggregatedResource2 = new AggregatedResource("Endless Love");
                AggregatedResource aggregatedResource3 = new AggregatedResource("Life as we know it");
                AggregatedResource aggregatedResource4 = new AggregatedResource("How do you know");
                AggregatedResource aggregatedResource5 = new AggregatedResource("The Last Song");
                AggregatedResource aggregatedResource6 = new AggregatedResource("Thor");

                ResourceIndex resourceIndex4 = fileSharer4.getServiceHolder().getResourceIndex();
                Assert.assertTrue(resourceIndex4 instanceof SuperPeerResourceIndex);
                SuperPeerResourceIndex superPeerResourceIndex4 = (SuperPeerResourceIndex) resourceIndex4;
                Assert.assertEquals(superPeerResourceIndex4.getAllAggregatedResources().size(), 6);

                Assert.assertTrue(superPeerResourceIndex4.getAllAggregatedResources().contains(aggregatedResource1));
                AggregatedResource storedAggregatedResource1 =
                        new ArrayList<>(superPeerResourceIndex4.findAggregatedResources(aggregatedResource1.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource1.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource1.getAllNodes().contains(node5));

                Assert.assertTrue(superPeerResourceIndex4.getAllAggregatedResources().contains(aggregatedResource2));
                AggregatedResource storedAggregatedResource2 =
                        new ArrayList<>(superPeerResourceIndex4.findAggregatedResources(aggregatedResource2.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource2.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource2.getAllNodes().contains(node5));

                Assert.assertTrue(superPeerResourceIndex4.getAllAggregatedResources().contains(aggregatedResource3));
                AggregatedResource storedAggregatedResource3 =
                        new ArrayList<>(superPeerResourceIndex4.findAggregatedResources(aggregatedResource3.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource3.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource3.getAllNodes().contains(node5));

                Assert.assertTrue(superPeerResourceIndex4.getAllAggregatedResources().contains(aggregatedResource4));
                AggregatedResource storedAggregatedResource4 =
                        new ArrayList<>(superPeerResourceIndex4.findAggregatedResources(aggregatedResource4.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource4.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource4.getAllNodes().contains(node6));

                Assert.assertTrue(superPeerResourceIndex4.getAllAggregatedResources().contains(aggregatedResource5));
                AggregatedResource storedAggregatedResource5 =
                        new ArrayList<>(superPeerResourceIndex4.findAggregatedResources(aggregatedResource5.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource5.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource5.getAllNodes().contains(node6));

                Assert.assertTrue(superPeerResourceIndex4.getAllAggregatedResources().contains(aggregatedResource6));
                AggregatedResource storedAggregatedResource6 =
                        new ArrayList<>(superPeerResourceIndex4.findAggregatedResources(aggregatedResource6.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource6.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource6.getAllNodes().contains(node6));
            }
            {
                AggregatedResource aggregatedResource1 = new AggregatedResource("22 Jump Street");
                AggregatedResource aggregatedResource2 = new AggregatedResource("Iron Man 3");
                AggregatedResource aggregatedResource3 = new AggregatedResource("Lord of the Rings");
                AggregatedResource aggregatedResource4 = new AggregatedResource("James Bond Sky fall");
                AggregatedResource aggregatedResource5 = new AggregatedResource("Suicide Squad");
                AggregatedResource aggregatedResource6 = new AggregatedResource("Fast and Furious");

                ResourceIndex resourceIndex7 = fileSharer7.getServiceHolder().getResourceIndex();
                Assert.assertTrue(resourceIndex7 instanceof SuperPeerResourceIndex);
                SuperPeerResourceIndex superPeerResourceIndex7 = (SuperPeerResourceIndex) resourceIndex7;
                Assert.assertEquals(superPeerResourceIndex7.getAllAggregatedResources().size(), 6);

                Assert.assertTrue(superPeerResourceIndex7.getAllAggregatedResources().contains(aggregatedResource1));
                AggregatedResource storedAggregatedResource1 =
                        new ArrayList<>(superPeerResourceIndex7.findAggregatedResources(aggregatedResource1.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource1.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource1.getAllNodes().contains(node8));

                Assert.assertTrue(superPeerResourceIndex7.getAllAggregatedResources().contains(aggregatedResource2));
                AggregatedResource storedAggregatedResource2 =
                        new ArrayList<>(superPeerResourceIndex7.findAggregatedResources(aggregatedResource2.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource2.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource2.getAllNodes().contains(node8));

                Assert.assertTrue(superPeerResourceIndex7.getAllAggregatedResources().contains(aggregatedResource3));
                AggregatedResource storedAggregatedResource3 =
                        new ArrayList<>(superPeerResourceIndex7.findAggregatedResources(aggregatedResource3.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource3.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource3.getAllNodes().contains(node8));

                Assert.assertTrue(superPeerResourceIndex7.getAllAggregatedResources().contains(aggregatedResource4));
                AggregatedResource storedAggregatedResource4 =
                        new ArrayList<>(superPeerResourceIndex7.findAggregatedResources(aggregatedResource4.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource4.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource4.getAllNodes().contains(node9));

                Assert.assertTrue(superPeerResourceIndex7.getAllAggregatedResources().contains(aggregatedResource5));
                AggregatedResource storedAggregatedResource5 =
                        new ArrayList<>(superPeerResourceIndex7.findAggregatedResources(aggregatedResource5.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource5.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource5.getAllNodes().contains(node9));

                Assert.assertTrue(superPeerResourceIndex7.getAllAggregatedResources().contains(aggregatedResource6));
                AggregatedResource storedAggregatedResource6 =
                        new ArrayList<>(superPeerResourceIndex7.findAggregatedResources(aggregatedResource6.getName()))
                                .get(0);
                Assert.assertEquals(storedAggregatedResource6.getAllNodes().size(), 1);
                Assert.assertTrue(storedAggregatedResource6.getAllNodes().contains(node9));
            }
            {
                ResourceIndex resourceIndex = fileSharer.getServiceHolder().getResourceIndex();
                Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
                SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;
                Assert.assertEquals(superPeerResourceIndex.getAllAggregatedResources().size(), 0);
            }
        } finally {
            fileSharer.shutdown();
            waitFor(shutDownDelay);

            fileSharer9.shutdown();
            waitFor(shutDownDelay);

            fileSharer8.shutdown();
            waitFor(shutDownDelay);

            fileSharer7.shutdown();
            waitFor(shutDownDelay);

            fileSharer6.shutdown();
            waitFor(shutDownDelay);

            fileSharer5.shutdown();
            waitFor(shutDownDelay);

            fileSharer4.shutdown();
            waitFor(shutDownDelay);

            fileSharer3.shutdown();
            waitFor(shutDownDelay);

            fileSharer2.shutdown();
            waitFor(shutDownDelay);

            fileSharer1.shutdown();
            waitFor(shutDownDelay);
        }
    }
}
