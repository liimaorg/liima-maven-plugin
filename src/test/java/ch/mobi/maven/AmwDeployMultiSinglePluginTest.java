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
import ch.mobi.liima.client.dto.DeploymentParameter;
import com.jayway.jsonpath.JsonPath;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AmwDeployMultiSinglePluginTest extends AbstractLocalAmwTestBase {

    @Test
    public void testPreservePropertiesOnPromote() throws MojoFailureException, MojoExecutionException {
        // arrange
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        AmwClientMock amwClientMock = new AmwClientMock();
        plugin.setClient(amwClientMock);

        // set some plugin defaults manually:
        plugin.serverName = "serverName1";
        plugin.applicationName = "appName1";
        plugin.version = "";
        plugin.sourceEnv = "Y";
        plugin.targetEnv = "C";
        plugin.preservePropertiesPrefix = "dep.";


        // act
        plugin.execute();

        // assert
        Deployment newDeployment = amwClientMock.getNewDeployment();
        List<DeploymentParameter> deploymentParameters = newDeployment.getDeploymentParameters();
        assertTrue(keyExists("dep.paramName1.version", deploymentParameters));
        assertTrue(keyExists("dep.paramName2.version", deploymentParameters));
        assertTrue(keyExists("dep.paramName3.version", deploymentParameters));
    }

    private boolean keyExists(String key, List<DeploymentParameter> deploymentParameters) {
        for (DeploymentParameter deploymentParameter : deploymentParameters) {
            if (deploymentParameter.getKey().equals(key)) {
                return true;
            }
        }

        return false;
    }

    @Test
    public void testSendAmwSingleDeployment() throws MojoFailureException, MojoExecutionException, IOException {
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        plugin.version = "1.2.889";
        plugin.applicationName = "application_name_1";

        testSendAmwDeployment(plugin);
    }

    @Test
    public void testSendAmwMultiDeployment() throws MojoFailureException, MojoExecutionException, IOException {
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();
        plugin.applicationProperties = "application_name_1=";

        testSendAmwDeployment(plugin);
    }

    @Test
    public void testSendAmwMultiDeploymentWithDeployParams() throws MojoFailureException, MojoExecutionException, IOException {
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        plugin.version = "1.2.889";
        plugin.applicationName = "application_name_1";
        plugin.deploymentProperties = "key1=value1,key2=value2";

        FakeAmwResponseSingleDeployHandler handler = testSendAmwDeployment(plugin);

        assertNotNull(handler.getPostJson());
        assertNotNull(JsonPath.read(handler.getPostJson(), "$.deploymentParameters"));
        assertEquals("key1", JsonPath.read(handler.getPostJson(), "$.deploymentParameters[0].key"));
        assertEquals("value1", JsonPath.read(handler.getPostJson(), "$.deploymentParameters[0].value"));
        assertEquals("key2", JsonPath.read(handler.getPostJson(), "$.deploymentParameters[1].key"));
        assertEquals("value2", JsonPath.read(handler.getPostJson(), "$.deploymentParameters[1].value"));
    }

    private FakeAmwResponseSingleDeployHandler testSendAmwDeployment(AbstractAmwDeployPlugin plugin) throws MojoFailureException, MojoExecutionException, IOException {
        // arrange
        FakeAmwResponseSingleDeployHandler handler = new FakeAmwResponseSingleDeployHandler("amw-deployment-query-env.json", "amw-tracking-deployment-scheduled.json");
        this.serverBootstrap.registerHandler("*", handler);
        startServer();

        // act
        // setup amw plugin, set some fields maven would do manually:
        plugin.maxPollingDurationInMs = 60000;
        plugin.pollingIntervalInMs = 200;
        plugin.url = baseURL;
        // setup deployment parameters:
        plugin.serverName = "server_name_1";
        plugin.targetEnv = "C";
        plugin.sourceEnv = "Y";

        // act
        plugin.execute();

        // assert
        assertTrue(handler.getGetRequestCount() >= 2);
        assertEquals(1, handler.getPostRequestCount());
        assertEquals(0, handler.getOtherRequestCount());

        return handler;
    }

}
