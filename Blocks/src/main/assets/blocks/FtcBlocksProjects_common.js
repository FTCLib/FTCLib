/**
 * @fileoverview functions used in both FtcBlocksProjects.html and FtcOfflineBlocksProjects.html
 * @author lizlooney@google.com (Liz Looney)
 */

// The following are generated dynamically in HardwareUtil.fetchJavaScriptForHardware():
// function isValidProjectName
var projects = [];
var checkedProjects = [];
var sortByName = false;
var sortByDateModified = true;
var sortAscending = false;
var NEW_PROJECT_NAME_DIALOG_MODE_NEW_PROJECT = 1;
var NEW_PROJECT_NAME_DIALOG_MODE_RENAME_PROJECT = 2;
var NEW_PROJECT_NAME_DIALOG_MODE_COPY_PROJECT = 3;
var newProjectNameDialogMode = 0;

function initializeFtcBlocksProjects() {
  window.addEventListener('resize', resize, false);
  resize();

  fetchJavaScriptForHardware(function(jsHardware, errorMessage) {
    if (jsHardware) {
      var newScript = document.createElement('script');
      newScript.setAttribute('type', 'text/javascript');
      newScript.innerHTML = jsHardware;
      document.getElementsByTagName('head').item(0).appendChild(newScript);
    } else  {
      alert(errorMessage);
    }
  });

  initializeProjects();
  initializeSamples();
}

function resize() {
  // Compute the height of projectsTableScroll.
  var projectsTableScroll = document.getElementById('projectsTableScroll');
  var element = projectsTableScroll;
  var y = 0;
  do {
    y += element.offsetTop;
    element = element.offsetParent;
  } while (element);
  projectsTableScroll.style.height = (window.innerHeight - y) + 'px';
}

function initializeProjects() {
  projects = [];
  fetchProjects(function(jsonProjects, errorMessage) {
    if (jsonProjects) {
      projects = JSON.parse(jsonProjects);
    } else {
      alert(errorMessage);
    }
    sortProjectsAndFillTable();
  });
}

function initializeSamples() {
  var select = document.getElementById('newProjectSamplesSelect');
  select.options.length = 0; // Clear previous values just in case.
  select.appendChild(document.createElement('option')); // Insert blank option.

  fetchSamples(function(jsonSamples, errorMessage) {
    if (jsonSamples) {
      var samples = JSON.parse(jsonSamples);
      for (var i = 0; i < samples.length; i++) {
        var sample = samples[i];
        var option = document.createElement('option');
        option.innerHTML = sample.escapedName;
        option.value = JSON.stringify(sample);
        select.appendChild(option);
      }
    } else {
      console.log(errorMessage);
    }
  });
}

function sampleSelected() {
  var warnings = '';
  var select = document.getElementById('newProjectSamplesSelect');
  var jsonSample = select.options[select.selectedIndex].value;
  if (jsonSample) {
    var sample = JSON.parse(jsonSample);
    if (sample.requestedCapabilities) {
      var delimiter = '';
      for (var i = 0; i < sample.requestedCapabilities.length; i++) {
        var requestedCapability = sample.requestedCapabilities[i];
        var warning = getCapabilityWarning(requestedCapability);
        if (warning) {
          warnings += delimiter + warning;
          delimiter = '<br>';
        }
      }
    }
  }
  document.getElementById('newProjectNameError').innerHTML = warnings;
}

function toggleSortByName() {
  if (sortByName) {
    sortAscending = !sortAscending;
  } else {
    sortByName = true;
    // When sorting by name, sorting ascending makes more sense than descending.
    sortAscending = true;
    sortByDateModified = false;
  }
  sortProjectsAndFillTable();
}

function toggleSortByDateModified() {
  if (sortByDateModified) {
    sortAscending = !sortAscending;
  } else {
    sortByDateModified = true;
    // When sorting by date, sorting descending makes more sense than ascending.
    sortAscending = false;
    sortByName = false;
  }
  sortProjectsAndFillTable();
}

function sortProjectsAndFillTable() {
  if (sortByName) {
    projects.sort(function(project1, project2) {
      return project1.name.localeCompare(project2.name);
    });
  } else if (sortByDateModified) {
    projects.sort(function(project1, project2) {
      return project1.dateModifiedMillis - project2.dateModifiedMillis;
    });
  }
  if (!sortAscending) {
    projects.reverse();
  }

  var table = document.getElementById('projectsTable');
  // Remove all rows except the first one, which contains the column headers.
  for (var i = table.rows.length - 1; i >= 1; i--) {
    table.deleteRow(i);
  }
  for (var i = 0; i < projects.length; i++) {
    var tr = table.insertRow(-1);
    tr.className = 'project_tr';

    var tdTrash = tr.insertCell(-1);
    tdTrash.innerHTML = '<input type="checkbox" id="checkbox_' + i + '" onclick="projectCheckChanged(' + i + ')">';

    var tdName = tr.insertCell(-1);
    tdName.innerHTML = '<div class="project_name" onclick="openProject(' + i + ')">' +
        projects[i].escapedName + '</div>';

    var tdDateModified = tr.insertCell(-1);
    tdDateModified.innerHTML = formatTimestamp(projects[i].dateModifiedMillis);

    var tdEnabled = tr.insertCell(-1);
    tdEnabled.innerHTML = '<input type="checkbox" id="project_enabled_' + i + '" onclick="projectEnabledChanged(' + i + ')"' +
        (projects[i].enabled ? ' checked' : '') + '>';
  }

  var upTriangle = '&#x25B2;';
  var downTriangle = '&#x25BC;';
  var nameSortIndicator = document.getElementById('sortByNameIndicator');
  if (sortByName) {
    nameSortIndicator.innerHTML = (sortAscending ? upTriangle : downTriangle);
  } else {
    nameSortIndicator.innerHTML = '';
  }
  var dateSortIndicator = document.getElementById('sortByDateModifiedIndicator');
  if (sortByDateModified) {
    dateSortIndicator.innerHTML = (sortAscending ? upTriangle : downTriangle);
  } else {
    dateSortIndicator.innerHTML = '';
  }

  checkedProjects = [];
  updateButtons();
}

function formatTimestamp(timestampMillis) {
  var date = new Date(timestampMillis);
  var monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'];
  var formatted = monthNames[date.getMonth()] + '&nbsp;' + date.getDate() +
      ',&nbsp;' + date.getFullYear() + ',&nbsp;';
  if (date.getHours() == 0) {
    formatted += '12';
  } else if (date.getHours() > 12) {
    formatted += (date.getHours() - 12);
  } else {
    formatted += date.getHours();
  }
  formatted += ':';
  if (date.getMinutes() < 10) {
    formatted += '0';
  }
  formatted += date.getMinutes() + ':'
  if (date.getSeconds() < 10) {
    formatted += '0';
  }
  formatted += date.getSeconds() + '&nbsp;';
  if (date.getHours() < 12) {
    formatted += 'AM';
  } else {
    formatted += 'PM';
  }
  return formatted;
}

function newProjectButtonClicked() {
  // Show modal dialog asking for project name.
  document.getElementById('newProjectName').value = '';
  document.getElementById('newProjectNameError').innerHTML = '';
  newProjectNameDialogMode = NEW_PROJECT_NAME_DIALOG_MODE_NEW_PROJECT;
  document.getElementById('newProjectNameDialogTitle').innerHTML = 'Create New Op Mode';
  var select = document.getElementById('newProjectSamplesSelect');
  document.getElementById('newProjectSamplesArea').style.visibility =
      (select.options.length > 1) ? 'visible' : 'hidden';
  document.getElementById('newProjectNameDialog').style.display = 'block';
  document.getElementById('newProjectName').focus();
}

function cancelNewProjectNameDialog() {
  // Close the dialog.
  document.getElementById('newProjectNameDialog').style.display = 'none';
}

function okNewProjectNameDialog() {
  // Validate name for legal characters and for existing project names.
  var newProjectName = document.getElementById('newProjectName').value;
  if (!isValidProjectName(newProjectName)) {
    document.getElementById('newProjectNameError').innerHTML =
        'The project name must only contains alphanumeric<br>characters and !$%&\'()+,-.;=@[]^_{}~.';
    return;
  }
  var nameClash = false;
  for (var i = 0; i < projects.length; i++) {
    if (newProjectName == projects[i].name) {
      nameClash = true;
      break;
    }
  }
  if (nameClash) {
    document.getElementById('newProjectNameError').innerHTML =
        'There is already a project with this name.';
    return;
  }

  switch (newProjectNameDialogMode) {
    case NEW_PROJECT_NAME_DIALOG_MODE_NEW_PROJECT:
      newProjectOk(newProjectName);
      break;
    case NEW_PROJECT_NAME_DIALOG_MODE_RENAME_PROJECT:
      renameProjectOk(projects[checkedProjects[0]].name, newProjectName);
      break;
    case NEW_PROJECT_NAME_DIALOG_MODE_COPY_PROJECT:
      copyProjectOk(projects[checkedProjects[0]].name, newProjectName);
      break;
  }
}

function newProjectOk(newProjectName) {
  var select = document.getElementById('newProjectSamplesSelect');
  var jsonSample = select.options[select.selectedIndex].value;
  var sampleName = jsonSample
      ? JSON.parse(jsonSample).name
      : "";

  // Create new project.
  newProject(newProjectName, sampleName, function(blkFileContent, errorMessage) {
    if (blkFileContent) {
      finishNewOrUploadProject(newProjectName, blkFileContent,
          document.getElementById('newProjectNameError'),
          document.getElementById('newProjectNameDialog'));
    } else {
      document.getElementById('newProjectNameError').innerHTML = errorMessage;
    }
  });
}

function uploadProjectButtonClicked() {
  // Show modal dialog asking for file.
  document.getElementById('uploadProjectFileInput').value = ''
  document.getElementById('uploadProjectError').innerHTML = '';
  document.getElementById('uploadProjectDialogTitle').innerHTML = 'Upload Op Mode';
  document.getElementById('uploadProjectOk').disabled = true;
  document.getElementById('uploadProjectFileInput').onchange = function() {
    var files = document.getElementById('uploadProjectFileInput').files;
    document.getElementById('uploadProjectOk').disabled = (files.length == 0);
  };
  document.getElementById('uploadProjectDialog').style.display = 'block';
}

function cancelUploadProjectDialog() {
  // Close the dialog.
  document.getElementById('uploadProjectDialog').style.display = 'none';
}

function okUploadProjectDialog() {
  var file = document.getElementById('uploadProjectFileInput').files[0];
  var uploadProjectName = makeUploadProjectName(file.name);
  var reader = new FileReader();
  reader.onload = function(event) {
    // Get the blocks from the uploaded file.
    var blkFileContent = event.target.result;
    finishNewOrUploadProject(uploadProjectName, blkFileContent,
        document.getElementById('uploadProjectError'),
        document.getElementById('uploadProjectDialog'));
  };
  reader.readAsText(file);
}

function makeUploadProjectName(uploadFileName) {
  var lastDot = uploadFileName.lastIndexOf('.');
  var preferredName = (lastDot == -1)
      ? uploadFileName
      : uploadFileName.substr(0, lastDot);
  var name = preferredName; // No suffix.
  var suffix = 0;
  while (true) {
    var nameClash = false;
    for (var i = 0; i < projects.length; i++) {
      if (name == projects[i].name) {
        nameClash = true;
        break;
      }
    }
    if (!nameClash) {
      return name;
    }
    suffix++;
    name = preferredName + suffix;
  }
}

function finishNewOrUploadProject(projectName, blkFileContent, errorElement, dialogElement) {
  // The blocks content is up to and including the first </xml>.
  var i = blkFileContent.indexOf('</xml>');
  var blocksContent = blkFileContent.substring(0, i + 6);
  var dom = '';
  try {
    dom = Blockly.Xml.textToDom(blocksContent);
  } catch (e) {
    errorElement.innerHTML = 'Error: Invalid blocks. ' + e;
    return;
  }
  // Create a headless workspace to generate the JavaScript.
  var jsFileContent = '';
  try {
    var workspace = new Blockly.Workspace();
    // For consistency with previous versions, we explicitly set oneBasedIndex to true.
    workspace.options.oneBasedIndex = true;
    Blockly.Xml.domToWorkspace(dom, workspace);
    jsFileContent = Blockly.JavaScript.workspaceToCode(workspace);
  } catch (e) {
    errorElement.innerHTML = 'Error: Could not generate code for blocks. ' + e;
    return;
  }
  if (!jsFileContent) {
    errorElement.innerHTML = 'Error: No code generated for blocks.';
    return;
  }

  saveProject(projectName, blkFileContent, jsFileContent, function(success, errorMessage) {
    if (success) {
      // Close the dialog.
      dialogElement.style.display = 'none';
      openProjectBlocks(projectName);
    } else {
      errorElement.innerHTML = errorMessage;
    }
  });
}

function renameProjectButtonClicked() {
  // Show modal dialog asking for project name.
  document.getElementById('newProjectName').value = projects[checkedProjects[0]].name;
  document.getElementById('newProjectNameError').innerHTML = '';
  newProjectNameDialogMode = NEW_PROJECT_NAME_DIALOG_MODE_RENAME_PROJECT;
  document.getElementById('newProjectNameDialogTitle').innerHTML = 'Rename Selected Op Mode';
  document.getElementById('newProjectSamplesArea').style.visibility = 'hidden';
  document.getElementById('newProjectNameDialog').style.display = 'block';
  document.getElementById('newProjectName').focus();
}

function renameProjectOk(oldProjectName, newProjectName) {
  renameProject(oldProjectName, newProjectName, function(success, errorMessage) {
    if (success) {
      // Close the dialog.
      document.getElementById('newProjectNameDialog').style.display = 'none';
      initializeProjects();
    } else {
      document.getElementById('newProjectNameError').innerHTML = errorMessage;
    }
  });
}

function copyProjectButtonClicked() {
  // Show modal dialog asking for project name.
  document.getElementById('newProjectName').value = projects[checkedProjects[0]].name;
  document.getElementById('newProjectNameError').innerHTML = '';
  newProjectNameDialogMode = NEW_PROJECT_NAME_DIALOG_MODE_COPY_PROJECT;
  document.getElementById('newProjectNameDialogTitle').innerHTML = 'Copy Selected Op Mode';
  document.getElementById('newProjectSamplesArea').style.visibility = 'hidden';
  document.getElementById('newProjectNameDialog').style.display = 'block';
  document.getElementById('newProjectName').focus();
}

function copyProjectOk(oldProjectName, newProjectName) {
  copyProject(oldProjectName, newProjectName, function(success, errorMessage) {
    if (success) {
      // Close the dialog.
      document.getElementById('newProjectNameDialog').style.display = 'none';
      initializeProjects();
    } else {
      document.getElementById('newProjectNameError').innerHTML = errorMessage;
    }
  });
}

function downloadProjectsButtonClicked() {
  for (var i = 0; i < projects.length; i++) {
    var checkbox = document.getElementById('checkbox_' + i);
    if (checkbox.checked) {
      downloadProject(projects[i].name);
    }
  }
}

function downloadProject(projectName) {
  fetchBlkFileContent(projectName, function(blkFileContent, errorMessage) {
    if (blkFileContent) {
      var a = document.getElementById('download_link');
      a.href = 'data:text/plain;charset=utf-8,' + encodeURIComponent(blkFileContent);
      a.download = projectName + '.blk';
      a.click();
    } else {
      alert(errorMessage);
    }
  });
}

var starDelimitedDeleteProjectNames = '';

function deleteProjectsButtonClicked() {
  starDelimitedDeleteProjectNames = '';
  var table = document.getElementById('deleteProjectsTable');
  // Remove all rows.
  for (var i = table.rows.length - 1; i >= 0; i--) {
    table.deleteRow(i);
  }
  // Gather the checked projects.
  var delimiter = '';
  for (var i = 0; i < projects.length; i++) {
    var checkbox = document.getElementById('checkbox_' + i);
    if (checkbox.checked) {
      starDelimitedDeleteProjectNames += delimiter + projects[i].name;
      delimiter = '*';
      // Insert project name into the table.
      table.insertRow(-1).insertCell(-1).innerHTML = projects[i].escapedName;
    }
  }
  if (table.rows.length > 0) {
    // Show modal dialog confirming that the user wants to delete the projects.
    document.getElementById('deleteProjectsDialog').style.display = 'block';
  }
}

function noDeleteProjectsDialog() {
  // Close the dialog.
  document.getElementById('deleteProjectsDialog').style.display = 'none';
}

function yesDeleteProjectsDialog() {
  // Close the dialog.
  document.getElementById('deleteProjectsDialog').style.display = 'none';
  deleteProjects(starDelimitedDeleteProjectNames, function(success, errorMessage) {
    starDelimitedDeleteProjectNames = '';
    if (success) {
      initializeProjects();
    } else {
      alert(errorMessage);
    }
  });
}

function projectCheckAllChanged(i) {
  var checkboxAll = document.getElementById('checkbox_all');
  if (checkedProjects.length == 0) {
    // Check all.
    checkedProjects = [];
    for (var i = 0; i < projects.length; i++) {
      var checkbox = document.getElementById('checkbox_' + i);
      checkbox.checked = true;
      checkedProjects.push(i);
    }
    checkboxAll.checked = true;
  } else {
    // Check none.
    checkedProjects = [];
    for (var i = 0; i < projects.length; i++) {
      var checkbox = document.getElementById('checkbox_' + i);
      checkbox.checked = false;
    }
    checkboxAll.checked = false;
  }
  updateButtons();
}

function projectCheckChanged(i) {
  var checkbox = document.getElementById('checkbox_' + i);
  if (checkbox.checked) {
    if (checkedProjects.indexOf(i) == -1) {
      checkedProjects.push(i);
    }
  } else {
    var index = checkedProjects.indexOf(i);
    checkedProjects.splice(index, 1);
  }
  updateButtons();
}

function updateButtons() {
  var renameProjectButton = document.getElementById('renameProjectButton');
  renameProjectButton.disabled = (checkedProjects.length != 1);
  var copyProjectButton = document.getElementById('copyProjectButton');
  copyProjectButton.disabled = (checkedProjects.length != 1);
  var downloadProjectsButton = document.getElementById('downloadProjectsButton');
  downloadProjectsButton.disabled = (checkedProjects.length == 0);
  var deleteProjectsButton = document.getElementById('deleteProjectsButton');
  deleteProjectsButton.disabled = (checkedProjects.length == 0);
}

function openProject(i) {
  if (i >= 0 && i < projects.length) {
    openProjectBlocks(projects[i].name);
  }
}

function projectEnabledChanged(i) {
  if (i >= 0 && i < projects.length) {
    var enabledCheckbox = document.getElementById('project_enabled_' + i);
    var isChecked = enabledCheckbox.checked;
    enableProject(projects[i].name, isChecked, function(success, errorMessage) {
      if (success) {
        projects[i].enabled = isChecked;
      } else {
        // Undo the checkbox change in the UI.
        enabledCheckbox.checked = projects[i].enabled;
        console.log(errorMessage);
      }
    });
  }
}
