package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.network.UDPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.core.utils.NodeManager class.
 */
public class QueryManagerTestCase extends BaseTestCase {
    private Router router;
    private QueryManager queryManager;

    @BeforeMethod
    public void initializeMethod() {
        router = Mockito.spy(new Router(new UDPSocketNetworkHandler(), new UnstructuredFloodingRoutingStrategy()));
        queryManager = new QueryManager(router);
    }

    @AfterMethod
    public void cleanUp() {
        router.shutdown();
    }

    @Test
    public void testConstructor() {
        Object internalStateRouter = Whitebox.getInternalState(queryManager, "router");
        Assert.assertNotNull(internalStateRouter);
        Assert.assertTrue(internalStateRouter == router);

        Object internalStateListenersList = Whitebox.getInternalState(router, "listenersList");
        Assert.assertTrue(internalStateListenersList instanceof List<?>);
        List<?> listenersList = (List<?>) internalStateListenersList;
        Assert.assertEquals(listenersList.size(), 1);
        Assert.assertTrue(listenersList.get(0) == queryManager);
    }

    @Test
    public void testQuery() {
        queryManager.query("Cars");

        Configuration configuration = ServiceHolder.getConfiguration();
        Message usedMessage = new Message();
        usedMessage.setType(MessageType.SER);
        usedMessage.setData(MessageIndexes.SER_SOURCE_IP, configuration.getIp());
        usedMessage.setData(MessageIndexes.SER_SOURCE_PORT, Integer.toString(configuration.getPeerListeningPort()));
        usedMessage.setData(MessageIndexes.SER_FILE_NAME, "Cars");
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(NodeConstants.INITIAL_HOP_COUNT + 1));

        Mockito.verify(router, Mockito.times(1)).route(usedMessage);
    }
}
