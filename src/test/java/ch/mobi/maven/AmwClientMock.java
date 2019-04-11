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
import org.jetbrains.annotations.NotNull;

public class AmwClientMock extends LiimaClient {

    private Deployment newDeployment;

    public AmwClientMock() {
        super(null);
    }

    @Override
    public Deployment retrieveDeployment(@NotNull String amwServerName, @NotNull String environment) {
        Deployment sourceDeployment = new Deployment();
        sourceDeployment.setState("success");
        sourceDeployment.setAppServerName(amwServerName);
        sourceDeployment.setEnvironmentName(environment);
        sourceDeployment.addAppWithVersion(new AppWithVersion("appName1", "0.0.0"));
        sourceDeployment.addDeploymentParameter("dep.paramName1.version", "1");
        sourceDeployment.addDeploymentParameter("dep.paramName2.version", "2");
        sourceDeployment.addDeploymentParameter("dep.paramName3.version", "3");

        return sourceDeployment;
    }

    @Override
    public Deployment sendNewDeployment(Deployment deployment) {
        this.newDeployment = deployment;

        return deployment;
    }

    @Override
    public Deployment trackDeploymentRequest(Deployment requestedDeployment) {
        Deployment finalDeployment = new Deployment();
        finalDeployment.setState("success");

        return finalDeployment;
    }

    Deployment getNewDeployment() {
        return newDeployment;
    }

    @Override
    public String createDeploymentsWebUri(String trackingId) {
        return "htttp://liima/AMW_angular/#/filter?filters=";
    }
}
