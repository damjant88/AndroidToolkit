// Static icon imports — bundled by webpack, always available, never flicker
import productIcon from '../icons/product.png';
import attIcon from '../icons/att.png';
import sprintIcon from '../icons/sprint.png';
import tmoIcon from '../icons/tmo.png';
import seniorIcon from '../icons/Senior.png';
import toyoIcon from '../icons/toyo.png';
import dishIcon from '../icons/dish.png';
import spcIcon from '../icons/spc.png';

export const PROJECT_ICONS = {
  'product.png': productIcon,
  'att.png': attIcon,
  'sprint.png': sprintIcon,
  'tmo.png': tmoIcon,
  'Senior.png': seniorIcon,
  'toyo.png': toyoIcon,
  'dish.png': dishIcon,
  'spc.png': spcIcon,
};

export function getStaticIcon(iconName) {
  return PROJECT_ICONS[iconName] || productIcon;
}
