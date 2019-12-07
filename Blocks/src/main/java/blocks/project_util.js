/**
 * @fileoverview Project utilities.
 * @author lizlooney@google.com (Liz Looney)
 */

/**
 * Fetches the list of projects (as json) and calls the callback.
 */
function fetchProjects(callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    fetchProjectsViaHttp(callback);
  } else if  (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    fetchProjectsViaFile(callback);
  }
}

/**
 * Fetches the list of samples (as json) and calls the callback.
 */
function fetchSamples(callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    fetchSamplesViaHttp(callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    fetchSamplesViaFile(callback);
  }
}

/**
 * Opens the project with the given name.
 */
function openProjectBlocks(projectName) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    openProjectBlocksViaHttp(projectName);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    openProjectBlocksViaFile(projectName);
  }
}

/**
 * Fetches the blocks of an existing project and calls the callback
 */
function fetchBlkFileContent(projectName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    fetchBlkFileContentViaHttp(projectName, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    fetchBlkFileContentViaFile(projectName, callback);
  }
}

function newProject(projectName, sampleName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    newProjectViaHttp(projectName, sampleName, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    newProjectViaFile(projectName, sampleName, callback);
  }
}

function saveProject(projectName, blkFileContent, jsFileContent, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    saveProjectViaHttp(projectName, blkFileContent, jsFileContent, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    saveProjectViaFile(projectName, blkFileContent, jsFileContent, callback);
  }
}

function renameProject(oldProjectName, newProjectName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    renameProjectViaHttp(oldProjectName, newProjectName, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    renameProjectViaFile(oldProjectName, newProjectName, callback);
  }
}

function copyProject(oldProjectName, newProjectName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    copyProjectViaHttp(oldProjectName, newProjectName, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    copyProjectViaFile(oldProjectName, newProjectName, callback);
  }
}

function enableProject(projectName, enable, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    enableProjectViaHttp(projectName, enable, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    enableProjectViaFile(projectName, enable, callback);
  }
}

function deleteProjects(starDelimitedProjectNames, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    deleteProjectsViaHttp(starDelimitedProjectNames, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    deleteProjectsViaFile(starDelimitedProjectNames, callback);
  }
}

function getBlocksJavaClassName(projectName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    getBlocksJavaClassNameViaHttp(projectName, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    getBlocksJavaClassNameViaFile(projectName, callback);
  }
}

function saveBlocksJava(relativeFileName, javaCode, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    saveBlocksJavaViaHttp(relativeFileName, javaCode, callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    saveBlocksJavaViaFile(relativeFileName, javaCode, callback);
  }
}

//..........................................................................
// Code used when html/js is in a browser, loaded as an http:// URL.

// The following are generated dynamically in ProgrammingModeServer.fetchJavaScriptForServer():
// URI_LIST_PROJECTS
// URI_LIST_SAMPLES
// URI_FETCH_BLK
// URI_NEW_PROJECT
// URI_SAVE_PROJECT
// URI_RENAME_PROJECT
// URI_COPY_PROJECT
// URI_ENABLE_PROJECT
// URI_DELETE_PROJECTS
// URI_GET_BLOCKS_JAVA_CLASS_NAME
// URI_SAVE_BLOCKS_JAVA
// PARAM_NAME
// PARAM_NEW_NAME
// PARAM_BLK
// PARAM_JS
// PARAM_ENABLE
// PARAM_CONTENT

function fetchProjectsViaHttp(callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', URI_LIST_PROJECTS, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var jsonProjects = xhr.responseText;
        callback(jsonProjects, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Fetch projects failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send();
}

function fetchSamplesViaHttp(callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', URI_LIST_SAMPLES, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var jsonSamples = xhr.responseText;
        callback(jsonSamples, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Fetch samples failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send();
}

function openProjectBlocksViaHttp(projectName) {
  // Go to FtcBlocks.html?project=<projectName>.
  window.location.href = 'FtcBlocks.html?project=' + encodeURIComponent(projectName);
}

function fetchBlkFileContentViaHttp(projectName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(projectName);
  xhr.open('POST', URI_FETCH_BLK, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var blkFileContent = xhr.responseText;
        callback(blkFileContent, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Fetch blocks failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function newProjectViaHttp(projectName, sampleName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(projectName) +
      '&' + PARAM_SAMPLE_NAME + '=' + encodeURIComponent(sampleName);
  xhr.open('POST', URI_NEW_PROJECT, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var blkFileContent = xhr.responseText;
        callback(blkFileContent, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'New project failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function saveProjectViaHttp(projectName, blkFileContent, jsFileContent, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(projectName) +
      '&' + PARAM_BLK + '=' + encodeURIComponent(blkFileContent) +
      '&' + PARAM_JS + '=' + encodeURIComponent(jsFileContent);
  xhr.open('POST', URI_SAVE_PROJECT, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
          callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Save project failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function renameProjectViaHttp(oldProjectName, newProjectName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(oldProjectName) +
      '&' + PARAM_NEW_NAME + '=' + encodeURIComponent(newProjectName);
  xhr.open('POST', URI_RENAME_PROJECT, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Rename project failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function copyProjectViaHttp(oldProjectName, newProjectName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(oldProjectName) +
      '&' + PARAM_NEW_NAME + '=' + encodeURIComponent(newProjectName);
  xhr.open('POST', URI_COPY_PROJECT, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Copy project failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function enableProjectViaHttp(oldProjectName, enable, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(oldProjectName) +
      '&' + PARAM_ENABLE + '=' + (enable ? "true" : "false");
  xhr.open('POST', URI_ENABLE_PROJECT, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Enable project failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function deleteProjectsViaHttp(starDelimitedProjectNames, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(starDelimitedProjectNames);
  xhr.open('POST', URI_DELETE_PROJECTS, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Delete projects failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function getBlocksJavaClassNameViaHttp(projectName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(projectName);
  xhr.open('POST', URI_GET_BLOCKS_JAVA_CLASS_NAME, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var className = xhr.responseText;
        callback(className, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Get blocks java class name failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function saveBlocksJavaViaHttp(relativeFileName, javaCode, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(relativeFileName) +
      '&' + PARAM_JAVA + '=' + encodeURIComponent(javaCode);
  xhr.open('POST', URI_SAVE_BLOCKS_JAVA, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Save Java code failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

//..........................................................................
// Code used when html/js is in a browser, loaded as an file:// URL.

var db = false;

function openOfflineDatabase(callback) {
  var openRequest = window.indexedDB.open('FtcBlocksDatabase', 1);
  openRequest.onerror = function(event) {
    callback(false, 'openRequest error');
  };
  openRequest.onupgradeneeded = function(event) {
    var db1 = event.target.result;

    // Create the object store for .blk files.
    db1.createObjectStore('blkFiles', { keyPath: 'FileName' })
        .createIndex("name", "name", { unique: true });

    // Create the object store for other files.
    db1.createObjectStore('otherFiles', { keyPath: 'FileName' });

    event.target.transaction.oncomplete = function(event) {
      // Fill the blkFiles object store with projects.
      var blkFilesObjectStore = db1.transaction(['blkFiles'], 'readwrite')
          .objectStore('blkFiles');
      var blkFiles = getBlkFiles();
      for (var i = 0; i < blkFiles.length; i++) {
        var blkFile = blkFiles[i];
        blkFilesObjectStore.add(blkFile);
      }

      // Add a row to the otherFiles object store for the clipboard content.
      var otherFilesObjectStore = db1.transaction(['otherFiles'], 'readwrite')
          .objectStore('otherFiles');
      var value = Object.create(null);
      value['FileName'] = 'clipboard.xml';
      value['Content'] = '';
      otherFilesObjectStore.add(value);
    };
  };
  openRequest.onsuccess = function(event) {
    db = event.target.result;
    callback(true, '');
  };
}

function fetchProjectsViaFile(callback) {
  if (!db) {
    openOfflineDatabase(function(success, errorReason) {
      if (success) {
        fetchProjectsViaFile(callback);
      } else {
        callback(null, 'Fetch projects failed. (' + errorReason + ')');
      }
    });
    return;
  }
  var jsonProjects = '[';
  var delimiter = '';
  var openCursorRequest = db.transaction(['blkFiles'], 'readonly')
      .objectStore('blkFiles')
      .openCursor();
  openCursorRequest.onerror = function(event) {
    callback(null, 'Fetch projects failed. Could not open cursor.');
  };
  openCursorRequest.onsuccess = function(event) {
    var cursor = event.target.result;
    if (cursor) {
      var value = cursor.value;
      jsonProjects += delimiter + '{' +
          '"name":"' + value['name'] + '", ' +
          '"escapedName":"' + value['escapedName'] + '", ' +
          '"dateModifiedMillis":' + value['dateModifiedMillis'] + ', ' +
          '"enabled":' + value['enabled'] +
          '}';
      cursor.continue();
      delimiter = ',';
    } else {
      jsonProjects += ']';
      callback(jsonProjects, '');
    }
  };
}

function fetchSamplesViaFile(callback) {
  var jsonSamples = getSampleNamesJson();
  setTimeout(function() {
    callback(jsonSamples, '');
  }, 0);
}

function openProjectBlocksViaFile(projectName) {
  // Go to FtcBlocks.html?project=<projectName>.
  window.location.href = 'FtcOfflineBlocks.html?project=' + encodeURIComponent(projectName);
}

function fetchBlkFileContentViaFile(projectName, callback) {
  if (!db) {
    openOfflineDatabase(function(success, errorReason) {
      if (success) {
        fetchBlkFileContentViaFile(projectName, callback);
      } else {
        callback(null, 'Fetch blocks failed. (' + errorReason + ')');
      }
    });
    return;
  }
  var getRequest = db.transaction(['blkFiles'], 'readonly')
      .objectStore('blkFiles').index("name").get(projectName);
  getRequest.onerror = function(event) {
    callback(null, 'Fetch blocks failed. (getRequest error)');
  };
  getRequest.onsuccess = function(event) {
    if (event.target.result === undefined) {
      callback(null, 'Fetch blocks failed. (not found)');
      return;
    }
    var value = event.target.result;
    callback(value['Content'], '');
  };
}

function newProjectViaFile(projectName, sampleName, callback) {
  var blkFileContent = getSampleBlkFileContent(sampleName);
  setTimeout(function() {
    callback(blkFileContent, '');
  }, 0);
}

function saveProjectViaFile(projectName, blkFileContent, jsFileContent, callback) {
  if (!db) {
    openOfflineDatabase(function(success, errorReason) {
      if (success) {
        saveProjectViaFile(projectName, blkFileContent, jsFileContent, callback);
      } else {
        callback(false, 'Save project failed. (' + errorReason + ')');
      }
    });
    return;
  }
  var blkFileName = projectName + '.blk';
  var extra = parseExtraXml(blkFileContent);
  var blkFilesObjectStore = db.transaction(['blkFiles'], 'readwrite')
      .objectStore('blkFiles');
  var getRequest = blkFilesObjectStore.get(blkFileName);
  getRequest.onerror = function(event) {
    callback(false, 'Save project failed. (getRequest error)');
  };
  getRequest.onsuccess = function(event) {
    var value;
    if (event.target.result === undefined) {
      var value = Object.create(null);
      value['FileName'] = blkFileName;
      value['name'] = projectName;
      value['escapedName'] = escapeHtml(projectName);
    } else {
      value = event.target.result;
    }
    value['Content'] = blkFileContent;
    value['dateModifiedMillis'] = Date.now();
    value['enabled'] = extra['enabled']
    var putRequest = blkFilesObjectStore.put(value);
    putRequest.onerror = function(event) {
      callback(false, 'Save project failed. (putRequest error)');
    };
    putRequest.onsuccess = function(event) {
      callback(true, '');
    };
  };
}

function renameProjectViaFile(oldProjectName, newProjectName, callback) {
  if (!db) {
    openOfflineDatabase(function(success, errorReason) {
      if (success) {
        renameProjectViaFile(oldProjectName, newProjectName, callback);
      } else {
        callback(false, 'Rename project failed. (' + errorReason + ')');
      }
    });
    return;
  }
  var oldFileName = oldProjectName + '.blk';
  var newFileName = newProjectName + '.blk';
  var blkFilesObjectStore = db.transaction(['blkFiles'], 'readwrite')
      .objectStore('blkFiles');
  var getRequest = blkFilesObjectStore.get(oldFileName);
  getRequest.onerror = function(event) {
    callback(false, 'Rename project failed. (getRequest error)');
  };
  getRequest.onsuccess = function(event) {
    if (event.target.result === undefined) {
      callback(false, 'Rename project failed. (project not found)');
      return;
    }
    var value = event.target.result;
    value['FileName'] = newFileName;
    value['name'] = newProjectName;
    value['escapedName'] = escapeHtml(newProjectName);
    value['dateModifiedMillis'] = Date.now();
    var putRequest = blkFilesObjectStore.put(value);
    putRequest.onerror = function(event) {
      callback(false, 'Rename project failed. (putRequest error)');
    };
    putRequest.onsuccess = function(event) {
      var deleteRequest = blkFilesObjectStore.delete(oldFileName);
      deleteRequest.onerror = function(event) {
        callback(false, 'Rename project failed. (deleteRequest error)');
      };
      deleteRequest.onsuccess = function(event) {
        callback(true, '');
      };
    };
  };
}

function copyProjectViaFile(oldProjectName, newProjectName, callback) {
  if (!db) {
    openOfflineDatabase(function(success, errorReason) {
      if (success) {
        copyProjectViaFile(oldProjectName, newProjectName, callback);
      } else {
        callback(false, 'Copy project failed. (' + errorReason + ')');
      }
    });
    return;
  }
  var oldFileName = oldProjectName + '.blk';
  var newFileName = newProjectName + '.blk';
  var blkFilesObjectStore = db.transaction(['blkFiles'], 'readwrite')
      .objectStore('blkFiles');
  var getRequest = blkFilesObjectStore.get(oldFileName);
  getRequest.onerror = function(event) {
    callback(false, 'Copy project failed. (getRequest error)');
  };
  getRequest.onsuccess = function(event) {
    if (event.target.result === undefined) {
      callback(false, 'Copy project failed. (project not found)');
      return;
    }
    var value = event.target.result;
    value['FileName'] = newFileName;
    value['name'] = newProjectName;
    value['escapedName'] = escapeHtml(newProjectName);
    value['dateModifiedMillis'] = Date.now();
    var putRequest = blkFilesObjectStore.put(value);
    putRequest.onerror = function(event) {
      callback(false, 'Copy project failed. (putRequest error)');
    };
    putRequest.onsuccess = function(event) {
      callback(true, '');
    };
  };
}

function enableProjectViaFile(oldProjectName, enable, callback) {
  if (!db) {
    openOfflineDatabase(function(success, errorReason) {
      if (success) {
        enableProjectViaFile(oldProjectName, enable, callback);
      } else {
        callback(false, 'Enable project failed. (' + errorReason + ')');
      }
    });
    return;
  }
  var blkFileName = projectName + '.blk';

  var blkFilesObjectStore = db.transaction(['blkFiles'], 'readwrite')
      .objectStore('blkFiles');
  var getRequest = blkFilesObjectStore.get(blkFileName);
  getRequest.onerror = function(event) {
    callback(false, 'Enable project failed. (getRequest error)');
  };
  getRequest.onsuccess = function(event) {
    if (event.target.result === undefined) {
      callback(false, 'Enable project failed. (project not found)');
      return;
    }
    var value = event.target.result;
    var oldContent = value['Content'];
    var newContent = oldContent.replace(
        '<Enabled value="' + !enable + '"',
        '<Enabled value="' + enable + '"');
    value['Content'] = newContent;
    value['dateModifiedMillis'] = Date.now();
    value['enabled'] = enable;
    var putRequest = blkFilesObjectStore.put(value);
    putRequest.onerror = function(event) {
      callback(false, 'Enable project failed. (putRequest error)');
    };
    putRequest.onsuccess = function(event) {
      callback(true, '');
    };
  };
}

function deleteProjectsViaFile(starDelimitedProjectNames, callback) {
  if (!db) {
    openOfflineDatabase(function(success, errorReason) {
      if (success) {
        deleteProjectsViaFile(starDelimitedProjectNames, callback);
      } else {
        callback(false, 'Delete projects failed. (' + errorReason + ')');
      }
    });
    return;
  }
  var projectNames = starDelimitedProjectNames.split('*');
  var errorReasons = [];
  var successCount = 0;

  for (var i = 0; i < projectNames.length; i++) {
    deleteOneProjectViaFile(projectNames[i], function(success, errorReason) {
      if (success) {
        successCount++;
      } else {
        errorReasons.push(errorReason);
      }
      if (successCount + errorReasons.length == projectNames.length) {
        if (errorReasons.length == 0) {
          callback(true, '');
        } else {
          callback(false, 'Delete projects failed. (' + errorReasons.join(', ') + ')');
        }
      }
    });
  }
}

function deleteOneProjectViaFile(projectName, callback) {
  var blkFileName = projectName + '.blk';
  var deleteRequest = db.transaction(['blkFiles'], 'readwrite')
      .objectStore('blkFiles')
      .delete(blkFileName);
  deleteRequest.onerror = function(event) {
    callback(false, 'deleteRequest error');
  };
  deleteRequest.onsuccess = function(event) {
    callback(true, '');
  };
}

function getBlocksJavaClassNameViaFile(projectName, callback) {
  var className = [];
  var ch = projectName.charAt(0);
  if (isJavaIdentifierStart(ch)) {
    className.push(ch);
  } else if (isJavaIdentifierPart(ch)) {
    className.push('_');
    className.push(ch);
  }
  var length = projectName.length;
  for (var i = 1; i < length; i++) {
    ch = projectName.charAt(i);
    if (isJavaIdentifierPart(ch)) {
      className.push(ch);
    }
  }
  setTimeout(function() {
    callback(className.join(''), '');
  }, 0);
}

function saveBlocksJavaViaFile(relativeFileName, javaCode, callback) {
  // In offline blocks, we allow download of java, but not export to OnBotJava.
  setTimeout(function() {
    callback(false, 'Save Java code failed.');
  }, 0);
}
