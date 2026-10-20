/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.wasp.tck.tags;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.derby.drda.NetworkServerControl;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Starts the database the SQL tests of the Jakarta Tags TCK use, before the tests run, and stops it after they ran.
 *
 * <p>
 * It's a Derby network server at {@code jstl.db.server}:{@code jstl.db.port}, in the JVM of the tests, with an
 * in-memory database (in {@code jstl.db.url}) that has the tables of {@code jstl-tables.sql}. The pages of the tests
 * connect to it with the Derby client driver, from the runtime they're deployed to, wherever that runs.
 */
public class JstlDatabase implements LauncherSessionListener {

    private static final String TABLES = "/jstl-tables.sql";

    private NetworkServerControl server;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        String host = System.getProperty("jstl.db.server", "localhost");
        int port = Integer.getInteger("jstl.db.port", 1527);

        // A server that's already there, such as one GlassFish started, would get the tables, but not be stopped
        try {
            new Socket(host, port).close();
            throw new IllegalStateException("Port " + port + " on " + host + " is in use, the database can't start "
                    + "there. Stop the server that uses it (asadmin stop-database for Derby in GlassFish), or set "
                    + "jstl.db.port.");
        } catch (IOException portIsFree) {
            // Expected
        }

        try {
            server = new NetworkServerControl(InetAddress.getByName(host), port);
            server.start(null);
            waitUntilStarted();
            createTables();
        } catch (Exception e) {
            throw new IllegalStateException("Could not start the database for the Jakarta Tags TCK", e);
        }
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        if (server != null) {
            try {
                server.shutdown();
            } catch (Exception e) {
                System.err.println("Could not stop the database for the Jakarta Tags TCK: " + e);
            }
        }
    }

    private void waitUntilStarted() throws Exception {
        for (int i = 0;; i++) {
            try {
                server.ping();
                return;
            } catch (Exception e) {
                if (i == 100) {
                    throw e;
                }
                Thread.sleep(100);
            }
        }
    }

    private static void createTables() throws SQLException {
        String url = System.getProperty("jstl.db.url");
        String user = System.getProperty("jstl.db.user");
        String password = System.getProperty("jstl.db.password");

        // Derby's default schema is the user's, which is where the tests look for the tables
        try (Connection connection = DriverManager.getConnection(url + ";create=true", user, password);
                Statement statement = connection.createStatement()) {
            for (String sql : readTables().split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql.strip());
                }
            }
        }
    }

    private static String readTables() {
        try (InputStream tables = JstlDatabase.class.getResourceAsStream(TABLES)) {
            return new String(tables.readAllBytes(), UTF_8).replaceAll("(?m)^--.*$", "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
