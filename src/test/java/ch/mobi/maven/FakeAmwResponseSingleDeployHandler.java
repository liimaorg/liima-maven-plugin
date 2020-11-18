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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class FakeAmwResponseSingleDeployHandler implements HttpRequestHandler {

    private final String deploymentsFilename;
    private final String deployRequestResponseFilename;

    private long postRequestCount = 0;
    private long getRequestCount = 0;
    private long otherRequestCount = 0;

    private long pollingRequests = 0;

    private String postJson;

    public FakeAmwResponseSingleDeployHandler(String deploymentsFilename, String deployRequestResponseFilename) {
        this.deploymentsFilename = deploymentsFilename;
        this.deployRequestResponseFilename = deployRequestResponseFilename;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws IOException {
        String jsonResponse = "";

        String method = request.getRequestLine().getMethod();
        switch (method) {
            case "GET":
                // GET ... Latest+deployment+job+for+App+Server+and+Env ...
                if (request.getRequestLine().getUri().contains("Latest%20deployment%20job%20for%20App%20Server%20and%20Env")) {
                    InputStream deploymentsAsStream = getClass().getResourceAsStream("/" + this.deploymentsFilename);
                    jsonResponse = IOUtils.toString(deploymentsAsStream, Charset.defaultCharset());
                } else {
                    InputStream progressAsStream = getClass().getResourceAsStream("/" + "amw-tracking-deployment-scheduled.json");
                    if (pollingRequests >= 3) {
                        progressAsStream = getClass().getResourceAsStream("/" + "amw-tracking-deployment-success.json");
                    }
                    jsonResponse = IOUtils.toString(progressAsStream, "UTF-8");
                    pollingRequests++;
                }

                this.getRequestCount++;
                break;
            case "POST":
                BasicHttpEntityEnclosingRequest entityReq = (BasicHttpEntityEnclosingRequest) request;
                postJson = EntityUtils.toString(entityReq.getEntity());
                InputStream deploymentAsStream = getClass().getResourceAsStream("/" + this.deployRequestResponseFilename);
                jsonResponse = IOUtils.toString(deploymentAsStream, Charset.defaultCharset());
                this.postRequestCount++;
                break;
            default:
                this.otherRequestCount++;
        }

        response.setEntity(new StringEntity(jsonResponse, ContentType.APPLICATION_JSON));
    }

    public long getPostRequestCount() {
        return postRequestCount;
    }

    public long getGetRequestCount() {
        return getRequestCount;
    }

    public long getOtherRequestCount() {
        return otherRequestCount;
    }

    public String getPostJson() {
        return postJson;
    }
}
