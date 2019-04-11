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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.plugin.logging.Log;

class StringBufferedLogger implements Log {

        private final StringBuilder sb;

        public StringBufferedLogger(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public void debug(CharSequence content) {
            info(content);
        }

        @Override
        public void debug(CharSequence content, Throwable error) {
            info(content, error);
        }

        @Override
        public void debug(Throwable error) {
            info(error);
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(CharSequence content) {
            sb.append(content).append(System.lineSeparator());
        }

        @Override
        public void info(CharSequence content, Throwable error) {
            sb.append(content).append(System.lineSeparator());
            sb.append(ExceptionUtils.getStackTrace(error));
        }

        @Override
        public void info(Throwable error) {
            sb.append(ExceptionUtils.getStackTrace(error));
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(CharSequence content) {
            info(content);
        }

        @Override
        public void warn(CharSequence content, Throwable error) {
            info(content, error);
        }

        @Override
        public void warn(Throwable error) {
            info(error);
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public void error(CharSequence content) {
            info(content);
        }

        @Override
        public void error(CharSequence content, Throwable error) {
            info(content, error);
        }

        @Override
        public void error(Throwable error) {
            info(error);
        }
    }
