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

import org.apache.http.HttpHost;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

/**
 * Base class to create tests that need a local http server to mock an AMW instance
 *
 * see test examples:
 * <ul>
 *     <li><a href="https://hc.apache.org/httpcomponents-client-4.5.x/httpclient/xref-test/index.html">HttpClient (LocalServerTestBase)</a></li>
 *     <li><a href="https://hc.apache.org/httpcomponents-client-4.5.x/fluent-hc/xref-test/index.html">HttpClientFluent (TestFluent)</a></li>
 * </ul>
 */
public class AbstractLocalAmwTestBase {
    protected ServerBootstrap serverBootstrap;
    protected String baseURL;
    private HttpServer server;

    @Before
    public void setupLocalServer() {
        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(15000).build();
        this.serverBootstrap = ServerBootstrap.bootstrap().setSocketConfig(socketConfig);
    }

    public void startServer() throws IOException {
        this.server = serverBootstrap.create();
        this.server.start();

        HttpHost target =  new HttpHost("localhost", server.getLocalPort());
        this.baseURL = "http://localhost:" + target.getPort();
    }

    @After
    public void stopLocalServer() {
        if (server != null) {
            System.out.println("shutting down HTTP server...");
            server.stop();
//            server.shutdown(5, TimeUnit.SECONDS);
            System.out.println("HTTP server stopped.");
        }
    }
}
