// Copyright 2020 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buildfarm.cas;

import build.bazel.remote.execution.v2.Digest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reference memory implementation of entry/directory mappings.
 *
 * <p>Memory usage for this implementation is combinatorial and is only provided as a reference for
 * requirements.
 */
class MemoryDirectoriesIndex extends DirectoriesIndex {
  private final SetMultimap<String, Digest> entryDirectories =
      MultimapBuilder.treeKeys().hashSetValues().build();
  private final Map<Digest, ImmutableList<String>> directories = new HashMap<>();

  MemoryDirectoriesIndex(Path root) {
    super(root);
  }

  @Override
  public void close() {}

  @Override
  public void start() {}

  @Override
  public synchronized Set<Digest> removeEntry(String entry) {
    return entryDirectories.removeAll(entry);
  }

  @Override
  public Iterable<String> directoryEntries(Digest directory) {
    return directories.get(directory);
  }

  @Override
  public synchronized void put(Digest directory, Iterable<String> entries) {
    directories.put(directory, ImmutableList.copyOf(entries));
    for (String entry : entries) {
      entryDirectories.put(entry, directory);
    }
  }

  @Override
  public synchronized void remove(Digest directory) {
    Iterable<String> entries = directories.remove(directory);
    if (entries == null) return;
    for (String entry : entries) {
      // safe for multiple removal
      entryDirectories.remove(entry, directory);
    }
  }
}
