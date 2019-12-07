/**
 * @fileoverview Toolbox utilities.
 * @author lizlooney@google.com (Liz Looney)
 */

function addToolboxIcons(workspace) {
  addToolboxIconsForChildren(workspace.toolbox_.tree_.getChildren());
}

function addToolboxIconsForChildren(children) {
  for (var i = 0, child; child = children[i]; i++) {
    if (child.getChildCount() > 0) {
      addToolboxIconsForChildren(child.getChildren());
    } else {
      var iconClass = getIconClass(child.getText());
      if (iconClass) {
        child.setIconClass('toolbox-node-icon ' + iconClass);
      }
    }
  }
}
