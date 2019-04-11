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

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

public class AbstractAmwDeployPluginTest {

    @Test
    public void formatMessage() {
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        Map<String, String> targetAmwAppsToDeploy = new LinkedHashMap<>();
        targetAmwAppsToDeploy.put("app1_mod1", "1.0.0");
        targetAmwAppsToDeploy.put("app1_mod2", "1.2.0");

        String sourceVersions = StringUtils.join(targetAmwAppsToDeploy.values().toArray(), ", ");

        String msg = plugin.formatMessage("##teamcity[buildNumber ''{0} => {1}'']", sourceVersions, "P");

        assertThat(msg, containsString("1.0.0"));

    }

    @Test
    public void formatMessageWithEmptyMap() {
        AmwDeployMultiPlugin plugin = new AmwDeployMultiPlugin();

        Map<String, String> targetAmwAppsToDeploy = new LinkedHashMap<>();

        String sourceVersions = StringUtils.join(targetAmwAppsToDeploy.values().toArray(), ", ");

        String msg = plugin.formatMessage("##teamcity[buildNumber ''{0} => {1}'']", sourceVersions, "P");

        assertThat(msg, containsString("=> P"));

    }

}
