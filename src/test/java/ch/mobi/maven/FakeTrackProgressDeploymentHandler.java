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
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


public class FakeTrackProgressDeploymentHandler implements HttpRequestHandler {

    // always in progress

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws IOException {
        String outputFilename = "amw-tracking-deployment-progress.json";

        InputStream resourceAsStream = getClass().getResourceAsStream("/" + outputFilename);
        String jsonResponse = IOUtils.toString(resourceAsStream, Charset.defaultCharset());

        response.setEntity(new StringEntity(jsonResponse, ContentType.APPLICATION_JSON));
    }
}
