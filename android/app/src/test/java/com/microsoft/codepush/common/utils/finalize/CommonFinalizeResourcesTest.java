package com.microsoft.codepush.common.utils.finalize;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.microsoft.codepush.common.utils.CodePushUtils.finalizeResources;
import static junit.framework.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CommonFinalizeResourcesTest {

    private static Closeable createGoodResource() {
        return new Closeable() {
            @Override
            public void close() throws IOException {
            }
        };
    }

    private static Closeable createBrokenResource(final IOException exceptionToThrow) {
        return new Closeable() {
            @Override
            public void close() throws IOException {
                if (exceptionToThrow == null) {
                    throw new IOException();
                } else {
                    throw exceptionToThrow;
                }
            }
        };
    }

    @Parameters(name = "{index}: finalizeResources({0}, null) = {1}")
    public static Collection<Object[]> data() {

        IOException exception1 = new IOException();
        IOException exception2 = new IOException();

        return Arrays.asList(new Object[][]{
                {Arrays.asList(createGoodResource()), null},
                {Arrays.asList(createGoodResource(), createGoodResource()), null},
                {Arrays.asList(createGoodResource(), createBrokenResource(exception1)), exception1},
                {Arrays.asList(createBrokenResource(exception1), createGoodResource()), exception1},
                {Arrays.asList(createBrokenResource(exception1), createBrokenResource(exception2)), exception2}
        });
    }

    @Parameter
    public List<Closeable> resourcesToTest;

    @Parameter(1)
    public IOException expectedException;

    @Test
    public void testFinalizeResources() {
        assertEquals(expectedException, finalizeResources(resourcesToTest, null));
    }
}
