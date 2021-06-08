/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2021 The ZAP Development Team
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
package org.zaproxy.gradle.addon.internal;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.zaproxy.gradle.addon.internal.model.GitHubRepo;
import org.zaproxy.gradle.addon.internal.model.GitHubUser;

public abstract class GitHubReleaseExtension {

    private static final String DEFAULT_USERNAME = "zapbot";
    private static final String DEFAULT_EMAIL = "12745184+zapbot@users.noreply.github.com";
    private static final String DEFAULT_ENV_AUTH_TOKEN = "ZAPBOT_TOKEN";

    private static final String DEFAULT_ENV_REPO = "GITHUB_REPOSITORY";

    private static final String DEFAULT_MARKETPLACE_REPO = "zaproxy/zap-admin";

    @Inject
    public GitHubReleaseExtension(Project project) {
        getRepo().set(new GitHubRepo(System.getenv(DEFAULT_ENV_REPO), project.getRootDir()));
        getUser()
                .set(
                        new GitHubUser(
                                DEFAULT_USERNAME,
                                DEFAULT_EMAIL,
                                System.getenv(DEFAULT_ENV_AUTH_TOKEN)));
        getMarketplaceRepo().set(new GitHubRepo(DEFAULT_MARKETPLACE_REPO, null));
    }

    public abstract Property<GitHubRepo> getRepo();

    public abstract Property<GitHubUser> getUser();

    public abstract Property<GitHubRepo> getMarketplaceRepo();
}
