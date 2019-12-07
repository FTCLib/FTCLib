/**
 * @fileoverview Functions to generate code for the initialize method call for TensorFlow Object Detection
 * @author lizlooney@google.com (Liz Looney)
 */

// The following are generated dynamically in HardwareUtil.fetchJavaScriptForHardware():
// The following are defined in vars.js:

function tfod_initialize_JavaScript(block, identifier, vuforiaIdentifier) {
  var minimumConfidence = Blockly.JavaScript.valueToCode(
      block, 'MINIMUM_CONFIDENCE', Blockly.JavaScript.ORDER_COMMA);
  var useObjectTracker = Blockly.JavaScript.valueToCode(
      block, 'USE_OBJECT_TRACKER', Blockly.JavaScript.ORDER_COMMA);
  var enableCameraMonitoring = Blockly.JavaScript.valueToCode(
      block, 'ENABLE_CAMERA_MONITORING', Blockly.JavaScript.ORDER_COMMA);
  return identifier + '.initialize(' + vuforiaIdentifier + ', ' +
      minimumConfidence + ', ' + useObjectTracker + ', ' + enableCameraMonitoring + ');\n';
}

function tfod_initialize_FtcJava(block, className, vuforiaClassName) {
  var identifier = Blockly.FtcJava.importDeclareAssign_(block, null, className);
  var vuforiaIdentifier = Blockly.FtcJava.importDeclareAssign_(block, null, vuforiaClassName);
  var minimumConfidence = Blockly.FtcJava.valueToCode(
      block, 'MINIMUM_CONFIDENCE', Blockly.FtcJava.ORDER_COMMA);
  if (isNaN(minimumConfidence)) {
    minimumConfidence = '(float) (' + minimumConfidence + ')';
  } else {
    minimumConfidence = minimumConfidence + 'F';
  }
  var useObjectTracker = Blockly.FtcJava.valueToCode(
      block, 'USE_OBJECT_TRACKER', Blockly.FtcJava.ORDER_COMMA);
  var enableCameraMonitoring = Blockly.FtcJava.valueToCode(
      block, 'ENABLE_CAMERA_MONITORING', Blockly.FtcJava.ORDER_COMMA);
  return identifier + '.initialize(' + vuforiaIdentifier + ', ' +
      minimumConfidence + ', ' + useObjectTracker + ', ' + enableCameraMonitoring + ');\n';
}

function tfod_activate_JavaScript(block, identifier) {
  return identifier + '.activate();\n';
}

function tfod_activate_FtcJava(block, className) {
  var identifier = Blockly.FtcJava.importDeclareAssign_(block, null, className);
  return identifier + '.activate();\n';
}

function tfod_deactivate_JavaScript(block, identifier) {
  return identifier + '.deactivate();\n';
}

function tfod_deactivate_FtcJava(block, className) {
  var identifier = Blockly.FtcJava.importDeclareAssign_(block, null, className);
  return identifier + '.deactivate();\n';
}

function tfod_setClippingMargins_JavaScript(block, identifier) {
  var left = Blockly.JavaScript.valueToCode(block, 'LEFT', Blockly.JavaScript.ORDER_COMMA);
  var top = Blockly.JavaScript.valueToCode(block, 'TOP', Blockly.JavaScript.ORDER_COMMA);
  var right = Blockly.JavaScript.valueToCode(block, 'RIGHT', Blockly.JavaScript.ORDER_COMMA);
  var bottom = Blockly.JavaScript.valueToCode(block, 'BOTTOM', Blockly.JavaScript.ORDER_COMMA);
  return identifier + '.setClippingMargins(' +
      left + ', ' + top + ', ' + right + ', ' + bottom + ');\n';
}

function tfod_setClippingMargins_FtcJava(block, className) {
  var identifier = Blockly.FtcJava.importDeclareAssign_(block, null, className);
  var left = Blockly.FtcJava.valueToCode(block, 'LEFT', Blockly.FtcJava.ORDER_COMMA);
  var top = Blockly.FtcJava.valueToCode(block, 'TOP', Blockly.FtcJava.ORDER_COMMA);
  var right = Blockly.FtcJava.valueToCode(block, 'RIGHT', Blockly.FtcJava.ORDER_COMMA);
  var bottom = Blockly.FtcJava.valueToCode(block, 'BOTTOM', Blockly.FtcJava.ORDER_COMMA);
  return identifier + '.setClippingMargins(' +
      left + ', ' + top + ', ' + right + ', ' + bottom + ');\n';
}

function tfod_getRecognitions_JavaScript(block, identifier) {
  var code = 'JSON.parse(' + identifier + '.getRecognitions())';
  return [code, Blockly.JavaScript.ORDER_FUNCTION_CALL];
}

function tfod_getRecognitions_FtcJava(block, className) {
  var identifier = Blockly.FtcJava.importDeclareAssign_(block, null, className);
  var code = identifier + '.getRecognitions()';
  return [code, Blockly.FtcJava.ORDER_FUNCTION_CALL];
}

function tfod_typedEnum_label_JavaScript(block) {
  var code = '"' + block.getFieldValue('LABEL') + '"';
  return [code, Blockly.JavaScript.ORDER_ATOMIC];
}

function tfod_typedEnum_label_FtcJava(block) {
  // Even in Java, a label is actually just a string, not an enum.
  var code = '"' + block.getFieldValue('LABEL') + '"';
  return [code, Blockly.FtcJava.ORDER_ATOMIC];
}
