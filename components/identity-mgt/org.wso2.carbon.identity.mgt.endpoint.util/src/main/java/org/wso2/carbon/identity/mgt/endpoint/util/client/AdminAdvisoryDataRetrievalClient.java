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

package org.wso2.carbon.identity.mgt.endpoint.util.client;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil;

import java.io.IOException;

/**
 * Client to interact with the Admin Advisory Management API.
 */
public class AdminAdvisoryDataRetrievalClient {

    private static final Log LOG = LogFactory.getLog(AdminAdvisoryDataRetrievalClient.class);
    private static final String ADMIN_BANNER_API_RELATIVE_PATH = "/api/server/v1/admin-advisory-management/banner";
    private static final String DEFAULT_BANNER_CONTENT = "Warning - unauthorized use of this tool is strictly " +
            "prohibited. All activities performed using this tool are logged and monitored.";
    private static final String ENABLE_BANNER = "enableBanner";
    private static final String BANNER_CONTENT= "bannerContent";

    /**
     * Check for admin advisory banner configs in the given tenant.
     *
     * @param tenant Tenant Domain.
     * @return JSON Object containing admin advisory banner configs.
     * @throws AdminAdvisoryDataRetrievalClientException Error while retrieving the admin advisory banner configs.
     */
    public JSONObject getAdminAdvisoryBannerData(String tenant) throws AdminAdvisoryDataRetrievalClientException {

        try {
            String uri = getAdminAdvisoryBannerEndpoint(tenant);
            HttpGet request = new HttpGet(uri);

            String responseString = IdentityManagementEndpointUtil.getHttpClientResponseString(request);

            if (!StringUtils.isEmpty(responseString)) {
                return new JSONObject(new JSONTokener(responseString));
            }
            JSONObject defaultBanner = new JSONObject();
            defaultBanner.put(ENABLE_BANNER, false);
            defaultBanner.put(BANNER_CONTENT, DEFAULT_BANNER_CONTENT);
            return defaultBanner;
        } catch (IOException e) {
            String msg = "Error while getting admin advisory banner preference for tenant : " + tenant;
            LOG.debug(msg, e);
            throw new AdminAdvisoryDataRetrievalClientException(msg, e);
        }
    }

    /**
     * Get the tenant admin advisory banner config endpoint.
     *
     * @param tenantDomain Tenant Domain.
     * @return Tenant qualified endpoint.
     * @throws AdminAdvisoryDataRetrievalClientException Error while getting the endpoint.
     */
    private String getAdminAdvisoryBannerEndpoint(String tenantDomain)
            throws AdminAdvisoryDataRetrievalClientException {

        return getEndpoint(tenantDomain);
    }

    /**
     * Resolve the tenant admin advisory banner config endpoint.
     *
     * @param tenantDomain Tenant Domain.
     * @return Resolved endpoint.
     * @throws AdminAdvisoryDataRetrievalClientException Error while getting the base path.
     */
    private String getEndpoint(String tenantDomain) throws AdminAdvisoryDataRetrievalClientException {

        try {
            return IdentityManagementEndpointUtil.getBasePath(tenantDomain, ADMIN_BANNER_API_RELATIVE_PATH);
        } catch (ApiException e) {
            throw new AdminAdvisoryDataRetrievalClientException("Error while building url for context: "
                    + ADMIN_BANNER_API_RELATIVE_PATH);
        }
    }
}
