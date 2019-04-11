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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AmwSetVersionParameterPluginTest extends AbstractLocalAmwTestBase {

    private AmwSetVersionParameterPlugin plugin;
    private StringBuilder sb;

    private void setupPlugin() {
        plugin = new AmwSetVersionParameterPlugin();
        plugin.applicationName = "application_name_2";
        plugin.serverName = "server_name_2";
        plugin.sourceEnv = "Y";
        plugin.propertyName = "testPropertyName";
        plugin.url = this.baseURL;
        plugin.project = new MavenProject();

        sb = new StringBuilder();
        Log sbLog = new StringBufferedLogger(sb);
        plugin.setLog(sbLog);
    }

    @Test
    public void testExecuteWithSuccessDeployment() throws Exception {
        // arrange
        this.serverBootstrap.registerHandler("*", new FakeAmwRepsonseHandler("amw-deployments-query-response.json"));
        startServer();

        plugin = new AmwSetVersionParameterPlugin();
        plugin.applicationName = "application_name_2";
        plugin.serverName = "server_name_2";
        plugin.sourceEnv = "Y";
        plugin.propertyName = "testPropertyName";
        plugin.url = this.baseURL;
        plugin.project = new MavenProject();
        String expectedVersion = "0.1.36";

        sb = new StringBuilder();
        Log sbLog = new StringBufferedLogger(sb);
        plugin.setLog(sbLog);

        // act
        plugin.execute();

        // assert
        assertTrue(sb.toString().contains("##teamcity[setParameter name='testPropertyName' value='0.1.36']"));
        assertNotNull(plugin.project.getProperties().getProperty("testPropertyName"));
        assertEquals(expectedVersion, plugin.project.getProperties().getProperty("testPropertyName"));
    }

    @Test
    public void testExecuteWithBlankSourceEnv() throws Exception {
        // arrange
        setupPlugin();
        plugin.sourceEnv = "   ";
        plugin.propertyName = "testPropertyName";
        plugin.project = new MavenProject();

        sb = new StringBuilder();
        Log sbLog = new StringBufferedLogger(sb);
        plugin.setLog(sbLog);

        // act
        plugin.execute();

        // assert
        assertTrue(sb.toString().contains("Source environment is blank!"));
        assertFalse(sb.toString().contains("##teamcity[setParameter"));
    }

    @Test
    public void testExecute() throws Exception {
        // arrange
        this.serverBootstrap.registerHandler("*", new FakeAmwRepsonseHandler("amw-deployments-query-response.json"));
        startServer();

        setupPlugin();

        // act
        plugin.execute();

        // assert
        assertTrue(sb.toString().contains("##teamcity[setParameter name="));
        assertNotNull(plugin.project.getProperties().getProperty("testPropertyName"));
    }

    @Test
    public void testExecuteWithAmwFailure() throws Exception {
        // arrange
        this.serverBootstrap.registerHandler("*", new FakeAmwRepsonseHandler("non-existent-response.json"));
        startServer();

        setupPlugin();

        sb = new StringBuilder();
        Log sbLog = new StringBufferedLogger(sb);
        plugin.setLog(sbLog);

        // act
        plugin.execute();

        // assert
        assertTrue(sb.toString().contains("##teamcity[setParameter name='testPropertyName' value='']"));
    }

    @Test
    public void testExecuteWithEmptySourceEnv() throws Exception {
        // arrange
        setupPlugin();
        plugin.sourceEnv = "";
        plugin.serverName = "server_name_3";
        plugin.applicationName = "application_name_3";

        // act
        plugin.execute();

        // assert
        assertFalse(sb.toString().contains("##teamcity[setParameter"));
    }

}
