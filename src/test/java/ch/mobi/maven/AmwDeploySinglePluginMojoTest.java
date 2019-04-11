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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

public class AmwDeploySinglePluginMojoTest {

    @Rule
    public TestResources resources = new TestResources();

    @Rule
    public MojoRule rule = new MojoRule()
    {
        @Override
        protected void before() {
        }

        @Override
        protected void after() {
        }
    };

    @Test
    @Ignore
    public void testExecute() throws Exception {
        File basedir = resources.getBasedir("deploy-single");
//        rule.executeMojo(basedir, "deploy-single");

//        Mojo mojoWithPluginConfig = rule.lookupMojo("ch.mobi.maven", "amw-maven-plugin", "2.0.0-SNAPSHOT", "deploy-single", null);
//        assertNotNull(mojoWithPluginConfig);


//        File pom = new File(basedir, "pom.xml");
//        Mojo mojoWithPom = rule.lookupMojo("deploy-single", pom);
//        Mojo mojo = rule.configureMojo(mojoWithPom, "amw-maven-plugin", pom);

        // currently the maven-plugin-testing-harness only sets values from <configuration />
        // so no default values are set
        // see: http://stackoverflow.com/questions/34601634/maven-plugin-mojo-configure-default-parameter-values
        // seems to work using lookupConfiguredMojo(): http://stackoverflow.com/questions/31528763/how-to-populate-parameter-defaultvalue-in-maven-abstractmojotestcase
        AmwDeploySinglePlugin plugin = (AmwDeploySinglePlugin) rule.lookupConfiguredMojo(basedir, "deploy-single");

        assertNotNull(plugin);
        assertNotNull(plugin.url);

         plugin.execute();

//        MavenProject mavenProject = rule.readMavenProject(basedir);
//        rule.executeMojo(mavenProject, "deploy-single");
    }

}
