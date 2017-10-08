package org.microfuse.file.sharer.node;

import org.microfuse.file.sharer.node.core.ServiceHolder;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * The base test case class which needs to be extended by all test cases.
 */
public class BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BaseTestCase.class);

    @BeforeMethod
    public void baseInitializeMethod() {
        // Resetting ServiceHolder singleton fields
        List<String> managerFields = Arrays.asList("configuration", "router", "resourceIndex", "peerType");
        managerFields.forEach(managerField -> {
            try {
                Field field = ServiceHolder.class.getDeclaredField(managerField);
                field.setAccessible(true);
                field.set(ServiceHolder.class, null);
                field.setAccessible(false);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                logger.warn("Test Case Cleanup: Failed to reset field " + managerField
                        + " in " + ServiceHolder.class.getName());
            }
        });

        // Deleting configuration file
        new File(Constants.CONFIG_FILE).delete();
    }
}
