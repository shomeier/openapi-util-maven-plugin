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
        testProject("merge-simple");
    }

    @Test
    public void testMergeResourcesIncludes() throws Exception {
        testProject("merge-resources-includes");
    }

    @Test
    public void testMergeResourcesExcludes() throws Exception {
        testProject("merge-resources-excludes");
    }

    @Test
    public void testMergeResolve() throws Exception {
        testProject("merge-resolve");
    }

    @Test
    public void testMergeResolveFully() throws Exception {
        testProject("merge-resolveFully");
    }

    @Test
    public void testMergeExclude() throws Exception {
        testProject("merge-exclude");
    }

    @Test
    public void testMergeTransform() throws Exception {
        testProject("merge-transform");
    }

    @Test
    public void testMergeTransformOneFileHeader() throws Exception {
        testProject("merge-transform-one-file-header");
    }

    @Test
    public void testMergeTransformNoFileHeader() throws Exception {
        testProject("merge-transform-no-file-header");
    }

    /** Do not need the MojoRule. */
    @WithoutMojo
    @Test
    public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
        assertTrue(true);
    }

    private void testProject(String prjFolder) throws Exception {

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

}

