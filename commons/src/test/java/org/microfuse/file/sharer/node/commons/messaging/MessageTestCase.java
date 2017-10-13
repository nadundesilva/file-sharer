package org.microfuse.file.sharer.node.commons.messaging;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.commons.messaging.Message class.
 */
public class MessageTestCase {
    private Message message;

    @BeforeMethod
    public void initializeMethod() {
        message = new Message();
        message.setType(MessageType.SER);
        message.setData(Lists.newArrayList("129.82.62.142", "5070", "Lord of the Rings"));
    }

    @Test
    public void testGetData() {
        Assert.assertEquals(message.getData(1), "5070");
    }

    @Test
    public void testSetData() {
        message.setData(1, "5080");
        Assert.assertEquals(message.getData().get(1), "5080");
    }

    @Test
    public void testClone() {
        Message clonedMessage = message.clone();
        Assert.assertFalse(message == clonedMessage);                        // Equals methods had been overridden
        Assert.assertEquals(message.getData(), clonedMessage.getData());        // Only a shallow copy
    }

    @Test
    public void testMessageParsingWithQuotesAtEnd() {
        Message message = Message.parse("0047 " + MessageType.SER.getValue()
                + " 129.82.62.142 5070 \"Lord of the Rings\"");
        List<String> data = message.getData();

        Assert.assertEquals(message.getType(), MessageType.SER);
        Assert.assertEquals(data.get(0), "129.82.62.142");
        Assert.assertEquals(data.get(1), "5070");
        Assert.assertEquals(data.get(2), "Lord of the Rings");
    }

    @Test
    public void testMessageParsingWithQuotesAtMiddle() {
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

    @Test
    public void testToString() {
        Assert.assertEquals(message.toString(), "0047 SER 129.82.62.142 5070 \"Lord of the Rings\"");
    }
}
