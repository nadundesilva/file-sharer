package org.microfuse.file.sharer.node;

import org.microfuse.file.sharer.node.core.Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * The base test case class which needs to be extended by all test cases.
 */
public class BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BaseTestCase.class);

    @BeforeMethod
    public void cleanUp() {
        List<String> managerFields = Arrays.asList("configuration", "router", "resourceIndex");

        managerFields.forEach(managerField -> {
            try {
                Field field = Manager.class.getDeclaredField(managerField);
                field.setAccessible(true);
                field.set(Manager.class, null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                logger.warn("Test Case Cleanup: Failed to reset field " + managerField + " in Manager.");
            }
        });
    }
}
