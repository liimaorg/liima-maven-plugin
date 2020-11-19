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

import ch.mobi.liima.client.LiimaClient;
import ch.mobi.liima.client.dto.AppWithVersion;
import ch.mobi.liima.client.dto.Deployment;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;


import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmwDeployMultiPluginTest {

    @Test(expected = MojoFailureException.class)
    public void testSetupAmwApplicationPropertiesWithEmpty() throws MojoFailureException, MojoExecutionException {
        // arrange
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        plugin.applicationProperties = "         ";

        // act
        plugin.execute();

        // assert
        // --> exception
    }

    @Test
    public void testSetupAmwApplicationProperties() throws MojoFailureException {
        // arrange
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        String testApps = "ch_mobi_app1=,ch_mobi_app3=";

        // act
        Map<String, String> stringStringMap = plugin.parsePropertiesParameter(testApps);

        // assert
        assertEquals("Should have 2 applications", 2, stringStringMap.size());
        assertTrue("contains ch_mobi_app1",  stringStringMap.containsKey("ch_mobi_app1"));
    }

    @Test
    public void testCheckNoSnapshotVersionsToLocal() throws MojoExecutionException {
        // arrange
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        Map<String, String> targetAppsToDeploy = new HashMap<>();
        targetAppsToDeploy.put("ch_mobi_app_1", "2.0.0-SNAPSHOT");
        targetAppsToDeploy.put("ch_mobi_app_2", "1.2.3-SNAPSHOT");

        // act
        plugin.checkNoSnapshotVersions(targetAppsToDeploy, "local");

        // assert
        // --> nothing, all good
    }

    @Test(expected = MojoExecutionException.class)
    public void testCheckNoSnapshotVersionsToEnv() throws MojoExecutionException {
        // arrange
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        Map<String, String> targetAppsToDeploy = new HashMap<>();
        targetAppsToDeploy.put("ch_mobi_app_1", "2.0.0-SNAPSHOT");
        targetAppsToDeploy.put("ch_mobi_app_2", "1.2.3-SNAPSHOT");

        // act
        plugin.checkNoSnapshotVersions(targetAppsToDeploy, "c");

        // assert
        // --> exception
    }

    @Test(expected = MojoExecutionException.class)
    public void testDetermineVersionsWithInvalidApps() throws MojoExecutionException {
        // arrange
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        Map<String, String> sourceAmwApps = new HashMap<>();
        sourceAmwApps.put("ch_mobi_app_1", "");
        sourceAmwApps.put("ch_mobi_app_2", "");

        // act
        Map<String, String> amwApps = new HashMap<>();
        amwApps.put("ch_mobi_app_1", "");
        amwApps.put("ch_mobi_app_2", "");


        plugin.determineVersions(amwApps, sourceAmwApps);

        // assert
        // --> exception
    }

    @Test
    public void testDetermineVersionsWithValidApps() throws MojoExecutionException {
        // arrange
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        Map<String, String> sourceAmwApps = new HashMap<>();
        sourceAmwApps.put("ch_mobi_app_1", "1.2.3");
        sourceAmwApps.put("ch_mobi_app_2", "3.4");

        // act
        Map<String, String> amwApps = new HashMap<>();
        amwApps.put("ch_mobi_app_1", "2.2");
        amwApps.put("ch_mobi_app_2", "4.4");

        // both versions (source and wished) are give, assume promote mode, use source version
        Map<String, String> determinedVersions = plugin.determineVersions(amwApps, sourceAmwApps);

        // assert
        assertNotNull(determinedVersions);
        assertEquals("expect given version app1", "1.2.3", determinedVersions.get("ch_mobi_app_1"));
        assertEquals("expect given version app2", "3.4", determinedVersions.get("ch_mobi_app_2"));

    }

    @Test
    public void testApplicationParamParsing() throws MojoFailureException {
        // arrange
        String amwApps = "app1=1.0.0,app2=2.0.0,app4=4.0.0";

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("app1", "1.0.0");
        expectedMap.put("app2", "2.0.0");
        expectedMap.put("app4", "4.0.0");

        // act
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();
        Map<String, String> amwAppsMap = plugin.parsePropertiesParameter(amwApps);

        // assert
        assertThat(amwAppsMap, equalTo(expectedMap));

    }

    @Test
    public void testDeploymentParameterParsing() throws MojoFailureException {
        // arrange
        String amwApps = "teamcity.build.triggeredBy=Firstname Lastname\nteamcity.build.triggeredBy.username=exampleusername\nteamcity.build.name=Mobitor :: Deploy (Y)\nteamcity.project.name=Mobitor";
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("teamcity.build.triggeredBy", "Firstname Lastname");
        expectedMap.put("teamcity.build.triggeredBy.username", "exampleusername");
        expectedMap.put("teamcity.build.name", "Mobitor :: Deploy (Y)");
        expectedMap.put("teamcity.project.name", "Mobitor");

        // act
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();
        Map<String, String> amwAppsMap = plugin.parsePropertiesParameter(amwApps);

        // assert
        assertThat(amwAppsMap, equalTo(expectedMap));

    }

    @Test
    public void testRedeploySameVersionsIfTargetDeploymentNotSuccessful() throws MojoFailureException, MojoExecutionException {
        // arrange
        AmwDeployMultiPlugin plugin = spy(AmwDeployMultiPlugin.class);
        plugin.targetEnv = "target";
        plugin.sourceEnv = "source";
        plugin.serverName = "server";
        plugin.applicationProperties = "app1=,app2=";
        plugin.url = "https://liima.host.domain/AMW_rest/resources";


        LiimaClient mockAmwClient = mock(LiimaClient.class);
        Deployment currSrcDepl = createDeployment("server", "success", "source");
        Deployment currTgtDepl = createDeployment("server", "epicfail", "target");
        when(mockAmwClient.retrieveDeployment(eq("server"), eq("source"))).thenReturn(currSrcDepl);
        when(mockAmwClient.retrieveDeployment(eq("server"), eq("target"))).thenReturn(currTgtDepl);

        Deployment requestedDepl = new Deployment();
        requestedDepl.setId(1L);
        requestedDepl.setTrackingId(11L);
        when(mockAmwClient.sendNewDeployment(any())).thenReturn(requestedDepl);

        Deployment finalDepl = new Deployment();
        finalDepl.setState("success");
        when(mockAmwClient.trackDeploymentRequest(any(Deployment.class))).thenReturn(finalDepl);

        when(plugin.getClient()).thenReturn(mockAmwClient);

        // act
        plugin.execute();


        // assert
        verify(mockAmwClient, times(1)).retrieveDeployment(eq("server"), eq("source"));
        verify(mockAmwClient, times(1)).retrieveDeployment(eq("server"), eq("target"));

        // target deployment is failed, so a deployment request is expected:
        verify(mockAmwClient, times(1)).sendNewDeployment(any());
        verify(mockAmwClient, times(1)).trackDeploymentRequest(any());
    }

    @Test
    public void testRedeploySameVersionsToLocalTarget() throws MojoFailureException, MojoExecutionException {
        // arrange
        AmwDeployMultiPlugin plugin = spy(AmwDeployMultiPlugin.class);
        plugin.targetEnv = "Local";
        plugin.sourceEnv = "source";
        plugin.serverName = "server";
        plugin.applicationProperties = "app1=,app2=";
        plugin.url = "https://liima.host.domain/AMW_rest/resources";

        LiimaClient mockAmwClient = mock(LiimaClient.class);
        Deployment currSrcDepl = createDeployment("server", "success", "source");
        Deployment currTgtDepl = createDeployment("server", "success", "target");
        when(mockAmwClient.retrieveDeployment(eq("server"), eq("source"))).thenReturn(currSrcDepl);
        when(mockAmwClient.retrieveDeployment(eq("server"), eq("Local"))).thenReturn(currTgtDepl);

        Deployment requestedDepl = new Deployment();
        requestedDepl.setId(1L);
        requestedDepl.setTrackingId(11L);
        when(mockAmwClient.sendNewDeployment(any())).thenReturn(requestedDepl);

        Deployment finalDepl = new Deployment();
        finalDepl.setState("success");
        when(mockAmwClient.trackDeploymentRequest(any(Deployment.class))).thenReturn(finalDepl);

        when(plugin.getClient()).thenReturn(mockAmwClient);

        // act
        plugin.execute();


        // assert
        verify(mockAmwClient, times(1)).retrieveDeployment(eq("server"), eq("source"));
        verify(mockAmwClient, times(1)).retrieveDeployment(eq("server"), eq("Local"));

        // target deployment is failed, so a deployment request is expected:
        verify(mockAmwClient, times(1)).sendNewDeployment(any());
        verify(mockAmwClient, times(1)).trackDeploymentRequest(any());
    }

    private Deployment createDeployment(String serverName, String state, String env) {
        Deployment deployment = new Deployment();
        deployment.setAppServerName(serverName);
        deployment.setState(state);
        deployment.setEnvironmentName(env);
        AppWithVersion srcApp1 = new AppWithVersion("app1", "1");
        AppWithVersion srcApp2 = new AppWithVersion("app2", "2");
        deployment.addAppWithVersion(srcApp1);
        deployment.addAppWithVersion(srcApp2);

        return deployment;
    }
}
