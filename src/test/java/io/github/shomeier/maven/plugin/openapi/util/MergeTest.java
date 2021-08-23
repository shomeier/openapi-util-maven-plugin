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

public class MergeTest {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {}

        @Override
        protected void after() {}
    };

    @Test
    public void testMergeSimple() throws Exception {
        File pom = new File("target/test-classes/merge-simple");
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
                .getResource("merge-simple/src/main/resources/expectedOutput.yaml").getFile());
        assertEquals("The files differ!",
                FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(outputFile, "utf-8"));
    }

    @Test
    public void testMergeResourcesIncludes() throws Exception {

        String prjFolder = "merge-resources-includes";
        File pom = new File("target/test-classes/" + prjFolder);
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
                .getResource(prjFolder + "/src/main/resources/expectedOutput.yaml").getFile());
        assertEquals("The files differ!",
                FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(outputFile, "utf-8"));
    }

    @Test
    public void testMergeResourcesExcludes() throws Exception {
        String prjFolder = "merge-resources-excludes";
        File pom = new File("target/test-classes/" + prjFolder);
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
                .getResource(prjFolder + "/src/main/resources/expectedOutput.yaml")
                .getFile());
        assertEquals("The files differ!",
                FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(outputFile, "utf-8"));
    }

    @Test
    public void testMergeResolve() throws Exception {

        String prjName = "merge-resolve";
        File pom = new File("target/test-classes/" + prjName);
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
                .getResource(prjName + "/src/main/resources/expectedOutput.yaml").getFile());
        assertEquals("The files differ!",
                FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(outputFile, "utf-8"));
    }

    @Test
    public void testMergeResolveFully() throws Exception {

        String prjName = "merge-resolveFully";
        File pom = new File("target/test-classes/" + prjName);
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
                .getResource(prjName + "/src/main/resources/expectedOutput.yaml").getFile());
        assertEquals("The files differ!",
                FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(outputFile, "utf-8"));
    }

    @Test
    public void testMergeExclude() throws Exception {
        File pom = new File("target/test-classes/merge-exclude");
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
                .getResource("merge-exclude/src/main/resources/expectedOutput.yaml").getFile());
        assertEquals("The files differ!",
                FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(outputFile, "utf-8"));
    }

    @Test
    public void testMergeTransform() throws Exception {

        String prjName = "merge-transform";
        File pom = new File("target/test-classes/" + prjName);
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
                .getResource(prjName + "/src/main/resources/expectedOutput.yaml").getFile());
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

