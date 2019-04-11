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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.Permission;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ExpectedSystemExit implements TestRule {
    public static ExpectedSystemExit none() {
        return new ExpectedSystemExit();
    }

    private boolean expectExit = false;
    private Integer expectedStatus = null;

    private ExpectedSystemExit() {
    }

    public void expectSystemExitWithStatus(int status) {
        expectSystemExit();
        expectedStatus = status;
    }

    public void expectSystemExit() {
        expectExit = true;
    }

    public Statement apply(final Statement base, Description description) {
        ProvideSecurityManager provideNoExitSecurityManager = new ProvideSecurityManager(new NoExitSecurityManager());
        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                    handleMissingSystemExit();
                } catch (TryToExitException e) {
                    handleSystemExit(e);
                }
            }
        };
        return provideNoExitSecurityManager.apply(statement, description);
    }

    private void handleMissingSystemExit() {
        if (expectExit)
            fail("System.exit has not been called.");
    }

    private void handleSystemExit(TryToExitException e) {
        if (!expectExit)
            fail("Unexpected call of System.exit(" + e.status + ").");
        else if (expectedStatus != null)
            assertEquals("Wrong exit status", expectedStatus, e.status);
    }

    private static class TryToExitException extends SecurityException {
        private static final long serialVersionUID = 159678654L;

        final Integer status;

        public TryToExitException(int status) {
            super("Tried to exit with status " + status + ".");
            this.status = status;
        }
    }

    private static class NoExitSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // allow anything.
        }

        @Override
        public void checkExit(int status) {
            throw new TryToExitException(status);
        }
    }
}
