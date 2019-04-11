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

import org.junit.rules.ExternalResource;

public class ProvideSecurityManager extends ExternalResource {
      private final SecurityManager manager;
      private SecurityManager originalManager;

      public ProvideSecurityManager(SecurityManager manager) {
             this.manager = manager;
      }

      @Override
     protected void before() {
              originalManager = System.getSecurityManager();
              System.setSecurityManager(manager);
      }
      @Override
      protected void after() {
              System.setSecurityManager(originalManager);
      }
}
