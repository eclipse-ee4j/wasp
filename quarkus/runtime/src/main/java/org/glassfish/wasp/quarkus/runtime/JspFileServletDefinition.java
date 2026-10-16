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
package org.glassfish.wasp.quarkus.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Servlet declared in web.xml with a jsp-file instead of a servlet-class. Quarkus doesn't register these, so the
 * extension does, with everything the declaration contains.
 */
public class JspFileServletDefinition {

    private String name;
    private String jspFile;
    private List<String> mappings = new ArrayList<>();
    private Map<String, String> initParams = new LinkedHashMap<>();
    private int loadOnStartup = -1;

    /** The security-role-refs: role name to role link */
    private Map<String, String> securityRoleRefs = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJspFile() {
        return jspFile;
    }

    public void setJspFile(String jspFile) {
        this.jspFile = jspFile;
    }

    public List<String> getMappings() {
        return mappings;
    }

    public void setMappings(List<String> mappings) {
        this.mappings = mappings;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

    public void setInitParams(Map<String, String> initParams) {
        this.initParams = initParams;
    }

    /**
     * @return the load-on-startup order, or -1 if not loaded on startup
     */
    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    public Map<String, String> getSecurityRoleRefs() {
        return securityRoleRefs;
    }

    public void setSecurityRoleRefs(Map<String, String> securityRoleRefs) {
        this.securityRoleRefs = securityRoleRefs;
    }
}
