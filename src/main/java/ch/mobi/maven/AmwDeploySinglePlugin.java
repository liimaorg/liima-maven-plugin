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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sends a deployment request to AMW for a single application (one server, one ear file).
 */
@Mojo(name = "deploy-single", requiresProject = false)
public class AmwDeploySinglePlugin extends AmwDeployMultiPlugin {

    /** AMW application name */
    @Parameter(property = "amwApplicationName", required = true)
    String applicationName;

    /** Version of the artifact to deploy (version of the .ear file in the maven repository) */
    @Parameter(property = "amwVersion")
    String version;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String ver = (version == null) ? "" : version;
        this.applicationProperties = applicationName + "=" + ver;
        super.execute();
    }

}
