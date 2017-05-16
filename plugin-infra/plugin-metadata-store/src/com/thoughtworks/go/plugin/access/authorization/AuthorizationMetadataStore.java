/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.access.common.MetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;

import java.util.HashSet;
import java.util.Set;

public class AuthorizationMetadataStore extends MetadataStore<AuthorizationPluginInfo> {
    private static final AuthorizationMetadataStore store = new AuthorizationMetadataStore();

    protected AuthorizationMetadataStore() {
    }

    public static AuthorizationMetadataStore instance() {
        return store;
    }

    public Set<String> getPluginsThatSupportsPasswordBasedAuthentication() {
        return getPluginsThatSupports(SupportedAuthType.Password);
    }

    private Set<String> getPluginsThatSupports(SupportedAuthType supportedAuthType) {
        Set<String> plugins = new HashSet<>();
        for (AuthorizationPluginInfo pluginInfo : this.pluginInfos.values()) {
            if (pluginInfo.getCapabilities().getSupportedAuthType() == supportedAuthType) {
                plugins.add(pluginInfo.getDescriptor().id());
            }
        }
        return plugins;
    }

    public Set<String> getPluginsThatSupportsUserSearch() {
        Set<String> plugins = new HashSet<>();
        for (AuthorizationPluginInfo pluginInfo : this.pluginInfos.values()) {
            if (pluginInfo.getCapabilities().canSearch()) {
                plugins.add(pluginInfo.getDescriptor().id());
            }
        }
        return plugins;
    }

    public Set<String> getPluginsThatSupportsWebBasedAuthentication() {
        return getPluginsThatSupports(SupportedAuthType.Web);
    }
}
