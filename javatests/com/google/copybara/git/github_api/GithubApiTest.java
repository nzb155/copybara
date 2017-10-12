/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara.git.github_api;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.copybara.git.GitRepository.newBareRepo;
import static com.google.copybara.testing.git.GitTestUtil.getGitEnv;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.copybara.git.GitRepository;
import com.google.copybara.git.github_api.testing.AbstractGithubApiTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GithubApiTest extends AbstractGithubApiTest {

  private MockHttpTransport httpTransport;

  private Map<String, byte[]> requestToResponse;
  private Map<String, Predicate<String>> requestValidators;
  private Path credentialsFile;

  @Override
  public GitHubApiTransport getTransport() throws Exception {
    credentialsFile = Files.createTempFile("credentials", "test");
    Files.write(credentialsFile, "https://user:SECRET@github.com".getBytes(UTF_8));
    GitRepository repo = newBareRepo(Files.createTempDirectory("test_repo"),
        getGitEnv(), /*verbose=*/true)
        .init()
        .withCredentialHelper("store --file=" + credentialsFile);

    requestToResponse = new HashMap<>();
    requestValidators = new HashMap<>();
    httpTransport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        String requestString = method + " " + url;
        MockLowLevelHttpRequest request = new MockLowLevelHttpRequest() {
          @Override
          public LowLevelHttpResponse execute() throws IOException {
            assertWithMessage("Request content did not match expected values.")
                .that(requestValidators.get(method + " " + url).test(getContentAsString()))
                .isTrue();
            return super.execute();
          }
        };
        byte[] content = requestToResponse.get(requestString);
        assertWithMessage("'" + method + " " + url + "'").that(content).isNotNull();
        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
        response.setContent(content);
        request.setResponse(response);
        return request;
      }
    };
    return new GitHubApiTransportImpl(repo, httpTransport, "some_storage_file");
  }

  @Override
  public void trainMockGet(String apiPath, byte[] response) throws Exception {
    String path = String.format("GET https://api.github.com%s", apiPath);
    requestToResponse.put(path, response);
    requestValidators.put(path, (r) -> true);
  }

  @Test
  public void getWithoutCredentials() throws Exception {
    Files.delete(credentialsFile);
    testGetPull();
  }

  @Override
  public void trainMockPost(String apiPath, Predicate<String> requestValidator, byte[] response)
      throws Exception {
    String path = String.format("POST https://api.github.com%s", apiPath);
    requestToResponse.put(path, response);
    requestValidators.put(path, requestValidator);
  }
}