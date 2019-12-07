/*
Copyright 2018 Google LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.google.blocks.ftcrobotcontroller.util;

class OfflineBlocksProject {
  final String fileName;
  final String content;
  final String name;
  final long dateModifiedMillis;
  final boolean enabled;

  OfflineBlocksProject(String fileName, String content, String name,
      long dateModifiedMillis, boolean enabled) {
    this.fileName = fileName;
    this.content = content.replace("\n", " ").replaceAll("\\> +\\<", "><");
    this.name = name;
    this.dateModifiedMillis = dateModifiedMillis;
    this.enabled = enabled;
  }

  // java.lang.Object methods

  @Override
  public boolean equals(Object o) {
    if (o instanceof OfflineBlocksProject) {
      OfflineBlocksProject that = (OfflineBlocksProject) o;
      return this.fileName.equals(that.fileName)
          && this.content.equals(that.content)
          && this.name.equals(that.name)
          && this.dateModifiedMillis == that.dateModifiedMillis
          && this.enabled == that.enabled;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return fileName.hashCode()
        + content.hashCode()
        + name.hashCode();
  }
}
