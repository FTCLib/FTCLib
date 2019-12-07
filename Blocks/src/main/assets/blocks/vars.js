var titlePrefix = 'FTC';
var currentProjectName;
var currentClassName = '';
var isDirty = false;
var missingHardware = [];
var blockIdsWithMissingHardware = [];
var WarningBits = {
  NONE: 0,
  MISSING_HARDWARE: 1 << 0,
  RELIC_RECOVERY: 1 << 1,
  ROVER_RUCKUS: 1 << 2,
};
var mouseX, mouseY;
var previousClipboardXml;
var savedClipboardContent;

var showJavaCheckbox;
var javaArea;
var javaContent;
var parentArea;
var blocksAndBannerArea;
var blocklyArea;
var blocklyDiv;
var banner;
var bannerText;
var bannerButton;
var split;
var workspace;

var projectEnabled = true;

var setPropertyColor = 147;
var getPropertyColor = 151;
var functionColor = 289;
var commentColor = 200;

var identifierFieldNames = ['IDENTIFIER', 'IDENTIFIER1', 'IDENTIFIER2'];

function createNonEditableField(label) {
  var field = new Blockly.FieldTextInput(label);
  field.CURSOR = '';
  field.showEditor_ = function(opt_quietInput) {};
  return field;
}

function createFieldDropdown(choices) {
  if (choices.length == 0) {
    return createNonEditableField('');
  }
  // Disable validation. We'll show a warning if the newValue is not in the choices. This can
  // happen if the hardware configuration has changed.
  var field = new Blockly.FieldDropdown(choices);
  field.doClassValidation_ = function(newValue) {
    return newValue;
  };
  return field;
}

function isJavaIdentifierStart(c) {
  return /[a-zA-Z$_]/.test(c);
}

function isJavaIdentifierPart(c) {
  return /[a-zA-Z0-9$_]/.test(c);
}

function makeIdentifier(deviceName) {
  var identifier = '';

  var c = deviceName.charAt(0);
  if (isJavaIdentifierStart(c)) {
    identifier += c;
  } else if (isJavaIdentifierPart(c)) {
    identifier += ('_' + c);
  }

  for (var i = 1; i < deviceName.length; i++) {
    c = deviceName.charAt(i);
    if (isJavaIdentifierPart(c)) {
      identifier += c;
    }
  }
  return identifier;
}

function escapeHtml(text) {
  var out = '';
  for (var i = 0; i < text.length; i++) {
    var c = text.charAt(i);
    if (c == ' ') {
      out += '&nbsp;';
    } else if (c == '<') {
      out += '&lt;'
    } else if (c == '>') {
      out += '&gt;';
    } else if (c == '&') {
      out += '&amp;';
    } else if (c > 0x7E || c < ' ') {
      out += ('&#' + text.charCodeAt(i) + ';');
    } else {
      out += c;
    }
  }
  return out;
}

function formatExtraXml(flavor, group, enabled) {
  return '<?xml version=\'1.0\' encoding=\'UTF-8\' standalone=\'yes\' ?>' +
      '<Extra>' +
      '<OpModeMeta flavor="' + flavor + '" group="' + group + '" />' +
      '<Enabled value="' + enabled + '" />' +
      '</Extra> ';
}

function parseExtraXml(blkFileContent) {
  var extra = Object.create(null);
  extra['flavor'] = 'TELEOP';
  extra['group'] = '';
  extra['enabled'] = true;

  // The blocks content is up to and including the first </xml>.
  var i = blkFileContent.indexOf('</xml>');
  // The extra xml content is after the first </xml>.
  // Set OpModeMeta and Enabled UI components.
  var extraXml = blkFileContent.substring(i + 6); // 6 is length of </xml>
  if (extraXml.length > 0) {
    var parser = new DOMParser();
    var xmlDoc = parser.parseFromString(extraXml, 'text/xml');
    var opModeMetaElements = xmlDoc.getElementsByTagName('OpModeMeta');
    if (opModeMetaElements.length >= 1) {
      extra['flavor'] = opModeMetaElements[0].getAttribute('flavor');
      extra['group'] = opModeMetaElements[0].getAttribute('group');
    }
    var enabledElements = xmlDoc.getElementsByTagName('Enabled');
    if (enabledElements.length >= 1) {
      var enabledString = enabledElements[0].getAttribute('value');
      if (enabledString) {
        extra['enabled'] = (enabledString == 'true');
      }
    }
  }
  return extra;
}
