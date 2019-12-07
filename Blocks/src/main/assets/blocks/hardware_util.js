/**
 * @fileoverview Hardware utilities.
 * @author lizlooney@google.com (Liz Looney)
 */

// Note: This file is misnamed. It includes some utilities not related to hardware.

/**
 * Fetches the JavaScript code related to the hardware in the active configuration and calls the
 * callback.
 */
function fetchJavaScriptForHardware(callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    fetchJavaScriptForHardwareViaHttp(callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    fetchJavaScriptForHardwareViaFile(callback);
  }
}

function getConfigurationName(callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    getConfigurationNameViaHttp(callback);
  } else if (window.location.protocol === 'file:') {
    // html/js is in a browser, loaded as an file:// URL.
    getConfigurationNameViaFile(callback);
  }
}

/**
 * Sends a ping request and calls the callback.
 */
function sendPing(name, callback) {
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    // html/js is in a browser, loaded as an http:// URL.
    sendPingViaHttp(name, callback);
  } else {
    callback(false);
  }
}

//..........................................................................
// Code used when html/js is in a browser, loaded as a http:// URL.

// The following are generated dynamically in ProgrammingModeServer.fetchJavaScriptForServer():
// URI_HARDWARE
// URI_PING
// PARAM_NAME

function fetchJavaScriptForHardwareViaHttp(callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', URI_HARDWARE, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var jsHardware = xhr.responseText;
        callback(jsHardware, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Fetch JavaScript for Hardware failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send();
}

function getConfigurationNameViaHttp(callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('POST', URI_GET_CONFIGURATION_NAME, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        var className = xhr.responseText;
        callback(className, '');
      } else {
        // TODO(lizlooney): Use specific error messages for various xhr.status values.
        callback(null, 'Get configuration name failed. Error code ' + xhr.status + '. ' + xhr.statusText);
      }
    }
  };
  xhr.send();
}

function sendPingViaHttp(name, callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('POST', URI_PING, true);
  xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        callback(true);
      } else {
        callback(false);
      }
    }
  };
  var params = PARAM_NAME + '=' + encodeURIComponent(name);
  xhr.send(params);
}

//..........................................................................
// Code used when html/js is in a browser, loaded as a file:// URL.

function fetchJavaScriptForHardwareViaFile(callback) {
  setTimeout(function() {
    callback('// See FtcOfflineBlocks.js', '');
  }, 0);
}

function getConfigurationNameViaFile(callback) {
  setTimeout(function() {
    callback(getOfflineConfigurationName(), '');
  }, 0);
}
