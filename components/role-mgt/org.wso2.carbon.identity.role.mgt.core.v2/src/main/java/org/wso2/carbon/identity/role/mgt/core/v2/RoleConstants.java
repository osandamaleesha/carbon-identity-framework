/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.role.mgt.core.v2;

/**
 * Role constants.
 */
public class RoleConstants {

    private RoleConstants() {

    }

    public static final String DISABLED = "Disabled";

    // DB product names.
    public static final String H2 = "H2";
    public static final String MY_SQL = "MySQL";
    public static final String MARIADB = "MariaDB";
    public static final String ORACLE = "Oracle";
    public static final String MICROSOFT = "Microsoft SQL Server";
    public static final String POSTGRE_SQL = "PostgreSQL";
    public static final String DB2 = "DB2";
    public static final String INFORMIX = "INFORMIX";

    // Named query fields.
    public static final String OFFSET = "OFFSET";
    public static final String LIMIT = "LIMIT";
    public static final String ZERO_BASED_START_INDEX = "ZERO_BASED_START_INDEX";
    public static final String ONE_BASED_START_INDEX = "ONE_BASED_START_INDEX";
    public static final String END_INDEX = "END_INDEX";
    public static final String WILDCARD_CHARACTER = "*";


    // Group related constants.
    public static final String ID_URI = "urn:ietf:params:scim:schemas:core:2.0:id";

    // Administrator role name.
    public static final String ADMINISTRATOR = "Administrator";

    // Domain names
    public static final String APPLICATION_DOMAIN = "Application";
    public static final String INTERNAL_DOMAIN = "Internal";

    // Role audiences
    public static final String APPLICATION = "Application";
    public static final String ORGANIZATION = "Organization";

    /**
     * Grouping of constants related to database table names.
     */
    public static class RoleTableColumns {

        public static final String UM_ID = "UM_ID";
        public static final String UM_ROLE_NAME = "UM_ROLE_NAME";
        public static final String UM_TENANT_ID = "UM_TENANT_ID";
        public static final String NEW_UM_ROLE_NAME = "NEW_UM_ROLE_NAME";
        public static final String UM_USER_NAME = "UM_USER_NAME";
        public static final String UM_GROUP_NAME = "UM_GROUP_NAME";
        public static final String UM_DOMAIN_NAME = "UM_DOMAIN_NAME";

        public static final String TENANT_ID = "TENANT_ID";
        public static final String ATTR_NAME = "ATTR_NAME";
        public static final String ATTR_VALUE = "ATTR_VALUE";
        public static final String ROLE_NAME = "ROLE_NAME";
        public static final String AUDIENCE = "AUDIENCE";
        public static final String AUDIENCE_ID = "AUDIENCE_ID";
        public static final String AUDIENCE_REF_ID = "AUDIENCE_REF_ID";
        public static final String ROLE_ID = "ROLE_ID";
        public static final String SCOPE_NAME = "SCOPE_NAME";
        public static final String APP_ID = "APP_ID";
        public static final String SHARED_ROLE_ID = "SHARED_ROLE_ID";
        public static final String SHARED_ROLE_TENANT_ID = "SHARED_ROLE_TENANT_ID";
        public static final String MAIN_ROLE_ID = "MAIN_ROLE_ID";
        public static final String MAIN_ROLE_TENANT_ID = "MAIN_ROLE_TENANT_ID";
        public static final String NEW_ROLE_NAME = "NEW_ROLE_NAME";
        public static final String USER_NOT_FOUND_ERROR_MESSAGE = "A user doesn't exist with name: %s " +
                "in the tenantDomain: %s";

        private RoleTableColumns() {

        }
    }

    /**
     * Error message enums.
     * <p>
     * Error codes for server errors start with 75 and client errors start with 70.
     */
    public enum Error {

        INVALID_REQUEST("60001"),
        INVALID_OFFSET("60002"),
        INVALID_LIMIT("60003"),
        INVALID_FILTER("60004"),
        INVALID_SORT_BY("60005"),
        INVALID_SORT_ORDER("60006"),
        ROLE_NOT_FOUND("60007"),
        ROLE_ALREADY_EXISTS("60008"),
        OPERATION_FORBIDDEN("60009"),
        OPERATION_NOT_SUPPORTED("60010"),

        // Error thrown by custom event handler.
        ERROR_CODE_CUSTOM_EVENT_HANDLER_ERROR("55001"),

        UNEXPECTED_SERVER_ERROR("65001"),
        SORTING_NOT_IMPLEMENTED("65002");

        private final String code;
        private static final String ROLE_MANAGEMENT_ERROR_CODE_PREFIX = "RMA-";

        Error(String code) {

            this.code = code;
        }

        public String getCode() {

            return ROLE_MANAGEMENT_ERROR_CODE_PREFIX + code;
        }
    }
}
