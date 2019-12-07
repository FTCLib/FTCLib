/**
 * @fileoverview functions used in both FtcBlocks.html and FtcOfflineBlocks.html
 * @author lizlooney@google.com (Liz Looney)
 */

function initializeFtcBlocks() {
  fetchJavaScriptForHardware(function(jsHardware, errorMessage) {
    if (jsHardware) {
      var newScript = document.createElement('script');
      newScript.setAttribute('type', 'text/javascript');
      newScript.innerHTML = jsHardware;
      document.getElementsByTagName('head')[0].appendChild(newScript);

      initializeBlockly();
      initializeToolbox();

      setTimeout(function() {
        initializeBlocks();
      }, 10);
    } else  {
      alert(errorMessage);
    }
  });
}

function initializeBlocks() {
  var projectName = getParameterByName('project');
  if (isValidProjectName(projectName)) {
    currentProjectName = projectName;
    getBlocksJavaClassName(currentProjectName, function(className, errorMessage) {
      if (className) {
        currentClassName = className;
        Blockly.FtcJava.setClassNameForFtcJava_(currentClassName);
      } else {
        alert(errorMessage);
      }
    });
    fetchBlkFileContent(currentProjectName, function(blkFileContent, errorMessage) {
      if (blkFileContent) {
        var blocksLoadedCallback = function() {
          showJava();
        };
        loadBlocks(blkFileContent, blocksLoadedCallback);
      } else {
        alert(errorMessage);
      }
    });
  } else {
    alert('Error: The specified project name is not valid.');
  }
}

function displayBanner(text, buttonText, buttonCallback) {
  banner.style.display = 'flex';
  bannerText.innerHTML = text;
  bannerButton.innerHTML = buttonText;
  bannerButton.onclick = buttonCallback;
  resizeBlocklyArea();
}

function hideBanner() {
  banner.style.display = 'none';
  resizeBlocklyArea();
}

/**
 * Get a URL parameter by name.
 * From http://stackoverflow.com/a/901144
 */
function getParameterByName(name) {
  url = window.location.href;
  name = name.replace(/[\[\]]/g, '\\$&');
  var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
      results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return '';
  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

function afterBlocklyClipboardCaptured(clipboardContent) {
  // Save clipboard content.
  savedClipboardContent = clipboardContent;
  saveClipboardContent(savedClipboardContent, function(success, errorMessage) {
    if (! success) {
      console.log(errorMessage);
    }
  });
}

function paste() {
  // Fetch clipboard text
  fetchClipboardContent(function(clipboardContent, errorMessage) {
    if (!clipboardContent) {
      // If we failed to fetch the clipboard, use the saved clipboard content.
      clipboardContent = savedClipboardContent;
    }
    pasteContent(clipboardContent);
  });
}

function projectEnabledChanged() {
  var enabledCheckbox = document.getElementById('project_enabled');
  var isChecked = enabledCheckbox.checked;
  enableProject(currentProjectName, isChecked, function(success, errorMessage) {
    if (success) {
      projectEnabled = isChecked;
      showJava();
    } else {
      // Undo the checkbox change in the UI.
      enabledCheckbox.checked = projectEnabled;
      console.log(errorMessage);
    }
  });
}

/**
 * Saves the workspace blocks (including OpMode flavor, group, enable) and generated javascript.
 * Called from Save button onclick.
 */
function saveButtonClicked() {
  if (blockIdsWithMissingHardware.length == 0) {
    saveProjectNow();
  } else {
    var messageDiv = document.getElementById('saveWithWarningsMessage');
    if (blockIdsWithMissingHardware.length == 1) {
      if (missingHardware.length == 1) {
        messageDiv.innerHTML = 'There is 1 block that uses a missing hardware device.';
      } else {
        messageDiv.innerHTML = 'There is 1 block that uses missing hardware devices.';
      }
    } else {
      if (missingHardware.length == 1) {
        messageDiv.innerHTML = 'There are ' + blockIdsWithMissingHardware.length +
            ' blocks that use a missing hardware device.';
      } else {
        messageDiv.innerHTML = 'There are ' + blockIdsWithMissingHardware.length +
            ' blocks that use missing hardware devices.';
      }
    }
     document.getElementById('saveWithWarningsDialog').style.display = 'block';
  }
}

function noSaveWithWarningsDialog() {
  // Close the dialog.
  document.getElementById('saveWithWarningsDialog').style.display = 'none';
}

function yesSaveWithWarningsDialog() {
  // Close the dialog.
  document.getElementById('saveWithWarningsDialog').style.display = 'none';
  saveProjectNow();
}

function saveProjectNow(opt_success_callback) {
  if (currentProjectName) {
    var allBlocks = workspace.getAllBlocks();
    for (var iBlock = 0, block; block = allBlocks[iBlock]; iBlock++) {
      saveBlockWarningHidden(block);
    }
    // Get the blocks as xml (text).
    var blocksContent = Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace));
    // Don't bother saving if there are no blocks.
    if (blocksContent.indexOf('<block') > -1) {
      var disabled = disableOrphans();
      // Generate JavaScript code.
      var jsFileContent = Blockly.JavaScript.workspaceToCode(workspace);
      reenableOrphans(disabled);
      var flavorSelect = document.getElementById('project_flavor');
      var flavor = flavorSelect.options[flavorSelect.selectedIndex].value;
      var group = document.getElementById('project_group').value;
      var blkFileContent = blocksContent + formatExtraXml(flavor, group, projectEnabled);
      saveProject(currentProjectName, blkFileContent, jsFileContent,
          function(success, errorMessage) {
        if (success) {
          isDirty = false;
          document.getElementById('saveSuccess').style.display = 'inline-block';
          document.getElementById('saveFailure').style.display = 'none';
          window.setTimeout(function() {
            document.getElementById('saveSuccess').style.display = 'none';
          }, 3000);
          if (opt_success_callback) {
            opt_success_callback();
          }
        } else {
          document.getElementById('saveSuccess').style.display = 'none';
          document.getElementById('saveFailure').innerHTML = errorMessage;
          document.getElementById('saveFailure').style.display = 'inline-block';
        }
      });
    }
  } else {
    alert('The specified project name is not valid');
  }
}

function disableOrphans() {
  Blockly.Events.disable();
  var disabled = [];
  var blocks = workspace.getTopBlocks(true);
  for (var x = 0, block; block = blocks[x]; x++) {
    if (block.type != 'procedures_defnoreturn' &&
        block.type != 'procedures_defreturn' &&
        block.isEnabled()) {
      do {
        block.setEnabled(false);
        disabled.push(block);
        block = block.getNextBlock();
      } while (block);
    }
  }
  Blockly.Events.enable();
  return disabled;
}

function reenableOrphans(disabled) {
  Blockly.Events.disable();
  for (var x = 0, block; block = disabled[x]; x++) {
    block.setEnabled(true);
  }
  Blockly.Events.enable();
}

/**
 * After saving the project, downloads the blk file.
 * Called from Download button onclick.
 */
function downloadButtonClicked() {
  saveProjectNow(function() {
    fetchBlkFileContent(currentProjectName, function(blkFileContent, errorMessage) {
      if (blkFileContent) {
        downloadBlocks(blkFileContent);
      } else {
        alert(errorMessage);
      }
    });
  });
}

function initializeSplit() {
  split = window.Split([blocksAndBannerArea, javaArea], {
    direction: 'horizontal',
    sizes: [75, 25],
    minSize: [200, 100],
    gutterSize: 4,
    snapOffset: 0,
    onDrag: resizeBlocklyArea,
  });
}

// Initialize global variables & blockly itself
function initializeBlockly() {
  addReservedWordsForJavaScript();
  addReservedWordsForFtcJava();

  document.addEventListener('mousemove', onMouseMove);
  document.addEventListener('keydown', onKeyDown);

  // Blockly's text_quotes extension (which uses images for the quotes) causes the Download
  // Image feature fail. Here, we replace it with one that uses quote characters.
  Blockly.Extensions.ALL_['text_quotes'] = function() {
    for (var i = 0, input; input = this.inputList[i]; i++) {
      for (var j = 0, field; field = input.fieldRow[j]; j++) {
        if ('TEXT' == field.name) {
          var before = workspace.RTL ? '\u201D' : '\u201C';
          var after = workspace.RTL ? '\u201C' : '\u201D';
          input.insertFieldAt(j, new Blockly.FieldLabel(before));
          input.insertFieldAt(j + 2, new Blockly.FieldLabel(after));
          return;
        }
      }
    }
  };

  isDirty = false;
  showJavaCheckbox = document.getElementById('show_java');
  javaArea = document.getElementById('javaArea');
  javaContent = document.getElementById('javaContent');
  parentArea = document.getElementById('parentArea');
  blocksAndBannerArea = document.getElementById('blocksAndBannerArea');
  blocklyArea = document.getElementById('blocklyArea');
  blocklyDiv = document.getElementById('blocklyDiv');
  banner = document.getElementById('banner');
  bannerText = document.getElementById('bannerText');
  bannerButton = document.getElementById('bannerBtn');
  workspace = Blockly.inject(blocklyDiv, {
    media: 'blockly/media/',
    zoom: {
      controls: true,
      wheel: true,
      startScale: 1.2,
      maxScale: 5,
      minScale: 0.3,
      scaleSpeed: 1.2},
    trashcan: false,
    toolbox: document.getElementById('toolbox')
  });

  if (parentArea.clientWidth >= 800) {
    showJavaCheckbox.checked = true;
  }
  showJavaChanged();

  parentArea.style.visibility = 'visible'; // Unhide parentArea

  window.addEventListener('resize', resizeBlocklyArea, false);
  resizeBlocklyArea();
  window.addEventListener('beforeunload', function(e) {
    if (!isDirty) {
      return undefined;
    }
    // It doesn't matter what string we return here. The browser will always use a standard message
    // for security reasons.
    (e || window.event).returnValue = ''; // Gecko + IE
    return ''; // Gecko + Webkit, Safari, Chrome etc.
  });

  workspace.addChangeListener(function(event) {
    isDirty = true;

    // Check blocks.
    var blockIds = [];
    switch (event.type) {
      case Blockly.Events.BLOCK_CREATE:
        Array.prototype.push.apply(blockIds, event.ids);
        break;
      case Blockly.Events.BLOCK_CHANGE:
        if (event.blockId) {
          blockIds.push(event.blockId);
        }
        break;
      case Blockly.Events.BLOCK_DELETE:
        // Remove deleted blocks from blockIdsWithMissingHardware.
        for (var iId = 0, blockId; blockId = event.ids[iId]; iId++) {
          if (blockIdsWithMissingHardware.includes(blockId)) {
            var index = blockIdsWithMissingHardware.indexOf(blockId);
            blockIdsWithMissingHardware.splice(index, 1);
          }
        }
        break;
    }
    for (var i = 0; i < blockIds.length; i++) {
      var block = workspace.getBlockById(blockIds[i]);
      if (block) {
        var hasWarningBits = checkBlock(block, missingHardware);
        if (hasWarningBits & WarningBits.MISSING_HARDWARE) {
          if (!blockIdsWithMissingHardware.includes(blockIds[i])) {
            blockIdsWithMissingHardware.push(blockIds[i]);
          }
        } else {
          if (blockIdsWithMissingHardware.includes(blockIds[i])) {
            var index = blockIdsWithMissingHardware.indexOf(blockIds[i]);
            blockIdsWithMissingHardware.splice(index, 1);
          }
          saveVisibleIdentifiers(block);
        }
      }
    }
    showJava();
  });
}

function resizeBlocklyArea() {
  // Compute the absolute coordinates and dimensions of blocklyArea.
  var x = 0;
  var y = 0;
  var w = blocklyArea.offsetWidth;
  var h = blocklyArea.offsetHeight;
  var element = blocklyArea;
  do {
    x += element.offsetLeft;
    y += element.offsetTop;
    element = element.offsetParent;
  } while (element);
  // Position blocklyDiv over blocklyArea.
  blocklyDiv.style.left = x + 'px';
  blocklyDiv.style.top = y + 'px';
  blocklyDiv.style.width = w + 'px';
  blocklyDiv.style.height = h + 'px';
  Blockly.svgResize(workspace);
}

function initializeToolbox() {
  workspace.updateToolbox(getToolbox());
  addToolboxIcons(workspace);
}

function loadBlocks(blkFileContent, opt_blocksLoaded_callback) {
  // The blocks content is up to and including the first </xml>.
  var i = blkFileContent.indexOf('</xml>');
  var blocksContent = blkFileContent.substring(0, i + 6);

  var extra = parseExtraXml(blkFileContent);
  var flavorSelect = document.getElementById('project_flavor');
  for (var i = 0; i < flavorSelect.options.length; i++) {
    if (flavorSelect.options[i].value == extra['flavor']) {
      flavorSelect.selectedIndex = i;
      break;
    }
  }
  document.getElementById('project_group').value = extra['group'];
  document.getElementById('project_enabled').checked = extra['enabled'];

  loadBlocksIntoWorkspace(blocksContent, opt_blocksLoaded_callback);
  checkDownloadImageFeature();
}

/**
 * Loads the given blocksContent into the workspace.
 */
function loadBlocksIntoWorkspace(blocksContent, opt_blocksLoaded_callback) {
  document.title = titlePrefix + ' - ' + currentProjectName;
  var escapedProjectName = currentProjectName.replace(/&/g, '&amp;');
  document.getElementById('project_name').innerHTML = escapedProjectName;
  missingHardware = [];
  blockIdsWithMissingHardware = [];
  workspace.clear();
  Blockly.Xml.domToWorkspace(Blockly.Xml.textToDom(blocksContent), workspace);

  // Use a timeout to allow the workspace change event to come through. Then, show an alert
  // if any blocks have warnings. Then clear isDirty.
  setTimeout(function() {
    if (blockIdsWithMissingHardware.length > 0) {
      var message = (blockIdsWithMissingHardware.length == 1)
          ? 'An error occured when trying to find the hardware devices for 1 block.\n\n'
          : 'Errors occured when trying to find the hardware devices for ' + blockIdsWithMissingHardware.length +
              ' blocks.\n\n';
      if (missingHardware.length > 0) {
        message += 'The following hardware devices were not found:\n';
        for (var i = 0; i < missingHardware.length; i++) {
          message += '    "' + missingHardware[i] + '"\n';
        }
      }
      message += '\nIf the current configuration is not the appropriate configuration for this ' +
          'blocks project, please activate the appropriate configuration and reload this page.';
      alert(message);
    }

    isDirty = false;

    if (opt_blocksLoaded_callback) {
      opt_blocksLoaded_callback();
    }
  }, 50);
}

/**
 * Add/remove the block warning if the given block's identifier(s) are not in the active
 * configuration. Return true if the block now has a warning, false otherwise.
 */
function checkBlock(block, missingHardware) {
  var warningBits = 0;
  try {
    var warningText = null; // null will remove any previous warning.
    for (var iFieldName = 0; iFieldName < identifierFieldNames.length; iFieldName++) {
      var identifierFieldName = identifierFieldNames[iFieldName];
      var field = block.getField(identifierFieldName);
      if (field) {
        var identifierFieldValue = field.getValue();
        var identifierFound = true;
        var fieldHasOptions = false;

        if (typeof field.getOptions === 'function') {
          // The identifier field is a dropdown field with options.
          // Check if identifierFieldValue is the value of one of the options.
          var options = field.getOptions();
          fieldHasOptions = true;
          identifierFound = false;
          for (var iOption = 0; iOption < options.length; iOption++) {
            if (options[iOption][1] == identifierFieldValue) {
              identifierFound = true;
              break;
            }
          }
          if (!identifierFound) {
            // Check if identifierFieldValue is the visible name of one of the options.
            for (var iOption = 0; iOption < options.length; iOption++) {
              if (options[iOption][0] == identifierFieldValue) {
                identifierFieldValue = options[iOption][1];
                field.setValue(identifierFieldValue);
                identifierFound = true;
                break;
              }
            }
          }
        } else {
          // The identifier field is a noneditable field instead of a dropdown field.
          // Since blockly doesn't allow a dropdown field to have zero options, we need to use a
          // noneditable field (which looks similar) when there are no hardware items for this
          // type of block.
          identifierFound = false;
        }

        if (!identifierFound) {
          // identifierFieldValue is the name of the identifier that will be used in generated
          // javascript, but it is not necessarily the same as the name that is used in the
          // hardware configuration and should be displayed on the block. For example, the
          // visible identifier name may contain characters that are not allowed in a javascript
          // identifier. Also, the identifier may have a suffix that allows us to distinguish
          // between hardware devices with the same configuration name or hardware devices that
          // support multiple hardware interfaces.
          // We now store the visible identifier names using block.data.
          // Here, we try to use block.data to retrieve the visible identifier name.
          var visibleIdentifierName;
          if (block.data) {
            if (block.data.startsWith('{')) {
              var visibleIdentifierNames = JSON.parse(block.data);
              visibleIdentifierName = visibleIdentifierNames[identifierFieldName];
            } else {
              visibleIdentifierName = block.data;
            }
          } else {
            // If the blocks file is older, we don't know what the visible name actually is.
            // The best we can do is to remove the hardware identifier suffix if there is one.
            visibleIdentifierName = removeHardwareIdentifierSuffix(identifierFieldValue);
          }
          if (typeof field.setText === 'function') {
            field.setText(visibleIdentifierName);
          }
          if (!missingHardware.includes(visibleIdentifierName)) {
            missingHardware.push(visibleIdentifierName);
          }
          warningBits |= WarningBits.MISSING_HARDWARE;
          if (fieldHasOptions) {
            warningText = addWarning(warningText,
                '"' + visibleIdentifierName + '" is not in the current robot configuration.\n\n' +
                'Please activate the configuration that contains the hardware device named "' +
                visibleIdentifierName + '",\nor select a device that is in the current robot configuration.');
          } else {
            warningText = addWarning(warningText,
                '"' + visibleIdentifierName + '" is not in the current robot configuration.\n\n' +
                'Please activate the configuration that contains the hardware device named "' +
                visibleIdentifierName + '".');
          }
        }
      }
    }
    if (block.type == 'vuforia_initialize' ||
        block.type == 'vuforia_initializeExtended' ||
        block.type == 'vuforia_initializeExtendedNoKey' ||
        block.type == 'vuforia_initialize_withWebcam' ||
        block.type == 'vuforia_activate' ||
        block.type == 'vuforia_deactivate' ||
        block.type == 'vuforia_track' ||
        block.type == 'vuforia_trackPose' ||
        block.type == 'vuforia_typedEnum_trackableName' ||
        block.type == 'vuforiaTrackingResults_getProperty_RelicRecoveryVuMark' ||
        block.type == 'vuMarks_typedEnum_relicRecoveryVuMark') {
      warningBits |= WarningBits.RELIC_RECOVERY;
      warningText = addWarning(warningText,
          'This block is optimized for Relic Recovery (2017-2018) and will not work correctly ' +
          'for SKYSTONE (2019-2020).\n\n' +
          'Please replace this block with the corresponding one from the Optimized for SKYSTONE ' +
          'toolbox category.');
    } else if (block.type == 'vuforiaRoverRuckus_initialize_withCameraDirection' ||
        block.type == 'vuforiaRoverRuckus_initialize_withWebcam' ||
        block.type == 'vuforiaRoverRuckus_activate' ||
        block.type == 'vuforiaRoverRuckus_deactivate' ||
        block.type == 'vuforiaRoverRuckus_track' ||
        block.type == 'vuforiaRoverRuckus_trackPose' ||
        block.type == 'vuforiaRoverRuckus_typedEnum_trackableName' ||
        block.type == 'tfodRoverRuckus_initialize' ||
        block.type == 'tfodRoverRuckus_activate' ||
        block.type == 'tfodRoverRuckus_deactivate' ||
        block.type == 'tfodRoverRuckus_setClippingMargins' ||
        block.type == 'tfodRoverRuckus_getRecognitions' ||
        block.type == 'tfodRoverRuckus_typedEnum_label') {
      warningBits |= WarningBits.ROVER_RUCKUS;
      warningText = addWarning(warningText,
          'This block is optimized for Rover Ruckus (2018-2019) and will not work correctly ' +
          'for SKYSTONE (2019-2020).\n\n' +
          'Please replace this block with the corresponding one from the Optimized for SKYSTONE ' +
          'toolbox category.');
    }

    // If warningText is null, the following will clear a previous warning.
    var previousWarningText = block.warning ? block.warning.getText() : null;
    if (previousWarningText != warningText) {
      block.setWarningText(warningText);
      if (warningText && block.warning && !readBlockWarningHidden(block)) {
        block.warning.setVisible(true);
      }
    }
  } catch (e) {
    console.log('Unable to verify the following block due to exception.');
    console.log(block);
    console.log(e);
  }
  return warningBits;
}

function addWarning(warningText, textToAdd) {
  if (warningText == null) {
    warningText = '';
  } else {
    warningText += '\n\n';
  }
  warningText += textToAdd;
  return warningText;
}

function removeHardwareIdentifierSuffix(identifierFieldValue) {
  var suffixes = getHardwareIdentifierSuffixes();
  for (var i = 0; i < suffixes.length; i++) {
    var suffix = suffixes[i];
    if (identifierFieldValue.endsWith(suffix)) {
      identifierFieldValue =
          identifierFieldValue.substring(0, identifierFieldValue.length - suffix.length);
      break;
    }
  }
  return identifierFieldValue;
}

function saveBlockWarningHidden(block) {
  var data = (block.data && block.data.startsWith('{'))
      ? JSON.parse(block.data) : null;

  if (block.warning) {
    if (!block.warning.isVisible()) {
      if (!data) {
        data = Object.create(null);
      }
      data.block_warning_hidden = true;
    } else {
      if (data) {
        delete data.block_warning_hidden;
      }
    }
  }

  block.data = data ? JSON.stringify(data) : null;
}

function readBlockWarningHidden(block) {
  if (block.data && block.data.startsWith('{')) {
    var data = JSON.parse(block.data);
    if (data.block_warning_hidden) {
      return true;
    }
  }

  return false;
}

function saveVisibleIdentifiers(block) {
  var data = (block.data && block.data.startsWith('{'))
      ? JSON.parse(block.data) : null;

  for (var iFieldName = 0; iFieldName < identifierFieldNames.length; iFieldName++) {
    var identifierFieldName = identifierFieldNames[iFieldName];
    var field = block.getField(identifierFieldName);
    if (field) {
      if (typeof field.getOptions === 'function') {
        // The identifier field is a dropdown field with options.
        // Save the user visible identifiers using block.data, so we can use it in the future if
        // the hardware device is not found in the configuration.
        if (!data) {
          data = Object.create(null);
        }
        data[identifierFieldName] = field.getText();
      }
    }
  }

  block.data = data ? JSON.stringify(data) : null;
}

function onMouseMove(e) {
  mouseX = e.clientX;
  mouseY = e.clientY;
}

function onKeyDown(e) {
  if (Blockly.mainWorkspace.options.readOnly || Blockly.utils.isTargetInput(e)) {
    // No key actions on readonly workspaces.
    // When focused on an HTML text input widget, don't trap any keys.
    return;
  }
  if (e.altKey || e.ctrlKey || e.metaKey) {
    if (Blockly.selected &&
        Blockly.selected.isDeletable() && Blockly.selected.isMovable()) {
      if (e.keyCode == 67 || // 'c' for copy.
          e.keyCode == 88) { // 'x' for cut.
        // Use a timeout so we can capture the blockly clipboard.
        setTimeout(function() {
          captureBlocklyClipboard();
        }, 1);
      }
    }

    if (e.keyCode == 86) { // 'v' for paste.
      // Override blockly's default paste behavior.
      paste();
      e.stopImmediatePropagation();
    }
  }
}

function captureBlocklyClipboard() {
  if (Blockly.clipboardXml_) {
    if (previousClipboardXml != Blockly.clipboardXml_) {
      previousClipboardXml = Blockly.clipboardXml_;

      // Convert to text.
      var xml = goog.dom.createDom('xml');
      xml.appendChild(Blockly.clipboardXml_);
      var serializer = new XMLSerializer();
      var clipboardContent = serializer.serializeToString(xml);
      xml.removeChild(Blockly.clipboardXml_);

      afterBlocklyClipboardCaptured(clipboardContent);
    }
  }
}

function pasteContent(clipboardContent) {
  if (!clipboardContent) {
    return;
  }
  var parser = new DOMParser();
  var xmlDoc = parser.parseFromString(clipboardContent, 'text/xml');
  var blocks = xmlDoc.getElementsByTagName('block');
  if (blocks.length > 0) {
    var block = blocks[0];
    // Place the pasted block near mouse.
    var svg = workspace.getParentSvg();
    var point = svg.createSVGPoint();
    point.x = mouseX;
    point.y = mouseY;
    point = point.matrixTransform(svg.getScreenCTM().inverse());
    point = point.matrixTransform(workspace.getCanvas().getCTM().inverse());
    block.setAttribute('x', point.x);
    block.setAttribute('y', point.y);
    workspace.paste(block);
  }
}

function downloadBlocks(blkFileContent) {
  var a = document.getElementById('download_link');
  a.href = 'data:text/plain;charset=utf-8,' + encodeURIComponent(blkFileContent);
  a.download = currentProjectName + '.blk';
  a.target = '_blank';
  a.click();
}

function checkDownloadImageFeature() {
  // Show and enable the download image button if workspace.svgBlockCanvas_ and canvas.toBlob is
  // defined.
  if (workspace.svgBlockCanvas_ !== undefined) {
    var canvasElement = document.createElement('canvas');
    if (canvasElement.toBlob !== undefined) {
      var downloadImageButton = document.getElementById('downloadImageButton');
      downloadImageButton.disabled = false;
      downloadImageButton.style.display = 'inline';
    }
  }
}

function downloadImageButtonClicked() {
  // Clone the workspace' svg canvas.
  var svgCanvas = workspace.svgBlockCanvas_;
  var clone = svgCanvas.cloneNode(true);
  var box;
  if (svgCanvas.tagName == 'svg') {
    box = svgCanvas.getBoundingClientRect();
    var metrics = workspace.getMetrics();
    var left = (parseFloat(metrics.contentLeft) - parseFloat(metrics.viewLeft)).toString();
    var top = (parseFloat(metrics.contentTop) - parseFloat(metrics.viewTop)).toString();
    var right = (parseFloat(metrics.contentWidth)).toString();
    var bottom = (parseFloat(metrics.contentHeight)).toString();
    clone.setAttribute('viewBox', left + ' ' + top + ' ' + right + ' ' + bottom);
  } else {
    clone.setAttribute('transform',
        clone.getAttribute('transform')
            .replace(/translate\(.*?\)/, '')
            .replace(/scale\(.*?\)/, '')
            .trim());
    var svg = document.createElementNS('http://www.w3.org/2000/svg','svg')
    svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
    svg.appendChild(clone)
    clone = svg;
    box = svgCanvas.getBBox();
    clone.setAttribute('viewBox', box.x + ' ' + box.y + ' ' + box.width + ' ' + box.height);
  }
  clone.setAttribute('version', '1.1');
  clone.setAttribute('width', box.width);
  clone.setAttribute('height', box.height);
  clone.setAttribute('style', 'background-color: #FFFFFF');
  var divElement = document.createElement('div');
  divElement.appendChild(clone);

  // Collect style sheets.
  var css = '';
  var sheets = document.styleSheets;
  for (var i = 0; i < sheets.length; i++) {
    if (isExternal(sheets[i].href)) {
      continue;
    }
    var rules = null;
    try {
      rules = sheets[i].cssRules;
    } catch (err) {
      // Cannot access cssRules from external css files in offline blocks editor.
    }
    if (rules != null) {
      for (var j = 0; j < rules.length; j++) {
        var rule = rules[j];
        if (typeof(rule.style) != 'undefined') {
          var match = null;
          try {
            match = svgCanvas.querySelector(rule.selectorText);
          } catch (err) {
          }
          if (match && rule.selectorText.indexOf('blocklySelected') == -1) {
            css += rule.selectorText + ' { ' + rule.style.cssText + ' }\n';
          } else if(rule.cssText.match(/^@font-face/)) {
            css += rule.cssText + '\n';
          }
        }
      }
    }
  }
  var styleElement = document.createElement('style');
  styleElement.setAttribute('type', 'text/css');
  styleElement.innerHTML = '<![CDATA[\n' + css + '\n]]>';
  var defsElement = document.createElement('defs');
  defsElement.appendChild(styleElement);
  clone.insertBefore(defsElement, clone.firstChild);

  // TODO(lizlooney): hide all blocklyScrollbarHandle, blocklyScrollbarBackground, image,
  // .blocklyMainBackground, rectCorner, indicatorWarning?

  var doctype = '<?xml version="1.0" standalone="no"?>' +
      '<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" ' +
      '"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">';
  var svg = doctype + divElement.innerHTML;
  svg = svg.replace(/&nbsp/g, '&#160');
  svg = svg.replace(/sans-serif/g, 'Arial, Verdana, "Nimbus Sans L", Helvetica');
  var uri = 'data:image/svg+xml;base64,' + window.btoa(unescape(encodeURIComponent(svg)));

  var image = new Image();
  image.onload = function() {
    var canvasElement = document.createElement('canvas');
    canvasElement.width = image.width;
    canvasElement.height = image.height;
    canvasElement.getContext('2d').drawImage(image, 0, 0);
    canvasElement.toBlob(function(blob) {
      var a = document.getElementById('download_link');
      a.href = URL.createObjectURL(blob);
      a.download = currentProjectName + '.png';
      a.target = '_blank';
      a.click();
    });
  };
  image.onerror = function (e) {
    alert('Unable to download blocks image. Sorry about that!');
  };
  image.src = uri;
}

function isExternal(url) {
  return url && url.lastIndexOf('http', 0) == 0 && url.lastIndexOf(window.location.host) == -1;
}

function projectFlavorChanged() {
  showJava();
}

function projectGroupChanged() {
  showJava();
}

function showJavaChanged() {
  if (document.getElementById('show_java').checked) {
    javaArea.style.display = 'flex';
    initializeSplit();
  } else {
    if (split) split.destroy();
    blocksAndBannerArea.style.width="100%";
    javaArea.style.display = 'none';
  }
  resizeBlocklyArea();
  showJava();
}

function showJava() {
  if (document.getElementById('show_java').checked) {
    var javaCode = generateJavaCode();
    if (javaCode) {
      javaContent.textContent = javaCode;
      javaContent.style.color = 'black';
      return;
    }
    javaContent.style.color = 'gray';
  }
}

function generateJavaCode() {
  // Get the blocks as xml (text).
  var blocksContent = Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace));
  // Don't bother exporting if there are no blocks.
  if (blocksContent.indexOf('<block') > -1) {
    if (currentClassName) {
      // Generate Java code.
      return Blockly.FtcJava.workspaceToCode(workspace);
    }
  }
  return '';
}
