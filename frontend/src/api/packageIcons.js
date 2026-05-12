// Maps installed package names to their icon files
// Same logic as DevicePanelStateFactory in the Swing app

const PACKAGE_ICON_MAP = {
  'com.smithmicro.tmobile.familymode.test': 'tmo.png',
  'com.tmobile.familycontrols': 'tmo.png',
  'com.smithmicro.safepath.dish.test': 'dish.png',
  'com.smithmicro.safepath.dish.kid.test': 'dish.png',
  'com.smithmicro.safepath.family': 'product.png',
  'com.smithmicro.safepath.family.child': 'product.png',
  'com.smithmicro.safepath.family.light': 'Senior.png',
  'com.smithmicro.safepath.family.speakeasy': 'Senior.png',
  'com.smithmicro.cci.test': 'Senior.png',
  'com.smithmicro.att.securefamily': 'att.png',
  'com.wavemarket.waplauncher': 'att.png',
  'com.att.securefamilycompanion': 'att.png',
  'com.smithmicro.sprint.safeandfound.test': 'sprint.png',
  'com.sprint.safefound': 'sprint.png',
  'com.smithmicro.orangespain.test': 'toyo.png',
  'com.orange.es.TuYo': 'toyo.png',
};

const PACKAGE_LABEL_MAP = {
  'com.smithmicro.tmobile.familymode.test': 'FamilyMode',
  'com.tmobile.familycontrols': 'FamilyMode',
  'com.smithmicro.safepath.dish.test': 'Dish',
  'com.smithmicro.safepath.dish.kid.test': 'Dish',
  'com.smithmicro.safepath.family': 'SPFamily',
  'com.smithmicro.safepath.family.child': 'SPFamily',
  'com.smithmicro.safepath.family.light': 'SPFamily',
  'com.smithmicro.safepath.family.speakeasy': 'SPFamily',
  'com.smithmicro.cci.test': 'SpeakEasy',
  'com.smithmicro.att.securefamily': 'SF IAP',
  'com.wavemarket.waplauncher': 'SF EAP',
  'com.att.securefamilycompanion': 'SF Companion',
  'com.smithmicro.sprint.safeandfound.test': 'Safe&Found',
  'com.sprint.safefound': 'Safe&Found',
  'com.smithmicro.orangespain.test': 'TuYo',
  'com.orange.es.TuYo': 'TuYo',
};

export function getIconForPackage(packageName) {
  if (!packageName) return 'Android.png';
  return PACKAGE_ICON_MAP[packageName] || 'Android.png';
}

export function getLabelForPackage(packageName) {
  if (!packageName) return 'Not Installed';
  return PACKAGE_LABEL_MAP[packageName] || 'Installed';
}
