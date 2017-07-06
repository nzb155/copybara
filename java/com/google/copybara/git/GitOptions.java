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

package com.google.copybara.git;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.copybara.GeneralOptions;
import com.google.copybara.Option;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Common arguments for {@link GitDestination}, {@link GitOrigin}, and other Git components.
 */
@Parameters(separators = "=")
public final class GitOptions implements Option {

  private final Supplier<GeneralOptions> generalOptionsSupplier;

  // Not used by git.destination but it will be at some point to make fetches more efficient.
  @Parameter(names = "--git-repo-storage",
      description = "Location of the storage path for git repositories. DEPRECATED",
      // TODO(malcon): Deprecate this flag
      hidden = true)
  String repoStorage;

  public GitOptions(Supplier<GeneralOptions> generalOptionsSupplier) {
    this.generalOptionsSupplier = Preconditions.checkNotNull(generalOptionsSupplier);
  }

  Path getRepoStorage() throws IOException {
    if (repoStorage == null) {
      return generalOptionsSupplier.get().getDirFactory().getCacheDir("git_repos");
    }
    return Paths.get(repoStorage);
  }
}
