/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.action.execution.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.action.execution.api.model.ActionInvocationErrorResponse;
import org.wso2.carbon.identity.action.execution.api.model.ActionInvocationFailureResponse;
import org.wso2.carbon.identity.action.execution.api.model.ActionInvocationIncompleteResponse;
import org.wso2.carbon.identity.action.execution.api.model.ActionInvocationResponse;
import org.wso2.carbon.identity.action.execution.api.model.ActionInvocationSuccessResponse;
import org.wso2.carbon.identity.action.execution.api.model.ActionType;
import org.wso2.carbon.identity.action.execution.api.model.Operation;
import org.wso2.carbon.identity.action.execution.internal.service.impl.ActionInvocationResponseClassFactory;
import org.wso2.carbon.identity.action.execution.internal.util.APIClient;
import org.wso2.carbon.identity.action.execution.internal.util.ActionExecutorConfig;
import org.wso2.carbon.identity.action.execution.internal.util.AuthMethods;
import org.wso2.carbon.identity.action.management.api.model.AuthProperty;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class APIClientTest {

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    private MockedStatic<ActionExecutorConfig> actionExecutorConfigStatic;
    private MockedStatic<LoggerUtils> loggerUtils;

    @InjectMocks
    private APIClient apiClient;

    @BeforeMethod
    public void setUp() throws Exception {

        actionExecutorConfigStatic = mockStatic(ActionExecutorConfig.class);
        loggerUtils = mockStatic(LoggerUtils.class);
        loggerUtils.when(() -> LoggerUtils.isDiagnosticLogsEnabled()).thenReturn(true);
        ActionExecutorConfig actionExecutorConfig = mock(ActionExecutorConfig.class);
        actionExecutorConfigStatic.when(ActionExecutorConfig::getInstance).thenReturn(actionExecutorConfig);
        when(actionExecutorConfig.getHttpConnectionPoolSize()).thenReturn(20);
        MockitoAnnotations.openMocks(this);
        when(actionExecutorConfig.getHttpRequestRetryCount()).thenReturn(2);
        setField(apiClient, "httpClient", httpClient);
    }

    @AfterMethod
    public void tearDown() {

        actionExecutorConfigStatic.close();
        loggerUtils.close();
    }

    @Test
    public void testCallAPIUnacceptableContentTypeForSuccessResponse()
            throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        InputStreamEntity entity =
                new InputStreamEntity(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.DEFAULT_TEXT.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");
        assertNotNull(apiResponse);
        assertNull(apiResponse.getResponse());
        assertFalse(apiResponse.isRetry());
        assertTrue(apiResponse.isError());
        assertNotNull(apiResponse.getErrorLog());
        assertEquals(apiResponse.getErrorLog(),
                "Unexpected response for status code: 200. The response content type is not application/json.");
    }

    @DataProvider(name = "unacceptableSuccessResponsePayloads")
    public String[] unacceptableSuccessResponsePayloads() {

        return new String[]{"{}", "", "success", "{\"actionStatus\":\"ERROR\"}, " + "{\"actionStatus\": \"FAILED\"}"};
    }

    @Test(dataProvider = "unacceptableSuccessResponsePayloads")
    public void testCallAPIUnacceptablePayloadForSuccessResponse(String payload)
            throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        InputStreamEntity entity =
                new InputStreamEntity(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isError());
        assertFalse(apiResponse.isRetry());
        assertNotNull(apiResponse.getErrorLog());
        assertEquals(apiResponse.getErrorLog(),
                "Unexpected response for status code: 200. Reading JSON response failed.");
    }

    @Test
    public void testCallAPIAcceptablePayloadForSuccessResponse() throws Exception {

        String successResponse =
                "{\"actionStatus\":\"SUCCESS\",\"operations\":[" +
                        "{\"op\":\"add\",\"path\":\"/accessToken/claims/-\",\"" +
                        "value\":{\"name\":\"customSID\",\"value\":\"12345\"}}]}";

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(successResponse.getBytes(
                StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");

        assertNotNull(apiResponse);
        assertNotNull(apiResponse.getResponse());
        assertEquals(apiResponse.getResponse().getActionStatus(), ActionInvocationResponse.Status.SUCCESS);
        assertTrue(apiResponse.getResponse() instanceof ActionInvocationSuccessResponse);
        ((ActionInvocationSuccessResponse) apiResponse.getResponse()).getOperations().forEach(operation -> {
            assertEquals(operation.getOp(), Operation.ADD);
            assertEquals(operation.getPath(), "/accessToken/claims/-");
            assertNull(operation.getUrl());
            assertTrue(operation.getValue() instanceof HashMap);
            ((HashMap<String, String>) operation.getValue()).forEach((key, value) -> {
                if ("name".equals(key)) {
                    assertEquals(value, "customSID");
                } else if ("value".equals(key)) {
                    assertEquals(value, "12345");
                }
            });
        });
        assertFalse(apiResponse.isRetry());
        assertNull(apiResponse.getErrorLog());
    }

    @Test
    public void testCallAPIAcceptablePayloadForIncompleteResponse() throws Exception {

        String incompleteResponse =
                "{\"actionStatus\": \"INCOMPLETE\", \"operations\": [" +
                        "{\"op\": \"redirect\",\"url\": \"https://dummy-url\"}]}";

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(incompleteResponse.getBytes(
                StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");

        assertNotNull(apiResponse);
        assertNotNull(apiResponse.getResponse());
        assertTrue(apiResponse.getResponse() instanceof ActionInvocationIncompleteResponse);
        ActionInvocationIncompleteResponse response =
                ((ActionInvocationIncompleteResponse) apiResponse.getResponse());
        assertEquals(response.getActionStatus(), ActionInvocationResponse.Status.INCOMPLETE);
        response.getOperations().forEach(operation -> {
            assertEquals(operation.getOp(), Operation.REDIRECT);
            assertEquals(operation.getUrl(), "https://dummy-url");
            assertNull(operation.getPath());
            assertNull(operation.getValue());
        });
        assertFalse(apiResponse.isRetry());
        assertNull(apiResponse.getErrorLog());
    }

    @DataProvider(name = "unexpectedIncompleteResponses")
    public Object[][] unexpectedIncompleteResponses() {

        return new Object[][]{
                {"{\"operations\": [" + "{\"op\": \"redirect\",\"url\": \"https://dummy-url\"}]}"},
                {"{\"actionStatus\": \"INCOMPLETE\"}"}
        };
    }

    @Test(dataProvider = "unexpectedIncompleteResponses")
    public void testCallAPIUnexpectedIncompleteResponse(String incompleteResponse) throws IOException {

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(incompleteResponse.getBytes(
                StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isError());
        assertFalse(apiResponse.isRetry());
        assertNotNull(apiResponse.getErrorLog());
    }

    @Test
    public void testCallAPIAcceptablePayloadForFailureResponse() throws Exception {

        String failureResponse =
                "{\"actionStatus\":\"FAILED\",\"failureReason\":\"Error_reason\"," +
                        "\"failureDescription\":\"Error_description\"}";

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(failureResponse.getBytes(
                StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        AuthProperty authProperty = new AuthProperty.AuthPropertyBuilder()
                .name("accessToken")
                .value("value")
                .isConfidential(true)
                .build();
        AuthMethods.AuthMethod bearAuth = new AuthMethods.BearerAuth(Collections.singletonList(authProperty));
        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", bearAuth, "{}");

        assertNotNull(apiResponse);
        assertNotNull(apiResponse.getResponse());
        assertEquals(apiResponse.getResponse().getActionStatus(), ActionInvocationResponse.Status.FAILED);
        assertEquals(((ActionInvocationFailureResponse) apiResponse.getResponse()).getFailureReason(),
                "Error_reason");
        assertEquals(((ActionInvocationFailureResponse) apiResponse.getResponse()).getFailureDescription(),
                "Error_description");
        assertFalse(apiResponse.isRetry());
        assertNull(apiResponse.getErrorLog());
    }

    @DataProvider(name = "unexpectedFailureResponses")
    public Object[][] unexpectedFailureResponses() {

        return new Object[][]{
                {HttpStatus.SC_OK, ContentType.DEFAULT_TEXT.getMimeType(), "",
                        "Unexpected response for status code: 200. The response content type is not application/json."},
                {HttpStatus.SC_OK, ContentType.APPLICATION_JSON.getMimeType(), "{\"actionStatus\":\"FAILED\"}",
                        "Unexpected response for status code: 200. Reading JSON response failed."},
                {HttpStatus.SC_OK, ContentType.APPLICATION_JSON.getMimeType(),
                        "{\"actionStatus\":\"FAILED\", \"failedReason\": \"Scope validation failed\"}",
                        "Unexpected response for status code: 200. Reading JSON response failed."},
                {HttpStatus.SC_OK, ContentType.APPLICATION_JSON.getMimeType(),
                        "{\"actionStatus\":\"FAILED\", \"failedReason\": \"Scope validation failed\"}, " +
                                "\"failedDescription\": \"<alert();>\"}",
                        "Unexpected response for status code: 200. Reading JSON response failed."}
        };
    }

    @Test(dataProvider = "unexpectedFailureResponses")
    public void testCallAPIUnexpectedFailureResponse(Object statusCode, Object contentType, Object payload,
                                                     Object expectedErrorLog)
            throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn((int) statusCode);
        InputStreamEntity entity =
                new InputStreamEntity(new ByteArrayInputStream(payload.toString().getBytes(StandardCharsets.UTF_8)));
        entity.setContentType(contentType.toString());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isError());
        assertFalse(apiResponse.isRetry());
        assertNotNull(apiResponse.getErrorLog());
        assertEquals(apiResponse.getErrorLog(), expectedErrorLog.toString());
    }

    @DataProvider(name = "unexpectedErrorResponses")
    public Object[][] unexpectedErrorResponses() {

        return new Object[][]{
                {HttpStatus.SC_BAD_REQUEST, ContentType.DEFAULT_TEXT.getMimeType(),
                        "", "Failed to execute the action request. Received status code: 400."},
                {HttpStatus.SC_UNAUTHORIZED, ContentType.APPLICATION_JSON.getMimeType(),
                        "{}", "Unexpected error response received for the status code: 401. " +
                        "Parsing JSON response failed."},
                {HttpStatus.SC_BAD_REQUEST, ContentType.APPLICATION_JSON.getMimeType(),
                        "{\"actionStatus\":\"ERROR\"}", "Unexpected error response received for the status " +
                        "code: 400. Parsing JSON response failed."},
                {HttpStatus.SC_UNAUTHORIZED, ContentType.APPLICATION_JSON.getMimeType(),
                        "{\"actionStatus\":\"SUCCESS\"}", "Unexpected error response received for the status " +
                        "code: 401. Parsing JSON response failed."},
                {HttpStatus.SC_UNAUTHORIZED, ContentType.APPLICATION_JSON.getMimeType(),
                        "{\"actionStatus\": \"ERROR\", \"error\": \"Scope validation failed\"," +
                                " \"errorDescription\": \"<alert;>\"}",
                        "Unexpected error response received for the status code: 401. Parsing JSON response failed."},
                {HttpStatus.SC_INTERNAL_SERVER_ERROR, ContentType.APPLICATION_JSON.getMimeType(),
                        "server_error", "Unexpected error response received for the status " +
                        "code: 500. Parsing JSON response failed."},
                {HttpStatus.SC_BAD_GATEWAY, ContentType.DEFAULT_TEXT.getMimeType(),
                        "", "Failed to execute the action request. Received status code: 502."},
                {HttpStatus.SC_CONFLICT, ContentType.DEFAULT_TEXT.getMimeType(),
                        "", "Unexpected response received with status code: 409."}
        };
    }

    @Test(dataProvider = "unexpectedErrorResponses")
    public void testCallAPIUnexpectedErrorResponse(Object statusCode, Object contentType, Object payload,
                                                   Object expectedErrorLog)
            throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn((int) statusCode);
        InputStreamEntity entity =
                new InputStreamEntity(new ByteArrayInputStream(payload.toString().getBytes(StandardCharsets.UTF_8)));
        entity.setContentType(contentType.toString());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isError());
        if ((int) statusCode == 500 || (int) statusCode == 502) { // This is a retry
            assertTrue(apiResponse.isRetry());
            verify(httpClient, times(2)).execute(any(HttpPost.class));
        } else {
            assertFalse(apiResponse.isRetry());
        }
        assertNotNull(apiResponse.getErrorLog());
        assertEquals(apiResponse.getErrorLog(), expectedErrorLog.toString());
    }

    @DataProvider(name = "acceptableErrorResponsePayloads")
    public Object[][] acceptableErrorResponses() {

        return new Object[][]{
                {HttpStatus.SC_BAD_REQUEST,
                        "{\"actionStatus\":\"ERROR\",\"errorMessage\":\"invalid_request\"," +
                                "\"errorDescription\":\"client_id is missing\"}"},
                {HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        "{\"actionStatus\":\"ERROR\",\"errorMessage\":\"server_error\"," +
                                "\"errorDescription\":\"internal server error\"}"},
                {HttpStatus.SC_UNAUTHORIZED,
                        "{\"actionStatus\":\"ERROR\",\"errorMessage\":\"access_denied\"," +
                                "\"errorDescription\":\"scope validation failed\"}"},
                {HttpStatus.SC_UNAUTHORIZED,
                        "{\"actionStatus\":\"ERROR\",\"errorMessage\":\"access_denied\"," +
                                "\"errorDescription\":\"\"}"}
        };
    }

    @Test(dataProvider = "acceptableErrorResponsePayloads")
    public void testCallAPIAcceptablePayloadForErrorResponse(Object statusCode, Object payload) throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn((int) statusCode);

        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(payload.toString().getBytes(
                StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");

        assertNotNull(apiResponse);
        assertNotNull(apiResponse.getResponse());
        assertEquals(apiResponse.getResponse().getActionStatus(), ActionInvocationResponse.Status.ERROR);
        assertTrue(apiResponse.getResponse() instanceof ActionInvocationErrorResponse);
        ActionInvocationErrorResponse errorResponseObject =
                ((ActionInvocationErrorResponse) apiResponse.getResponse());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(payload.toString());
        String error = rootNode.path("errorMessage").asText();
        String errorDescription = rootNode.path("errorDescription").asText();
        assertEquals(errorResponseObject.getErrorMessage(), error);
        assertEquals(errorResponseObject.getErrorDescription(), errorDescription);
        assertNull(apiResponse.getErrorLog());
    }

    @Test
    public void testCallAPIRetryOnTimeoutAndReceiveSuccessResponse() throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenThrow(new ConnectTimeoutException("Timeout"))
                .thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);

        String successResponse =
                "{\"actionStatus\":\"SUCCESS\",\"operations\":[" +
                        "{\"op\":\"add\",\"path\":\"/accessToken/claims/-\",\"" +
                        "value\":{\"name\":\"customSID\",\"value\":\"12345\"}}]}";
        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(successResponse.getBytes(
                StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse response = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(httpClient, times(2)).execute(any(HttpPost.class));
    }

    @Test
    public void testReceiveSuccessResponseWithExtendedResponseData() throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);

        ActionInvocationResponseClassFactory.registerActionInvocationResponseClassProvider(
                new TestActionInvocationResponseClassProvider());

        String successResponse =
                "{\"actionStatus\":\"SUCCESS\", \"data\": {\"id\":\"test-123-id\"}}";
        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(successResponse.getBytes(
                StandardCharsets.UTF_8)));
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        when(httpResponse.getEntity()).thenReturn(entity);

        ActionInvocationResponse response = apiClient.callAPI(ActionType.AUTHENTICATION,
                "http://example.com", null, "{}");

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    @Test
    public void testCallAPIRetryOnTimeoutAndReachMaxRetryAttempts() throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenThrow(new ConnectTimeoutException("Connection Timeout"))
                .thenThrow(new SocketTimeoutException("Read Timeout"));

        ActionInvocationResponse response = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");

        assertNotNull(response);
        assertTrue(response.isError());
        assertEquals(response.getErrorLog(), "Failed to execute the action request or maximum retry attempts reached.");
        verify(httpClient, times(2)).execute(any(HttpPost.class));
    }

    @Test
    public void testUnexpectedExceptionFromHttpClient() throws Exception {

        when(httpClient.execute(any(HttpPost.class))).thenThrow(new ClientProtocolException("Unexpected exception"));

        ActionInvocationResponse apiResponse = apiClient.callAPI(ActionType.PRE_ISSUE_ACCESS_TOKEN,
                "http://example.com", null, "{}");
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isError());
        assertEquals(apiResponse.getErrorLog(),
                "Failed to execute the action request or maximum retry attempts reached.");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
