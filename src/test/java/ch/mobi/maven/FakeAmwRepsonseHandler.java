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

public class FakeAmwRepsonseHandler implements HttpRequestHandler {

    private final String filename;

    private long postRequestCount = 0;
    private long getRequestCount = 0;
    private long otherRequestCount = 0;

    public FakeAmwRepsonseHandler(String filename) {
        this.filename = filename;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/" + this.filename);
        String jsonResponse = IOUtils.toString(resourceAsStream, Charset.defaultCharset());

        String method = request.getRequestLine().getMethod();
        switch (method) {
            case "GET":
                this.getRequestCount++;
                break;
            case "POST":
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
}
