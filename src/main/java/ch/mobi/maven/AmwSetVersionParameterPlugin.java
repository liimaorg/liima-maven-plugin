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
import ch.mobi.liima.client.LiimaConfiguration;
import ch.mobi.liima.client.dto.Deployment;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.text.MessageFormat;
import java.util.Collections;


/**
 * This plugin allows to query AMW for a deployed version of an application.
 * <p>
 * It will then set a Maven Property (see: {@link #propertyName}) and also log the key=value in the
 * TeamCity setParameter syntax.
 * <p>
 * <p>
 * See: <a href="https://confluence.jetbrains.com/display/TCD9/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-AddingorChangingaBuildParameter">Changing a Build Parameter</a>
 */
@Mojo(name = "set-version-parameter", requiresProject = false)
public class AmwSetVersionParameterPlugin extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    MavenProject project;

    /** AMW REST interface url, no trailing slash, e.g. https://liima.host.domain/AMW_rest/resources  */
    @Parameter(property = "amwUrl")
    String url;

    /** AMW server name, used to determine the deployed version */
    @Parameter(property = "amwServerName", required = true)
    String serverName;

    /** AMW applicationName, used to deploy the deployed version */
    @Parameter(property = "amwApplicationName", required = true)
    String applicationName;

    /** AMW environment, used to retrieve the version */
    @Parameter(property = "amwSourceEnv", defaultValue = "Local", required = true)
    String sourceEnv;

    /** name of the property to be used to set the value of the retrieved version as maven property and as TeamCity parameter */
    @Parameter(property = "versionPropertyName", defaultValue = "amw.version")
    String propertyName;


    @Override
    public void execute() throws MojoExecutionException {
        if (StringUtils.isBlank(sourceEnv)) {
            getLog().warn("Source environment is blank! Will do nothing.");
            return;
        }

        LiimaConfiguration amwConf = new LiimaConfiguration(url);
        LiimaClient client = new LiimaClient(amwConf);

        try {
            String logMsg = MessageFormat.format("Will query version for server [{0}] and application [{1}] on environment [{2}]", serverName, applicationName, sourceEnv);
            getLog().info(logMsg);

            String versionFromProps = project.getProperties().getProperty(propertyName);
            getLog().info(MessageFormat.format("Original property [{0}={1}]", propertyName, versionFromProps));

            Deployment deployment = client.retrieveDeployment(serverName, sourceEnv);

            String version;
            if (deployment != null && deployment.isSuccessful()) {
                version = client.getVersion(applicationName, Collections.singletonList(deployment));
                getLog().info("Found a version to set: " + version);

            } else {
                getLog().warn("Deployment could not be determined or was not successful! Will set empty version.");
                version = "";
            }

            // write this for TeamCity, see: https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity
            String teamCitySetParam = MessageFormat.format("##teamcity[setParameter name=''{0}'' value=''{1}'']", propertyName, version);
            getLog().info(teamCitySetParam);
            project.getProperties().setProperty(propertyName, version);

        } catch (Exception e) {
            throw new MojoExecutionException("Could not retrieve version from AMW", e);
        }
    }
}
