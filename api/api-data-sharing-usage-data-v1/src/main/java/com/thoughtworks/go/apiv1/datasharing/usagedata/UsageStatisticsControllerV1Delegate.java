/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.apiv1.datasharing.usagedata;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.datasharing.usagedata.representers.UsageStatisticsRepresenter;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.server.service.datasharing.DataSharingUsageDataService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.RSAEncryptionHelper;
import com.thoughtworks.go.spark.Routes.DataSharing;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import java.io.File;
import java.util.Map;

import static spark.Spark.*;

public class UsageStatisticsControllerV1Delegate extends ApiController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private DataSharingUsageDataService dataSharingUsageDataService;
    private SystemEnvironment systemEnvironment;
    private String SIGNATURE_KEY = "signature";
    private String SUBORDINATE_PUBLIC_KEY = "subordinate_public_key";

    public UsageStatisticsControllerV1Delegate(ApiAuthenticationHelper apiAuthenticationHelper, DataSharingUsageDataService dataSharingUsageDataService, SystemEnvironment systemEnvironment) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.dataSharingUsageDataService = dataSharingUsageDataService;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String controllerBasePath() {
        return DataSharing.USAGE_DATA_PATH;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/encrypted", this::setEncryptedContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/encrypted", mimeType, apiAuthenticationHelper::checkUserAnd403);

            get("", this::getUsageStatistics);
            post("/encrypted", this::getEncryptedUsageStatistics);
        });
    }

    public String getUsageStatistics(Request request, Response response) {
        UsageStatistics usageStatistics = dataSharingUsageDataService.get();
        return jsonizeAsTopLevelObject(request, writer -> UsageStatisticsRepresenter.toJSON(writer, usageStatistics));
    }

    public String getEncryptedUsageStatistics(Request request, Response response) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        Map<String, Object> body = readRequestBodyAsJSON(request);
        String signature = (String) body.get(SIGNATURE_KEY);
        String publicKey = (String) body.get(SUBORDINATE_PUBLIC_KEY);

        boolean isVerified = verifySignatureAndPublicKey(signature, publicKey, result);

        if (isVerified) {
            return RSAEncryptionHelper.encrypt(getUsageStatistics(request, response), publicKey);
        }

        return renderHTTPOperationResult(result, request, response);
    }

    private boolean verifySignatureAndPublicKey(String signature, String subordinatePublicKey, HttpLocalizedOperationResult result) throws Exception {
        if (StringUtils.isEmpty(signature)) {
            result.unprocessableEntity(String.format("Please provide '%s' field.", SIGNATURE_KEY));
            return false;
        }

        if (StringUtils.isEmpty(subordinatePublicKey)) {
            result.unprocessableEntity(String.format("Please provide '%s' field.", SUBORDINATE_PUBLIC_KEY));
            return false;
        }

        String masterPublicKey = FileUtils.readFileToString(new File(systemEnvironment.getUpdateServerPublicKeyPath()), "utf8");

        boolean isVerified;
        try {
            isVerified = RSAEncryptionHelper.verifySignature(subordinatePublicKey, signature, masterPublicKey);
        } catch (Exception e) {
            isVerified = false;
        }

        if (!isVerified) {
            result.unprocessableEntity(String.format("Invalid '%s' or '%s' provided.", SIGNATURE_KEY, SUBORDINATE_PUBLIC_KEY));
            return false;
        }

        return true;
    }
}
