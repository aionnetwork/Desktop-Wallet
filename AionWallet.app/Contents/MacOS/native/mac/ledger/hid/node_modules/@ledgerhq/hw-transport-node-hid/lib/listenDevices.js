"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _set = require("babel-runtime/core-js/set");

var _set2 = _interopRequireDefault(_set);

var _toConsumableArray2 = require("babel-runtime/helpers/toConsumableArray");

var _toConsumableArray3 = _interopRequireDefault(_toConsumableArray2);

var _events = require("events");

var _events2 = _interopRequireDefault(_events);

var _usb = require("usb");

var _usb2 = _interopRequireDefault(_usb);

var _debounce = require("lodash/debounce");

var _debounce2 = _interopRequireDefault(_debounce);

var _getDevices = require("./getDevices");

var _getDevices2 = _interopRequireDefault(_getDevices);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

exports.default = function (delay, listenDevicesPollingSkip, debug) {
  var events = new _events2.default();
  events.setMaxListeners(0);

  var listDevices = (0, _getDevices2.default)();

  var flatDevice = function flatDevice(d) {
    return d.path;
  };

  var getFlatDevices = function getFlatDevices() {
    return [].concat((0, _toConsumableArray3.default)(new _set2.default((0, _getDevices2.default)().map(function (d) {
      return flatDevice(d);
    }))));
  };

  var getDeviceByPaths = function getDeviceByPaths(paths) {
    return listDevices.find(function (d) {
      return paths.includes(flatDevice(d));
    });
  };

  var lastDevices = getFlatDevices();

  var poll = function poll() {
    if (!listenDevicesPollingSkip()) {
      debug("Polling for added or removed devices");

      var changeFound = false;
      var currentDevices = getFlatDevices();
      var newDevices = currentDevices.filter(function (d) {
        return !lastDevices.includes(d);
      });

      if (newDevices.length > 0) {
        debug("New device found:", newDevices);

        listDevices = (0, _getDevices2.default)();
        events.emit("add", getDeviceByPaths(newDevices));

        changeFound = true;
      } else {
        debug("No new device found");
      }

      var removeDevices = lastDevices.filter(function (d) {
        return !currentDevices.includes(d);
      });

      if (removeDevices.length > 0) {
        debug("Removed device found:", removeDevices);

        events.emit("remove", getDeviceByPaths(removeDevices));
        listDevices = listDevices.filter(function (d) {
          return !removeDevices.includes(flatDevice(d));
        });

        changeFound = true;
      } else {
        debug("No removed device found");
      }

      if (changeFound) {
        lastDevices = currentDevices;
      }
    } else {
      debug("Polling skipped, re-debouncing");
      debouncedPoll();
    }
  };

  var debouncedPoll = (0, _debounce2.default)(poll, delay);

  var attachDetected = function attachDetected(device) {
    debug("Device add detected:", device);

    debouncedPoll();
  };
  _usb2.default.on("attach", attachDetected);
  debug("attach listener added");

  var detachDetected = function detachDetected(device) {
    debug("Device removal detected:", device);

    debouncedPoll();
  };
  _usb2.default.on("detach", detachDetected);
  debug("detach listener added");

  return {
    stop: function stop() {
      debug("Stop received, removing listeners and cancelling pending debounced polls");
      debouncedPoll.cancel();
      _usb2.default.removeListener("attach", attachDetected);
      _usb2.default.removeListener("detach", detachDetected);
    },
    events: events
  };
};
//# sourceMappingURL=listenDevices.js.map