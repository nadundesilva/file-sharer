package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.bootstrap.BootstrapServer;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
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

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Bootstrapping Test");

        delay = 1000;
        bootstrapServer = new BootstrapServer();
        fileSharer = new FileSharer();
        Whitebox.setInternalState(fileSharer, "serviceHolder", serviceHolder);

        bootstrapServer.start();
        waitFor(delay);
        fileSharer.start();
        waitFor(delay);
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up Bootstrapping Test");

        fileSharer.shutdown();
        waitFor(delay);
        bootstrapServer.shutdown();
        waitFor(delay);
    }

    @Test
    public void testRegister() {
        logger.info("Running Bootstrapping Test 01 - Register in the bootstrap server");

        Assert.assertEquals(bootstrapServer.getAllNodes().size(), 1);
        Assert.assertEquals(bootstrapServer.getAllNodes().get(0).getIp(),
                serviceHolder.getConfiguration().getIp());
        Assert.assertEquals(bootstrapServer.getAllNodes().get(0).getPort(),
                serviceHolder.getConfiguration().getPeerListeningPort());
    }

    @Test
    public void testUnregister() {
        logger.info("Running Bootstrapping Test 02 - Unregister from the bootstrap server");

        fileSharer.shutdown();
        waitFor(delay);

        Assert.assertEquals(bootstrapServer.getAllNodes().size(), 0);
    }

    @Test
    public void testFirstNodeRegister() {
        logger.info("Running Bootstrapping Test 03 - Register first node");

        fileSharer.shutdown();
        waitFor(delay);

        serviceHolder.getOverlayNetworkManager();
        Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
        Assert.assertTrue(internalStateRouter instanceof Router);
        Router router = (Router) internalStateRouter;

        RouterListener routerListener = Mockito.mock(RouterListener.class);
        router.registerListener(routerListener);

        fileSharer.start();
        waitFor(delay);

        Message message = new Message();
        message.setType(MessageType.REG_OK);
        message.setData(MessageIndexes.REG_OK_NODES_COUNT, "0");

        Mockito.verify(routerListener, Mockito.times(1))
                .onMessageReceived(serviceHolder.getConfiguration().getBootstrapServer(), message);
        Assert.assertEquals(bootstrapServer.getAllNodes().size(), 1);
        Assert.assertEquals(bootstrapServer.getAllNodes().get(0).getIp(),
                serviceHolder.getConfiguration().getIp());
        Assert.assertEquals(bootstrapServer.getAllNodes().get(0).getPort(),
                serviceHolder.getConfiguration().getPeerListeningPort());
        Assert.assertEquals(serviceHolder.getPeerType(), PeerType.SUPER_PEER);

        RoutingTable routingTable = router.getRoutingTable();
        Assert.assertTrue(routingTable instanceof SuperPeerRoutingTable);
        SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;
        Assert.assertEquals(superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 0);
        Assert.assertEquals(superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes().size(), 0);
        Assert.assertEquals(superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes().size(), 0);
    }
}
