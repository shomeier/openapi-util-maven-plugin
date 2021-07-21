package io.github.shomeier.maven.plugin.openapi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import org.junit.Test;
import io.github.shomeier.maven.plugin.openapi.util.Merge;

public class MergeTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {}

        @Override
        protected void after() {}
    };

    /**
     * @throws Exception if any
     */
    @Test
    public void test() throws Exception {
        File pom = new File("target/test-classes/project-to-test/");
        assertNotNull(pom);
        assertTrue(pom.exists());

        Merge myMojo = (Merge) rule.lookupConfiguredMojo(pom, "merge");
        assertNotNull(myMojo);
        myMojo.execute();

        File outputFile = (File) rule.getVariableValueFromObject(myMojo, "outputFile");
        assertNotNull(outputFile);
        assertTrue(outputFile.exists());

        ClassLoader classLoader = getClass().getClassLoader();
        File expectedFile = new File(classLoader
                .getResource("project-to-test/src/main/resources/merge/simple/expectedOutput.yaml").getFile());
        assertEquals("The files differ!",
                FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(outputFile, "utf-8"));
    }

    /** Do not need the MojoRule. */
    @WithoutMojo
    @Test
    public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
        assertTrue(true);
    }

}

