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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This plugin allows to determine the version of a deployed AMW application. It can then download a systemtest artifact,
 * typically a .zip file, extract it and then run the JUnit tests in that directory.
 * <p>
 * The systemtest artifact is usually created during the release build (to share the same version as the deployment).
 */
@Mojo(name = "get-systemtest-zip", requiresProject = false)
public class AmwGetSystemtestZipPlugin extends AbstractMojo {

    /** AMW REST interface url, no trailing slash, e.g. https://liima.host.domain/AMW_rest/resources  */
    @Parameter(property = "amwUrl")
    String url;

    /** AMW server name, used to determine the deployed version */
    @Parameter(property = "amwServerName", required = true)
    String amwServerName;

    /** AMW applicationName, used to deploy the deployed version */
    @Parameter(property = "amwApplicationName", required = true)
    String amwApplicationName;

    /** AMW environment, used to determine the deployed version, paramter is not case sensitive */
    @Parameter(property = "amwEnvironmentName", defaultValue = "Local")
    String amwEnvironmentName;

    /** Maven groupId of the systemtest artifact to retrieve from the repository */
    @Parameter(property = "groupId", required = true)
    String groupId;

    /** Maven artifactId of the systemtest artifact to retrieve from the repository */
    @Parameter(property = "artifactId", required = true)
    String artifactId;

    /** Version of the systemtest artifact to retrieve. If empty the plugin will attempt to retrieve this information
     * from AMW
     */
    @Parameter(property = "version")
    String version;

    /** Maven classifier of the systemtest artifact */
    @Parameter(property = "classifier", defaultValue = "systemtest")
    String classifier;

    @Component
    protected ArtifactResolver artifactResolver;
    @Component
    protected ArtifactFactory artifactFactory;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    List<ArtifactRepository> pomRemoteRepositories;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    ArtifactRepository localRepository;

    @Parameter(defaultValue = "${basedir}")
    File baseDirectory;

    @Component
    protected ArchiverManager archiverManager;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Using basedir: " + baseDirectory.getAbsolutePath());

        LiimaConfiguration liimaConf = new LiimaConfiguration(url);
        LiimaClient liimaClient = new LiimaClient(liimaConf);

        //
        // get version of the artifact from AMW
        //
        if (version == null) {
            getLog().info("Version paramter not set, will query AMW...");
            try {
                version = liimaClient.retrieveVersion(amwApplicationName, amwEnvironmentName, amwServerName);
            } catch (Exception e) {
                throw new MojoExecutionException("Could not retrieve version from AMW", e);
            }
        }

        //
        // use groupId + artifactId + retrieved version + type:zip to download the systemtests.zip:
        //
        Artifact zipToDownload = classifier == null ? artifactFactory.createBuildArtifact(groupId, artifactId, version, "zip")
                : artifactFactory.createArtifactWithClassifier(groupId, artifactId, version, "zip", classifier);

        List<ArtifactRepository> repoList = new ArrayList<>();
        if (pomRemoteRepositories != null) {
            repoList.addAll(pomRemoteRepositories);
        }

        getLog().info("Resolving " + zipToDownload);
        try {
            artifactResolver.resolveAlways(zipToDownload, repoList, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Could not resolve artifact", e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Artifact could not be found", e);
        }

        //
        // unpack the zip
        //
        File systemTestsZip = zipToDownload.getFile();
        getLog().info("Retrieved file and put into: " + systemTestsZip.getName() + " placed into " + systemTestsZip.getAbsolutePath());
        getLog().info("Copy file to: " + baseDirectory.getAbsolutePath());

        try {
            FileUtils.copyFileToDirectory(systemTestsZip, baseDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not process downloaded artifact", e);
        }
        unpack(zipToDownload, baseDirectory);
        getLog().info("unpacked zip in " + baseDirectory.getAbsolutePath());
    }

    protected void unpack(Artifact artifact, File destination) throws MojoExecutionException {
        File file = artifact.getFile();
        try {
            UnArchiver unArchiver;
            try {
                unArchiver = archiverManager.getUnArchiver(artifact.getType());
                getLog().debug("Found unArchiver by type: " + unArchiver);
            } catch (NoSuchArchiverException e) {
                unArchiver = archiverManager.getUnArchiver(file);
                getLog().debug("Found unArchiver by extension: " + unArchiver);
            }

            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(destination);

            unArchiver.extract();

        } catch (NoSuchArchiverException ex) {
            throw new MojoExecutionException("Unknown archiver type", ex);
        }
    }

}
