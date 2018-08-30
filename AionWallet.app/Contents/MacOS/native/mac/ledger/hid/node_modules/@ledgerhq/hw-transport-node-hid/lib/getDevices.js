"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = getDevices;

var _nodeHid = require("node-hid");

var _nodeHid2 = _interopRequireDefault(_nodeHid);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var filterInterface = function filterInterface(device) {
  return ["win32", "darwin"].includes(process.platform) ? device.usagePage === 0xffa0 : device.interface === 0;
};
function getDevices() {
  return _nodeHid2.default.devices(0x2c97, 0x0).filter(filterInterface);
}
//# sourceMappingURL=getDevices.js.map