/**
 * @fileoverview FTC robot blocks related to TensorFlow Object Detection for SKYSTONE (2019-2020)
 * @author lizlooney@google.com (Liz Looney)
 */

// The following are generated dynamically in HardwareUtil.fetchJavaScriptForHardware():
// createSkyStoneTfodLabelDropdown
// SKY_STONE_TFOD_LABEL_TOOLTIPS
// tfodSkyStoneIdentifierForJavaScript
// vuforiaSkyStoneIdentifierForJavaScript
// The following are defined in vars.js:
// createNonEditableField
// functionColor
// getPropertyColor

Blockly.Blocks['tfodSkyStone_initialize'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionSKYSTONE'))
        .appendField('.')
        .appendField(createNonEditableField('initialize'));
    this.appendValueInput('MINIMUM_CONFIDENCE').setCheck('Number')
        .appendField('minimumConfidence')
        .setAlign(Blockly.ALIGN_RIGHT);
    this.appendValueInput('USE_OBJECT_TRACKER').setCheck('Boolean')
        .appendField('useObjectTracker')
        .setAlign(Blockly.ALIGN_RIGHT);
    this.appendValueInput('ENABLE_CAMERA_MONITORING').setCheck('Boolean')
        .appendField('enableCameraMonitoring')
        .setAlign(Blockly.ALIGN_RIGHT);
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour(functionColor);
    this.setTooltip('Initialize TensorFlow Object Detection for SKYSTONE.');
    this.getFtcJavaInputType = function(inputName) {
      switch (inputName) {
        case 'MINIMUM_CONFIDENCE':
          return 'double';
      }
      return '';
    };
  }
};

Blockly.JavaScript['tfodSkyStone_initialize'] = function(block) {
  return tfod_initialize_JavaScript(block, tfodSkyStoneIdentifierForJavaScript,
      vuforiaSkyStoneIdentifierForJavaScript);
};

Blockly.FtcJava['tfodSkyStone_initialize'] = function(block) {
  return tfod_initialize_FtcJava(block, 'TfodSkyStone', 'VuforiaSkyStone');
};

Blockly.Blocks['tfodSkyStone_activate'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionSKYSTONE'))
        .appendField('.')
        .appendField(createNonEditableField('activate'));
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour(functionColor);
    this.setTooltip('Activates object detection.');
  }
};

Blockly.JavaScript['tfodSkyStone_activate'] = function(block) {
  return tfod_activate_JavaScript(block, tfodSkyStoneIdentifierForJavaScript);
};

Blockly.FtcJava['tfodSkyStone_activate'] = function(block) {
  return tfod_activate_FtcJava(block, 'TfodSkyStone');
};

Blockly.Blocks['tfodSkyStone_deactivate'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionSKYSTONE'))
        .appendField('.')
        .appendField(createNonEditableField('deactivate'));
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour(functionColor);
    this.setTooltip('Deactivates object detection.');
  }
};

Blockly.JavaScript['tfodSkyStone_deactivate'] = function(block) {
  return tfod_deactivate_JavaScript(block, tfodSkyStoneIdentifierForJavaScript);
};

Blockly.FtcJava['tfodSkyStone_deactivate'] = function(block) {
  return tfod_deactivate_FtcJava(block, 'TfodSkyStone');
};

Blockly.Blocks['tfodSkyStone_setClippingMargins'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionSKYSTONE'))
        .appendField('.')
        .appendField(createNonEditableField('setClippingMargins'));
    this.appendValueInput('LEFT').setCheck('Number')
        .appendField('left')
        .setAlign(Blockly.ALIGN_RIGHT);
    this.appendValueInput('TOP').setCheck('Number')
        .appendField('top')
        .setAlign(Blockly.ALIGN_RIGHT);
    this.appendValueInput('RIGHT').setCheck('Number')
        .appendField('right')
        .setAlign(Blockly.ALIGN_RIGHT);
    this.appendValueInput('BOTTOM').setCheck('Number')
        .appendField('bottom')
        .setAlign(Blockly.ALIGN_RIGHT);
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour(functionColor);
    this.setTooltip('Sets the number of pixels to obscure on the left, top, right, and bottom ' +
        'edges of each image passed to the TensorFlow object detector. The size of the images ' +
        'are not changed, but the pixels in the margins are colored black.');
    this.getFtcJavaInputType = function(inputName) {
      switch (inputName) {
        case 'LEFT':
        case 'TOP':
        case 'BOTTOM':
        case 'RIGHT':
          return 'int';
      }
      return '';
    };
  }
};

Blockly.JavaScript['tfodSkyStone_setClippingMargins'] = function(block) {
  return tfod_setClippingMargins_JavaScript(block, tfodSkyStoneIdentifierForJavaScript);
};

Blockly.FtcJava['tfodSkyStone_setClippingMargins'] = function(block) {
  return tfod_setClippingMargins_FtcJava(block, 'TfodSkyStone');
};

Blockly.Blocks['tfodSkyStone_getRecognitions'] = {
  init: function() {
    this.setOutput(true, 'Array');
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionSKYSTONE'))
        .appendField('.')
        .appendField(createNonEditableField('getRecognitions'));
    this.setColour(functionColor);
    this.setTooltip('Returns a List of the current recognitions.');
    this.getFtcJavaOutputType = function() {
      return 'List<Recognition>';
    };
  }
};

Blockly.JavaScript['tfodSkyStone_getRecognitions'] = function(block) {
  return tfod_getRecognitions_JavaScript(block, tfodSkyStoneIdentifierForJavaScript);
};

Blockly.FtcJava['tfodSkyStone_getRecognitions'] = function(block) {
  return tfod_getRecognitions_FtcJava(block, 'TfodSkyStone');
};

Blockly.Blocks['tfodSkyStone_typedEnum_label'] = {
  init: function() {
    this.setOutput(true, 'String');
    this.appendDummyInput()
        .appendField(createNonEditableField('Label'))
        .appendField('.')
        .appendField(createSkyStoneTfodLabelDropdown(), 'LABEL');
    this.setColour(getPropertyColor);
    // Assign 'this' to a variable for use in the tooltip closure below.
    var thisBlock = this;
    var TOOLTIPS = SKY_STONE_TFOD_LABEL_TOOLTIPS;
    this.setTooltip(function() {
      var key = thisBlock.getFieldValue('LABEL');
      for (var i = 0; i < TOOLTIPS.length; i++) {
        if (TOOLTIPS[i][0] == key) {
          return TOOLTIPS[i][1];
        }
      }
      return '';
    });
  }
};

Blockly.JavaScript['tfodSkyStone_typedEnum_label'] = function(block) {
  return tfod_typedEnum_label_JavaScript(block);
};

Blockly.FtcJava['tfodSkyStone_typedEnum_label'] = function(block) {
  return tfod_typedEnum_label_FtcJava(block);
};
