/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fua;

import org.springframework.stereotype.Component;

/**
 * Contains module's config.
 */
@Component("fua.FuaConfig")
public class FuaConfig {
	
	public final static String MODULE_PRIVILEGE = "Fua Privilege";
	
	public final static String READ_FUA_PRIVILEGE = "Read Fua Privilege";
	
	public final static String MANAGE_FUA_PRIVILEGE = "Manage Fua Privilege";
	
	public final static String DELETE_FUA_PRIVILEGE = "Delete Fua Privilege";
	
	public final static String UPDATE_FUA_PRIVILEGE = "Update Fua Privilege";

	// Global Property for FUA Generator microservice URL
    public final static String FUA_GENERATOR_URL_GP = "fua.generator.url";
    
    public final static String FUA_GENERATOR_URL_DEFAULT = "http://localhost:3000";

	public final static String FUA_GENERATOR_IDENTIFIER = "fua.identifier";
}
