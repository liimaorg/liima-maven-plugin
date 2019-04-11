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

import ch.mobi.liima.client.dto.AppWithVersion;
import ch.mobi.liima.client.dto.Deployment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AmwDeploySinglePluginHttpMockTest extends AbstractLocalAmwTestBase {


    @Test
    public void testNoDeploymentRequestWhenFailed() throws Exception {
        // arrange
        Deployment requestedDeployment = new Deployment();
        AppWithVersion appWithVersion = new AppWithVersion("application_name_1", "1.20.0-889");
        requestedDeployment.addAppWithVersion(appWithVersion);

        FakeAmwResponseSingleDeployHandler handler = new FakeAmwResponseSingleDeployHandler("amw-deployment-response-failed.json", "amw-tracking-deployment-scheduled.json");

        this.serverBootstrap.registerHandler("*", handler);
        startServer();

        // setup amw plugin, set some fields maven would do manually:
        AmwDeploySinglePlugin plugin = new AmwDeploySinglePlugin();
        plugin.version = "1.20.0-889";
        plugin.serverName = "server_name_1";
        plugin.targetEnv = "C";
        plugin.sourceEnv = "Y";

        plugin.maxPollingDurationInMs = 60000;
        plugin.pollingIntervalInMs = 200;
        plugin.url = baseURL;


        // act
        plugin.execute();

        // assert
        assertEquals(6, handler.getGetRequestCount());
        assertEquals(1, handler.getPostRequestCount());
        assertEquals(0, handler.getOtherRequestCount());

    }

}
