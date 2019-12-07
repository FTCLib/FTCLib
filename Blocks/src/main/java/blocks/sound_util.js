/**
 * @fileoverview Sound utilities.
 * @author lizlooney@google.com (Liz Looney)
 */

/**
 * Fetches the list of sounds (as json) and calls the callback.
 */
function fetchSounds(callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    fetchSoundsViaHttp(callback);
  }
}

/**
 * Opens the sound with the given name.
 */
function playSoundFile(soundName) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    playSoundFileViaHttp(soundName);
  }
}

/**
 * Fetches the content of an existing sound file and calls the callback
 */
function fetchSoundFileContent(soundName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    fetchSoundFileContentViaHttp(soundName, callback);
  }
}

function saveSound(soundName, base64Content, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    saveSoundViaHttp(soundName, base64Content, callback);
  }
}

function renameSound(oldSoundName, newSoundName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    renameSoundViaHttp(oldSoundName, newSoundName, callback);
  }
}

function copySound(oldSoundName, newSoundName, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    copySoundViaHttp(oldSoundName, newSoundName, callback);
  }
}

function deleteSounds(starDelimitedSoundNames, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    deleteSoundsViaHttp(starDelimitedSoundNames, callback);
  }
}

//..........................................................................
// Code used when html/js is in a browser, loaded as an http:// URL.

// The following are generated dynamically in ProgrammingModeServer.fetchJavaScriptForServer():
// URI_LIST_SOUNDS
// URI_FETCH_SOUND
// URI_FETCH_SOUND_TYPE
// URI_SAVE_SOUND
// URI_RENAME_SOUND
// URI_COPY_SOUND
// URI_DELETE_SOUNDS
// PARAM_NAME
// PARAM_NEW_NAME

function fetchSoundsViaHttp(callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', URI_LIST_SOUNDS, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var jsonSounds = xhr.responseText;
        callback(jsonSounds, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Fetch sounds failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send();
}

function playSoundFileViaHttp(soundName) {
  fetchSoundFileMimeTypeViaHttp(soundName, function(mimeType, errorMessage) {
    if (mimeType) {
      fetchSoundFileContent(soundName, function(base64Content, errorMessage) {
        if (base64Content) {
          var audio = new Audio('data:' + mimeType + ';base64,' + base64Content);
          audio.play()
        } else {
          console.log(errorMessage);
        }
      });
    } else {
      console.log(errorMessage);
    }
  });
}

function fetchSoundFileContentViaHttp(soundName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(soundName);
  xhr.open('POST', URI_FETCH_SOUND, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var base64Content = xhr.responseText;
        callback(base64Content, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Fetch sound failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function fetchSoundFileMimeTypeViaHttp(soundName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(soundName);
  xhr.open('POST', URI_FETCH_SOUND_TYPE, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var mimeType = xhr.responseText;
        callback(mimeType, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Fetch sound mime type failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function saveSoundViaHttp(soundName, base64Content, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(soundName) +
      '&' + PARAM_CONTENT + '=' + encodeURIComponent(base64Content);
  xhr.open('POST', URI_SAVE_SOUND, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Save sound failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function renameSoundViaHttp(oldSoundName, newSoundName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(oldSoundName) +
      '&' + PARAM_NEW_NAME + '=' + encodeURIComponent(newSoundName);
  xhr.open('POST', URI_RENAME_SOUND, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Rename sound failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function copySoundViaHttp(oldSoundName, newSoundName, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(oldSoundName) +
      '&' + PARAM_NEW_NAME + '=' + encodeURIComponent(newSoundName);
  xhr.open('POST', URI_COPY_SOUND, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Copy sound failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}

function deleteSoundsViaHttp(starDelimitedSoundNames, callback) {
  var xhr = new XMLHttpRequest();
  var params = PARAM_NAME + '=' + encodeURIComponent(starDelimitedSoundNames);
  xhr.open('POST', URI_DELETE_SOUNDS, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(false, 'Delete sounds failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send(params);
}
