package ch.mobi.maven;

/*-
 * §
 * AMW Maven Plugin
 * --
 * Copyright (C) 2019 die Mobiliar
 * --
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * §§
 */

import ch.mobi.liima.client.dto.Deployment;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AmwDeploySinglePluginTest {

    @Test
    public void testDetermineVersionWithBothVersionAndSourceVersion() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        final String version = "1.0.20";
        final String sourceVersion = "1.0-10";

        // act
        String selectedVersion = plugin.determineVersion(version, sourceVersion);

        // assert
        assertEquals("use sourceVersion", sourceVersion, selectedVersion);
    }

    @Test
    public void testDetermineVersionWithSameVersions() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();

        // act
        String selectedVersion = plugin.determineVersion("1.0-20", "1.0-20");

        // assert
        assertEquals("use any version", "1.0-20", selectedVersion);
    }

    @Test
    public void testDetermineVersionWithNonNullVersionAndNullSourceVersion() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        final String version = "1.0.20";
        final String sourceVersion = null;

        // act
        String selectedVersion = plugin.determineVersion(version, sourceVersion);

        // assert
        assertEquals("use version", version, selectedVersion);
    }

    @Test
    public void testDetermineVersionWithNullVersionAndNonNullSourceVersion() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        final String version = null;
        final String sourceVersion = "1.0-10";

        // act
        String selectedVersion = plugin.determineVersion(version, sourceVersion);

        // assert
        assertEquals("use sourceVersion", sourceVersion, selectedVersion);
    }

    @Test(expected = MojoExecutionException.class)
    public void testDetermineVersionWithAllNull() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        final String version = null;
        final String sourceVersion = null;

        // act
        plugin.determineVersion(version, sourceVersion);

        // assert
        // => exception
    }

    private boolean getDeploymentRequestCompleted(String state) {
        Deployment deployment = new Deployment();
        deployment.setState(state);

        // act
        boolean completed = deployment.isComplete();
        return completed;
    }

    @Test
    public void testDeploymentRequestCompletedWithSuccess() {
        // arrange
        // act
        boolean completed = getDeploymentRequestCompleted("success");

        // assert
        assertTrue(completed);
    }

    @Test
    public void testDeploymentRequestCompletedWithFailure() {
        // arrange
        // act
        boolean completed = getDeploymentRequestCompleted("failed");

        // assert
        assertTrue(completed);
    }

    @Test
    public void testDeploymentRequestCompletedWithCanceled() {
        // arrange
        // act
        boolean completed = getDeploymentRequestCompleted("canceled");

        // assert
        assertTrue(completed);
    }

    @Test
    public void testDeploymentRequestCompletedWithRejected() {
        // arrange
        // act
        boolean completed = getDeploymentRequestCompleted("rejected");

        // assert
        assertTrue(completed);
    }

    @Test
    public void testDeploymentRequestCompletedWithInvalidValue() {
        // arrange
        // act
        boolean completed = getDeploymentRequestCompleted("OhMyDarling");

        // assert
        assertFalse(completed);
    }

    @Test
    public void testDeploymentRequestCompletedWithNull() {
        // arrange
        // act
        boolean completed = getDeploymentRequestCompleted(null);

        // assert
        assertFalse(completed);
    }



    @Test(expected = MojoExecutionException.class)
    public void testCheckForDeploymentFailure() throws Exception {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        Deployment deployment = new Deployment();
        deployment.setState("failed");

        // act
        plugin.checkForDeploymentState(deployment);

        // assert
        // => exception
    }

    @Test
    public void testDeploymentIsRequested() throws Exception {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        Deployment deployment = new Deployment();
        deployment.setState("requested");

        // act
        plugin.checkForDeploymentState(deployment);

        // assert
        assertTrue("nothing happened, no exception", true);
    }

    @Test
    public void testDeploymentShouldBeStateRequested() throws Exception {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        Deployment deployment = new Deployment();
        deployment.setState("scheduled");

        // act
        plugin.checkForDeploymentState(deployment);

        // assert
        assertTrue("nothing happened, no exception", true);
    }

    @Test(expected = MojoExecutionException.class)
    public void testDeploymentStateDelayedThrowsException() throws Exception {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        Deployment deployment = new Deployment();
        deployment.setState("delayed");

        // act
        plugin.checkForDeploymentState(deployment);

        // assert
        // --> exception
    }

    @Test(expected = MojoExecutionException.class)
    public void testSnapshotVersion() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();

        // act
        plugin.checkNoSnapshotVersion("1.0.0-SNAPSHOT", "c");
    }

    @Test
    public void testSnapshotVersionOnLocal() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();

        // act
        plugin.checkNoSnapshotVersion("1.0.0-SNAPSHOT", "local");

        // assert
        // --> no exception
    }

    @Test
    public void testSnapshotVersionNullParams() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();

        // act
        plugin.checkNoSnapshotVersion(null, null);

        // assert
        // -> nothing happened
    }

    @Test
    public void testSnapshotVersionWithNullHint() throws MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();

        // act
        plugin.checkNoSnapshotVersion("10.0", null);

        // assert
        // -> nothing happened
    }
}
