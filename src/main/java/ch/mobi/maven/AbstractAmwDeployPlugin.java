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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.text.MessageFormat;

import static org.codehaus.plexus.util.StringUtils.isBlank;
import static org.codehaus.plexus.util.StringUtils.isNotBlank;

public abstract class AbstractAmwDeployPlugin extends AbstractMojo {

    /** AMW REST interface url, no trailing slash, e.g. https://liima.host.domain/AMW_rest/resources  */
    @Parameter(property = "amwUrl")
    String url;

    /** AMW server name */
    @Parameter(property = "amwServerName", required = true)
    String serverName;

    /** AMW release name */
    @Parameter(property = "amwRelease", required = true)
    String release;

    /** if the version is omitted the plugin allows to determine the version from an environment and use that
     * version for deployment on the {@link #targetEnv}. The parameter is not case sensitive.
     */
    @Parameter(property = "amwSourceEnv")
    String sourceEnv;

    /** AMW target environment (where the deployment should end up). The parameter is not case sensitive. */
    @Parameter(property = "amwTargetEnv")
    String targetEnv;

    /**
     * A prefix used if a deployment is promoted from a source- to a target-environment to preserve some
     * deployment properties. Properties which keys start with this prefix will be re-used for the new
     * deployment request.
     */
    @Parameter(property = "amwPreservePropertiesPrefix", defaultValue = "dep.")
    String preservePropertiesPrefix;

    /**
     * maximum duration after which the plugin will interpret an AMW deployment as failed
     *
     * defaults to 10 Minutes
     */
    @Parameter(property = "amwMaxPollingDuration", defaultValue = "600000")
    long maxPollingDurationInMs = 600000;

    /** time between polling requests to determine the deployment state, defaults to 2 seconds */
    @Parameter(property = "amwPollingInterval", defaultValue = "2000")
    long pollingIntervalInMs = 2000;

    /**
     * whether to write log statements that are picked up by TeamCity build interaction
     * @see <a href="https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity">Build Interaction</a>
     */
    @Parameter(property = "writeTeamCityParameters", defaultValue = "true")
    boolean writeTeamCityParameters = false; // nasty hack: have it off in unit tests but on in plugin execution

    /**
     * if this parameter is set, the deployment will be set to todays date, using the given time. If this has already
     * passed, it is up to AMW (Liima) what it does with the request.
     * Format is HH:mm
     *
     * @see java.time.LocalTime
     */
    @Parameter(property = "amwDeploymentTime")
    String deploymentTime;

    private LiimaClient client;

    /**
     * @throws MojoExecutionException if no version can be determined (usually both are empty)
     */
    String determineVersion(String version, String sourceVersion) throws MojoExecutionException {
        logInfo("will determine which version to use between version [{0}] and source version [{1}]...", version, sourceVersion);

        if (isNotBlank(version) && isNotBlank(sourceVersion) && version.equals(sourceVersion)) {
            getLog().info("version parameter and source version are the same: " + version);
            return version;
        }

        // - if version is set and sourceVersion is set and they are not the same:
        if (isNotBlank(version) && isNotBlank(sourceVersion) && !version.equals(sourceVersion)) {
            getLog().warn("Both 'version' and 'sourceEnv' parameters are set.");
            getLog().warn(" version       = " + version);
            getLog().warn(" sourceVersion = " + sourceVersion);
            getLog().warn(" => Ignoring version, using sourceVersion: " + sourceVersion);
            return sourceVersion;
        }

        if (isBlank(sourceVersion) && isNotBlank(version)) {
            getLog().info("source version is empty, using version parameter: " + version);
            return version;
        }

        if (isBlank(version) && isNotBlank(sourceVersion)) {
            getLog().info("version is empty, using source version: " + sourceVersion);
            return sourceVersion;
        }

        throw new MojoExecutionException("both 'version' and 'sourceVersion' are blank or not suitable for a deployment request");
    }

    Deployment getDeploymentFromAmw(String appServerName, String environment) {
        try {
            Deployment deployment = getClient().retrieveDeployment(appServerName, environment);
            return deployment;

        } catch (Exception e) {
            getLog().error("Could not query AMW for deployed version.", e);
        }

        return null;
    }

    void checkNoSnapshotVersion(String version, String targetEnv) throws MojoExecutionException {
        // check combinations of version and sourceEnv
        if ("local".equalsIgnoreCase(targetEnv)) {
            getLog().info("targetEnv is Local environment, skipping SNAPSHOT check.");
            return;
        }

        if (StringUtils.isNotBlank(version) && version.contains("SNAPSHOT")) {
            String msg = MessageFormat.format("SNAPSHOT version detected: {0}! Will abort.", version);
            getLog().error(msg);
            throw new MojoExecutionException(msg);
        }
    }

    void checkForDeploymentState(Deployment finalDeployment) throws MojoExecutionException {
        if (finalDeployment.isRequested()) {
            logInfo("Deployment state is requested. This deployment needs to be approved in AMW.");
            return;
        }

        if (finalDeployment.hasFailed()) {
            String exMsg = formatMessage("AMW Deployment (TrackingId [{0,number,#}] failed.", finalDeployment.getTrackingId());
            throw new MojoExecutionException(exMsg);
        }

        if (!finalDeployment.isComplete()) {
            String msg = MessageFormat.format("Deployment did not finish (in time). TrackingId [{0,number,#}] State [{1}]", finalDeployment.getTrackingId(), finalDeployment.getState());
            logError(msg);
            throw new MojoExecutionException(msg);
        }
    }


    void logInfo(String pattern, Object ... arguments) {
        getLog().info(formatMessage(pattern, arguments));
    }

    void logInfoForTeamCity(String pattern, Object... arguments) {
        if (writeTeamCityParameters) {
            logInfo(pattern, arguments);
        }
    }

    void logWarn(String pattern, Object ... arguments) {
        getLog().warn(formatMessage(pattern, arguments));
    }

    void logError(String pattern, Object ... arguments) {
        getLog().error(formatMessage(pattern, arguments));
    }

    String formatMessage(String pattern, Object... arguments) {
        return MessageFormat.format(pattern, arguments);
    }

    protected LiimaClient getClient() {
        if (client == null) {
            LiimaConfiguration amwConf = new LiimaConfiguration(url);
            client = new LiimaClient(amwConf);
            client.setMaxPollingDurationInMs(this.maxPollingDurationInMs);
            client.setPollingIntervalInMs(this.pollingIntervalInMs);
        }

        return client;
    }

    void setClient(LiimaClient client) {
        this.client = client;
    }
}
