/**
 * @fileoverview FTC robot blocks related to TensorFlow Object Detection for Rover Ruckus (2018-2019)
 * @author lizlooney@google.com (Liz Looney)
 */

// The following are generated dynamically in HardwareUtil.fetchJavaScriptForHardware():
// createRoverRuckusTfodLabelDropdown
// ROVER_RUCKUS_TFOD_LABEL_TOOLTIPS
// tfodRoverRuckusIdentifierForJavaScript
// vuforiaRoverRuckusIdentifierForJavaScript
// The following are defined in vars.js:
// createNonEditableField
// functionColor
// getPropertyColor

Blockly.Blocks['tfodRoverRuckus_initialize'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionRoverRuckus'))
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
    this.setTooltip('Initialize TensorFlow Object Detection for Rover Ruckus.');
    this.getFtcJavaInputType = function(inputName) {
      switch (inputName) {
        case 'MINIMUM_CONFIDENCE':
          return 'double';
      }
      return '';
    };
  }
};

Blockly.JavaScript['tfodRoverRuckus_initialize'] = function(block) {
  return tfod_initialize_JavaScript(block, tfodRoverRuckusIdentifierForJavaScript,
      vuforiaRoverRuckusIdentifierForJavaScript);
};

Blockly.FtcJava['tfodRoverRuckus_initialize'] = function(block) {
  return tfod_initialize_FtcJava(block, 'TfodRoverRuckus', 'VuforiaRoverRuckus');
};

Blockly.Blocks['tfodRoverRuckus_activate'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionRoverRuckus'))
        .appendField('.')
        .appendField(createNonEditableField('activate'));
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour(functionColor);
    this.setTooltip('Activates object detection.');
  }
};

Blockly.JavaScript['tfodRoverRuckus_activate'] = function(block) {
  return tfod_activate_JavaScript(block, tfodRoverRuckusIdentifierForJavaScript);
};

Blockly.FtcJava['tfodRoverRuckus_activate'] = function(block) {
  return tfod_activate_FtcJava(block, 'TfodRoverRuckus');
};

Blockly.Blocks['tfodRoverRuckus_deactivate'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionRoverRuckus'))
        .appendField('.')
        .appendField(createNonEditableField('deactivate'));
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour(functionColor);
    this.setTooltip('Deactivates object detection.');
  }
};

Blockly.JavaScript['tfodRoverRuckus_deactivate'] = function(block) {
  return tfod_deactivate_JavaScript(block, tfodRoverRuckusIdentifierForJavaScript);
};

Blockly.FtcJava['tfodRoverRuckus_deactivate'] = function(block) {
  return tfod_deactivate_FtcJava(block, 'TfodRoverRuckus');
};

Blockly.Blocks['tfodRoverRuckus_setClippingMargins'] = {
  init: function() {
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionRoverRuckus'))
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

Blockly.JavaScript['tfodRoverRuckus_setClippingMargins'] = function(block) {
  return tfod_setClippingMargins_JavaScript(block, tfodRoverRuckusIdentifierForJavaScript);
};

Blockly.FtcJava['tfodRoverRuckus_setClippingMargins'] = function(block) {
  return tfod_setClippingMargins_FtcJava(block, 'TfodRoverRuckus');
};

Blockly.Blocks['tfodRoverRuckus_getRecognitions'] = {
  init: function() {
    this.setOutput(true, 'Array');
    this.appendDummyInput()
        .appendField('call')
        .appendField(createNonEditableField('TensorFlowObjectDetectionRoverRuckus'))
        .appendField('.')
        .appendField(createNonEditableField('getRecognitions'));
    this.setColour(functionColor);
    this.setTooltip('Returns a List of the current recognitions.');
    this.getFtcJavaOutputType = function() {
      return 'List<Recognition>';
    };
  }
};

Blockly.JavaScript['tfodRoverRuckus_getRecognitions'] = function(block) {
  return tfod_getRecognitions_JavaScript(block, tfodRoverRuckusIdentifierForJavaScript);
};

Blockly.FtcJava['tfodRoverRuckus_getRecognitions'] = function(block) {
  return tfod_getRecognitions_FtcJava(block, 'TfodRoverRuckus');
};

Blockly.Blocks['tfodRoverRuckus_typedEnum_label'] = {
  init: function() {
    this.setOutput(true, 'String');
    this.appendDummyInput()
        .appendField(createNonEditableField('Label'))
        .appendField('.')
        .appendField(createRoverRuckusTfodLabelDropdown(), 'LABEL');
    this.setColour(getPropertyColor);
    // Assign 'this' to a variable for use in the tooltip closure below.
    var thisBlock = this;
    var TOOLTIPS = ROVER_RUCKUS_TFOD_LABEL_TOOLTIPS;
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

Blockly.JavaScript['tfodRoverRuckus_typedEnum_label'] = function(block) {
  return tfod_typedEnum_label_JavaScript(block);
};

Blockly.FtcJava['tfodRoverRuckus_typedEnum_label'] = function(block) {
  return tfod_typedEnum_label_FtcJava(block);
};
