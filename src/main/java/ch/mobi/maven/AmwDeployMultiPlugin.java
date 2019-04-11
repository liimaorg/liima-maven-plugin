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
import ch.mobi.liima.client.dto.DeploymentParameter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.apache.commons.collections.MapUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;


/**
 * Sends a deployment request to AMW for a single or multiple application(s) (one server, one or more ear file(s)).
 */
@Mojo(name = "deploy-multi", requiresProject = false)
public class AmwDeployMultiPlugin extends AbstractAmwDeployPlugin {

    /**
     * List of amwApplicationName=amwApplicationVersion,amwApp2=amwApp2version,...
     * <p>
     * supported separators: "," or ";" or any kind of new-line
     * <p>
     * The string is parsed using {@link Properties#load(Reader)}
     */
    @Parameter(property = "amwApplicationProperties")
    String applicationProperties;

    /**
     * List of amwDeploymentParameterKey=amwDeploymentParameterValue,key2=value2,...
     * <p>
     * supported separators: "," or new-line
     * <p>
     * The string is parsed using {@link Properties#load(Reader)}
     */
    @Parameter(property = "amwDeploymentProperties")
    String deploymentProperties;

    @Parameter(property = "amwDeploySameVersion", defaultValue = "true")
    boolean deploySameVersion = true;

    /**
     * @return a map of the string that consists of key=value using separators: "," or new-lines.
     *         if an empty or blank string is passed in, an empty map will be returned.
     */
    Map<String, String> parsePropertiesParameter(String properties) throws MojoFailureException {
        if (isBlank(properties)) {
            return new HashMap<>();
        }

        getLog().debug("Trying to parse properties: " + properties);
        String propsPerLine = properties.replaceAll("(,|\\\\n)", "\n");

        Properties props = new Properties();
        try(Reader appReader = new StringReader(propsPerLine)) {
            props.load(appReader);

        } catch (IOException e) {
            getLog().error("Could not parse properties, cannot continue.");
            throw new MojoFailureException("Invalid properties string provided.", e);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> appsMap = new HashMap<>((Map) props);

        if (getLog().isDebugEnabled()) {
            getLog().debug("parsed the following applications:");
            for (String key : appsMap.keySet()) {
                String val = appsMap.get(key);
                getLog().debug("  " + key + " = " + val);
            }
        }

        return appsMap;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // create a map of amw applications with key the application name in amw and value the version (the version may be null)
        if (isBlank(this.applicationProperties)) {
            throw new MojoFailureException("The AMW applications list must not be empty.");
        }
        Map<String, String> amwApps = parsePropertiesParameter(this.applicationProperties);

        // create a map of key, values used for the deploymentParameters
        Map<String, String> deploymentParamsMap = parsePropertiesParameter(this.deploymentProperties);

        Map<String, String> sourceAmwApps = new HashMap<>();
        if(isNotBlank(sourceEnv)) {
            logInfo("Will query Liima for current version on sourceEnv={0}", sourceEnv);
            Deployment sourceDeployment = getDeploymentFromAmw(serverName, sourceEnv);
            if (sourceDeployment != null && sourceDeployment.isSuccessful()) {
                List<AppWithVersion> appsWithVersion = sourceDeployment.getAppsWithVersion();
                for (AppWithVersion appWithVersion : appsWithVersion) {
                    sourceAmwApps.put(appWithVersion.getApplicationName(), appWithVersion.getVersion());
                }
                logInfo("Found successful deployment on environment [{0}] with [{1}] applications.", sourceEnv, sourceAmwApps.size());

                // if source deployment is not null and successful preserve the parameters / properties that match the prefix:
                if (this.preservePropertiesPrefix != null) {
                    List<DeploymentParameter> deploymentParameters = sourceDeployment.getDeploymentParameters();
                    for (DeploymentParameter deploymentParameter : deploymentParameters) {
                        if (deploymentParameter.getKey().startsWith(this.preservePropertiesPrefix)) {
                            deploymentParamsMap.put(deploymentParameter.getKey(), deploymentParameter.getValue());
                            logInfo("Preserved deployment property {0}={1}", deploymentParameter.getKey(), deploymentParameter.getValue());
                        }
                    }
                }

            } else {
                logWarn("Last deployment on environment [{0}] was not successful! Ignoring source version.", sourceEnv);
            }

            // if both version is set and a source version is determined, and they are the same, but the deployment failed: skip promote!
            if (amwApps.equals(sourceAmwApps) && sourceDeployment != null && !sourceDeployment.isSuccessful()) {
                logError("Cannot promote failed deployment on [{0}] to [{1}]!", sourceEnv, targetEnv);
                throw new MojoFailureException("A failed deployment cannot be promoted.");
            }
        }

        getLog().info("Query current version on target environment: " + targetEnv);
        Map<String, String> currentAppToVersionOnTargetMap = new HashMap<>();
        Deployment currentDeploymentOnTarget = getDeploymentFromAmw(serverName, targetEnv);
        boolean deploymentOnTargetSuccessful = false;
        if (currentDeploymentOnTarget != null) {
            for (AppWithVersion appWithVersion : currentDeploymentOnTarget.getAppsWithVersion()) {
                currentAppToVersionOnTargetMap.put(appWithVersion.getApplicationName(), appWithVersion.getVersion());
            }

            deploymentOnTargetSuccessful = currentDeploymentOnTarget.isSuccessful();
        }

        Map<String, String> targetAmwAppsToDeploy = determineVersions(amwApps, sourceAmwApps);
        checkNoSnapshotVersions(targetAmwAppsToDeploy, targetEnv);

        String sourceVersions = Arrays.toString(targetAmwAppsToDeploy.entrySet().toArray());
        logInfo("Current versions on source: {0}", sourceVersions);
        String targetVersions = Arrays.toString(currentAppToVersionOnTargetMap.entrySet().toArray());
        logInfo("Current versions on target: {0}", targetVersions);

        if (!"local".equalsIgnoreCase(targetEnv) && currentAppToVersionOnTargetMap.equals(targetAmwAppsToDeploy)) {
            logInfo("Source version and Target version are equal.");
            logInfo("Deployment on target successful: {0}", deploymentOnTargetSuccessful);

            if(!this.deploySameVersion && deploymentOnTargetSuccessful) {
                logInfo("NOTE: deploySameVersion is disabled. Not creating deployment request.");
                logInfoForTeamCity("##teamcity[buildStatus text=''No deployment request created.'']");
                logInfoForTeamCity("##teamcity[buildNumber ''nop => ({0})'']", targetEnv);
                return;
            } else {
                logInfo("NOTE: source and target versions are the same but deploySameVersion is enabled. Will re-deploy.");
            }
        }

        String sourceVersionString = join(targetAmwAppsToDeploy.values().toArray(), ", ");
        String sourceEnvString = isNotBlank(sourceEnv) ? " (" + sourceEnv + ")" : "";
        logInfoForTeamCity("##teamcity[buildNumber ''{0}{1} => ({2})'']", sourceVersionString, sourceEnvString, targetEnv);

        //
        // create deploy request:
        //
        Deployment deployment = new Deployment(serverName, release, targetEnv);
        for (String key : targetAmwAppsToDeploy.keySet()) {
            AppWithVersion appWithVersion = new AppWithVersion(key, targetAmwAppsToDeploy.get(key));
            deployment.addAppWithVersion(appWithVersion);
        }
        if (isNotEmpty(deploymentParamsMap)) {
            for (Map.Entry<String, String> paramEntry : deploymentParamsMap.entrySet()) {
                deployment.addDeploymentParameter(paramEntry.getKey(), paramEntry.getValue());
            }
        }

        if (isNotBlank(deploymentTime)) {
            LocalTime localTime = LocalTime.parse(deploymentTime, DateTimeFormatter.ISO_LOCAL_TIME);
            LocalDateTime localDateTime = LocalDateTime.now();
            LocalDateTime deploymentDateTime = localDateTime.with(localTime);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.YY HH:mm");
            String timeForTeamCity = formatter.format(deploymentDateTime);

            // Date d = Date.from(deploymentDateTime.atZone(ZoneId.systemDefault()).toInstant());
            long deploymentMillis = deploymentDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            deployment.setDeploymentDate(deploymentMillis);

            logInfoForTeamCity("##teamcity[buildStatus text=''Deployment scheduled for {0}.'']", timeForTeamCity);
        }

        Deployment requestedDeployment = getClient().sendNewDeployment(deployment);

        String trackingUrl = getClient().createDeploymentsWebUri(String.valueOf(requestedDeployment.getTrackingId()));

        logInfo("Will track deployment with id [{0,number,#}] and trackingId [{1,number,#}]", requestedDeployment.getId(), requestedDeployment.getTrackingId());
        logInfo("See: {0}", trackingUrl);
        Deployment finalDeployment = getClient().trackDeploymentRequest(requestedDeployment);

        checkForDeploymentState(finalDeployment);

        logInfo("See: {0}", trackingUrl);
    }

    void checkNoSnapshotVersions(Map<String, String> targetAmwAppsToDeploy, String targetEnv) throws MojoExecutionException {
        for (String appKey : targetAmwAppsToDeploy.keySet()) {
            checkNoSnapshotVersion(targetAmwAppsToDeploy.get(appKey), targetEnv);
        }
    }

    Map<String, String> determineVersions(Map<String, String> amwApps, Map<String, String> sourceAmwApps) throws MojoExecutionException {
        // using amwApps as master for amw application  / version
        // not sure what to do if the maps have not the same size, would be weird

        getLog().debug("sourceAmwApps: " + sourceAmwApps);
        getLog().debug("amwApps: " + amwApps);

        Map<String, String> determinedVersions = new HashMap<>();
        for (String key : amwApps.keySet()) {
            String version = amwApps.get(key);
            String sourceVersion = sourceAmwApps.get(key);

            String detectedVersion = determineVersion(version, sourceVersion);
            determinedVersions.put(key, detectedVersion);
        }

        return determinedVersions;
    }

}
