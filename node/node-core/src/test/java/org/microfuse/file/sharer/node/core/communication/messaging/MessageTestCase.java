package org.microfuse.file.sharer.node.core.communication.messaging;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.commons.messaging.Message class.
 */
public class MessageTestCase {
    private static final Logger logger = LoggerFactory.getLogger(MessageTestCase.class);

    private Message message;
    private Message messageCopy;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Message Test");

        List<String> messageData = Lists.newArrayList("129.82.62.142", "5070", "Lord of the Rings");

        message = new Message();
        message.setType(MessageType.SER);
        message.setData(messageData);

        messageCopy = new Message();
        messageCopy.setType(MessageType.SER);
        messageCopy.setData(messageData);
    }

    @Test(priority = 1)
    public void testToString() {
        logger.info("Running Message Test 01 - To string");

        Assert.assertEquals(message.toString(), "0047 SER 129.82.62.142 5070 \"Lord of the Rings\"");
    }

    @Test(priority = 2)
    public void testEquals() {
        logger.info("Running Node Test 02 - Equals");

        Assert.assertTrue(message.equals(messageCopy));
    }

    @Test(priority = 2)
    public void testHashCode() {
        logger.info("Running Node Test 03 - Hash code");

        Assert.assertEquals(message.hashCode(), message.toString().hashCode());
    }

    @Test(priority = 3)
    public void testSetData() {
        logger.info("Running Message Test 04 - Set data");

        message.setData(1, "5080");
        Assert.assertEquals(message.getData().get(1), "5080");
    }

    @Test(priority = 3)
    public void testGetData() {
        logger.info("Running Message Test 05 - Get data");

        Assert.assertEquals(message.getData(1), "5070");
    }

    @Test(priority = 4)
    public void testMessageParsingWithQuotesAtEnd() {
        logger.info("Running Message Test 06 - Message parsing with quotes at end");

        Message message = Message.parse("0047 " + MessageType.SER.getValue()
                + " 129.82.62.142 5070 \"Lord of the Rings\"");
        List<String> data = message.getData();

        Assert.assertEquals(message.getType(), MessageType.SER);
        Assert.assertEquals(data.get(0), "129.82.62.142");
        Assert.assertEquals(data.get(1), "5070");
        Assert.assertEquals(data.get(2), "Lord of the Rings");
    }

    @Test(priority = 4)
    public void testMessageParsingWithQuotesAtMiddle() {
        logger.info("Running Message Test 07 - Message parsing with quotes at middle");

        Message message = Message.parse("0072 " + MessageType.SER_OK.getValue()
                + " 3 129.82.128.1 2301 \"Lord of the Rings\" Cars Thor");
        List<String> data = message.getData();

        Assert.assertEquals(message.getType(), MessageType.SER_OK);
        Assert.assertEquals(data.get(0), "3");
        Assert.assertEquals(data.get(1), "129.82.128.1");
        Assert.assertEquals(data.get(2), "2301");
        Assert.assertEquals(data.get(3), "Lord of the Rings");
        Assert.assertEquals(data.get(4), "Cars");
        Assert.assertEquals(data.get(5), "Thor");
    }

    @Test(priority = 5)
    public void testClone() {
        logger.info("Running Message Test 08 - Clone");

        Message clonedMessage = message.clone();
        Assert.assertFalse(message == clonedMessage);                        // Equals methods had been overridden
        Assert.assertEquals(message.getData(), clonedMessage.getData());        // Only a shallow copy
    }
}
