"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _getIterator2 = require("babel-runtime/core-js/get-iterator");

var _getIterator3 = _interopRequireDefault(_getIterator2);

var _regenerator = require("babel-runtime/regenerator");

var _regenerator2 = _interopRequireDefault(_regenerator);

var _asyncToGenerator2 = require("babel-runtime/helpers/asyncToGenerator");

var _asyncToGenerator3 = _interopRequireDefault(_asyncToGenerator2);

var _getPrototypeOf = require("babel-runtime/core-js/object/get-prototype-of");

var _getPrototypeOf2 = _interopRequireDefault(_getPrototypeOf);

var _classCallCheck2 = require("babel-runtime/helpers/classCallCheck");

var _classCallCheck3 = _interopRequireDefault(_classCallCheck2);

var _createClass2 = require("babel-runtime/helpers/createClass");

var _createClass3 = _interopRequireDefault(_createClass2);

var _possibleConstructorReturn2 = require("babel-runtime/helpers/possibleConstructorReturn");

var _possibleConstructorReturn3 = _interopRequireDefault(_possibleConstructorReturn2);

var _inherits2 = require("babel-runtime/helpers/inherits");

var _inherits3 = _interopRequireDefault(_inherits2);

var _promise = require("babel-runtime/core-js/promise");

var _promise2 = _interopRequireDefault(_promise);

var _nodeHid = require("node-hid");

var _nodeHid2 = _interopRequireDefault(_nodeHid);

var _hwTransport = require("@ledgerhq/hw-transport");

var _hwTransport2 = _interopRequireDefault(_hwTransport);

var _getDevices = require("./getDevices");

var _getDevices2 = _interopRequireDefault(_getDevices);

var _listenDevices2 = require("./listenDevices");

var _listenDevices3 = _interopRequireDefault(_listenDevices2);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

// FIXME drop
function defer() {
  var resolve = void 0,
      reject = void 0;
  var promise = new _promise2.default(function (success, failure) {
    resolve = success;
    reject = failure;
  });
  if (!resolve || !reject) throw new Error("defer() error"); // this never happens and is just to make flow happy
  return { promise: promise, resolve: resolve, reject: reject };
}

var listenDevicesDebounce = 500;
var listenDevicesPollingSkip = function listenDevicesPollingSkip() {
  return false;
};
var listenDevicesDebug = function listenDevicesDebug() {};

/**
 * node-hid Transport implementation
 * @example
 * import TransportNodeHid from "@ledgerhq/hw-transport-node-u2f";
 * ...
 * TransportNodeHid.create().then(transport => ...)
 */

var TransportNodeHid = function (_Transport) {
  (0, _inherits3.default)(TransportNodeHid, _Transport);

  function TransportNodeHid(device) // FIXME not used?
  {
    var ledgerTransport = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : true;
    var timeout = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : 0;
    (0, _classCallCheck3.default)(this, TransportNodeHid);

    var _this = (0, _possibleConstructorReturn3.default)(this, (TransportNodeHid.__proto__ || (0, _getPrototypeOf2.default)(TransportNodeHid)).call(this));

    _this.device = device;
    _this.ledgerTransport = ledgerTransport;
    _this.timeout = timeout;
    _this.exchangeStack = [];
    return _this;
  }

  /**
   */


  (0, _createClass3.default)(TransportNodeHid, [{
    key: "exchange",
    value: function exchange(apdu) {
      var _this2 = this;

      function ledgerWrap(channel, command, packetSize) {
        var sequenceIdx = 0;
        var offset = 0;

        var tmp = Buffer.alloc(7);
        tmp.writeUInt16BE(channel, 0);
        tmp[2] = 0x05; // TAG_APDU
        tmp.writeUInt16BE(sequenceIdx, 3);
        sequenceIdx++;
        tmp.writeUInt16BE(command.length, 5);
        var blockSize = command.length > packetSize - 7 ? packetSize - 7 : command.length;
        var result = Buffer.concat([tmp, command.slice(offset, offset + blockSize)], blockSize + 7);
        offset += blockSize;
        while (offset !== command.length) {
          tmp = Buffer.alloc(5);
          tmp.writeUInt16BE(channel, 0);
          tmp[2] = 0x05; // TAG_APDU
          tmp.writeUInt16BE(sequenceIdx, 3);
          sequenceIdx++;
          blockSize = command.length - offset > packetSize - 5 ? packetSize - 5 : command.length - offset;
          result = Buffer.concat([result, tmp, command.slice(offset, offset + blockSize)], result.length + blockSize + 5);
          offset += blockSize;
        }
        return result;
      }

      function ledgerUnwrap(channel, data, packetSize) {
        var offset = 0;
        var responseLength = void 0;
        var sequenceIdx = 0;
        var response = void 0;
        if (typeof data === "undefined" || data.length < 7 + 5) {
          return;
        }
        if (data[offset++] !== channel >> 8) {
          throw new _hwTransport.TransportError("Invalid channel", "InvalidChannel");
        }
        if (data[offset++] !== (channel & 0xff)) {
          throw new _hwTransport.TransportError("Invalid channel", "InvalidChannel");
        }
        if (data[offset++] !== 0x05) {
          throw new _hwTransport.TransportError("Invalid tag", "InvalidTag");
        }
        if (data[offset++] !== 0x00) {
          throw new _hwTransport.TransportError("Invalid sequence", "InvalidSequence");
        }
        if (data[offset++] !== 0x00) {
          throw new _hwTransport.TransportError("Invalid sequence", "InvalidSequence");
        }
        responseLength = (data[offset++] & 0xff) << 8;
        responseLength |= data[offset++] & 0xff;
        if (data.length < 7 + responseLength) {
          return;
        }
        var blockSize = responseLength > packetSize - 7 ? packetSize - 7 : responseLength;
        response = data.slice(offset, offset + blockSize);
        offset += blockSize;
        while (response.length !== responseLength) {
          sequenceIdx++;
          if (offset === data.length) {
            return;
          }
          if (data[offset++] !== channel >> 8) {
            throw new _hwTransport.TransportError("Invalid channel", "InvalidChannel");
          }
          if (data[offset++] !== (channel & 0xff)) {
            throw new _hwTransport.TransportError("Invalid channel", "InvalidChannel");
          }
          if (data[offset++] !== 0x05) {
            throw new _hwTransport.TransportError("Invalid tag", "InvalidTag");
          }
          if (data[offset++] !== sequenceIdx >> 8) {
            throw new _hwTransport.TransportError("Invalid sequence", "InvalidSequence");
          }
          if (data[offset++] !== (sequenceIdx & 0xff)) {
            throw new _hwTransport.TransportError("Invalid sequence", "InvalidSequence");
          }
          blockSize = responseLength - response.length > packetSize - 5 ? packetSize - 5 : responseLength - response.length;
          if (blockSize > data.length - offset) {
            return;
          }
          response = Buffer.concat([response, data.slice(offset, offset + blockSize)], response.length + blockSize);
          offset += blockSize;
        }
        return response;
      }

      var deferred = defer();
      var exchangeTimeout = void 0;
      var transport = void 0;
      if (!this.ledgerTransport) {
        transport = apdu;
      } else {
        transport = ledgerWrap(0x0101, apdu, 64);
      }

      if (this.timeout !== 0) {
        exchangeTimeout = setTimeout(function () {
          // Node.js supports timeouts
          deferred.reject(new _hwTransport.TransportError("timeout", "timeout"));
        }, this.timeout);
      }

      // enter the exchange wait list
      this.exchangeStack.push(deferred);

      if (this.exchangeStack.length === 1) {
        var processNextExchange = function processNextExchange() {
          // don't pop it now, to avoid multiple at once
          var deferred = _this2.exchangeStack[0];

          var send = function send(content) {
            var debug = _this2.debug;

            if (debug) {
              debug("=>" + content.toString("hex"));
            }
            var data = [0x00];
            for (var i = 0; i < content.length; i++) {
              data.push(content[i]);
            }
            _this2.device.write(data);
            return _promise2.default.resolve(content.length);
          };

          var recv = function recv() {
            return new _promise2.default(function (resolve, reject) {
              return _this2.device.read(function (err, res) {
                if (err || !res) reject(err);else {
                  var buffer = Buffer.from(res);
                  var debug = _this2.debug;

                  if (debug) {
                    debug("<=" + buffer.toString("hex"));
                  }
                  resolve(buffer);
                }
              });
            });
          };

          var performExchange = function performExchange() {
            var offsetSent = 0;
            var firstReceived = true;
            var toReceive = 0;

            var received = Buffer.alloc(0);
            var sendPart = function sendPart() {
              if (offsetSent === transport.length) {
                return receivePart();
              }
              var blockSize = transport.length - offsetSent > 64 ? 64 : transport.length - offsetSent;
              var block = transport.slice(offsetSent, offsetSent + blockSize);
              var paddingSize = 64 - block.length;
              if (paddingSize !== 0) {
                var padding = Buffer.alloc(paddingSize).fill(0);
                block = Buffer.concat([block, padding], block.length + paddingSize);
              }
              return send(block).then(function () {
                offsetSent += blockSize;
                return sendPart();
              });
            };

            var receivePart = function receivePart() {
              if (!_this2.ledgerTransport) {
                return recv().then(function (result) {
                  received = Buffer.concat([received, result], received.length + result.length);
                  if (firstReceived) {
                    firstReceived = false;
                    if (received.length === 2 || received[0] !== 0x61) {
                      return received;
                    } else {
                      toReceive = received[1];
                      if (toReceive === 0) {
                        toReceive = 256;
                      }
                      toReceive += 2;
                    }
                  }
                  if (toReceive < 64) {
                    return received;
                  } else {
                    toReceive -= 64;
                    return receivePart();
                  }
                });
              } else {
                return recv().then(function (result) {
                  received = Buffer.concat([received, result], received.length + result.length);
                  var response = ledgerUnwrap(0x0101, received, 64);
                  if (typeof response !== "undefined") {
                    return response;
                  } else {
                    return receivePart();
                  }
                });
              }
            };
            return sendPart();
          };

          performExchange().then(function (result) {
            var response = void 0,
                resultBin = result;
            if (!_this2.ledgerTransport) {
              if (resultBin.length === 2 || resultBin[0] !== 0x61) {
                response = resultBin;
              } else {
                var size = resultBin[1];
                // fake T0
                if (size === 0) {
                  size = 256;
                }
                response = resultBin.slice(2);
              }
            } else {
              response = resultBin;
            }
            // build the response
            if (_this2.timeout !== 0) {
              clearTimeout(exchangeTimeout);
            }
            return response;
          }).then(function (response) {
            // consume current promise
            _this2.exchangeStack.shift();

            // schedule next exchange
            if (_this2.exchangeStack.length > 0) {
              processNextExchange();
            }
            return response;
          }, function (err) {
            if (_this2.timeout !== 0) {
              clearTimeout(exchangeTimeout);
            }
            throw err;
          })
          // plug to deferred
          .then(deferred.resolve, deferred.reject);
        };

        // schedule next exchange
        processNextExchange();
      }

      // the exchangeStack will process the promise when possible
      return deferred.promise;
    }
  }, {
    key: "setScrambleKey",
    value: function setScrambleKey() {}
  }, {
    key: "close",
    value: function close() {
      this.device.close();
      return _promise2.default.resolve();
    }
  }], [{
    key: "open",


    /**
     */
    value: function () {
      var _ref = (0, _asyncToGenerator3.default)( /*#__PURE__*/_regenerator2.default.mark(function _callee(path) {
        return _regenerator2.default.wrap(function _callee$(_context) {
          while (1) {
            switch (_context.prev = _context.next) {
              case 0:
                return _context.abrupt("return", _promise2.default.resolve(new TransportNodeHid(new _nodeHid2.default.HID(path))));

              case 1:
              case "end":
                return _context.stop();
            }
          }
        }, _callee, this);
      }));

      function open(_x3) {
        return _ref.apply(this, arguments);
      }

      return open;
    }()
  }]);
  return TransportNodeHid;
}(_hwTransport2.default);

TransportNodeHid.isSupported = function () {
  return _promise2.default.resolve(typeof _nodeHid2.default.HID === "function");
};

TransportNodeHid.list = function () {
  return _promise2.default.resolve((0, _getDevices2.default)().map(function (d) {
    return d.path;
  }));
};

TransportNodeHid.setListenDevicesDebounce = function (delay) {
  listenDevicesDebounce = delay;
};

TransportNodeHid.setListenDevicesPollingSkip = function (conditionToSkip) {
  listenDevicesPollingSkip = conditionToSkip;
};

TransportNodeHid.setListenDevicesDebug = function (debug) {
  listenDevicesDebug = typeof debug === "function" ? debug : debug ? function () {
    var _console;

    for (var _len = arguments.length, log = Array(_len), _key = 0; _key < _len; _key++) {
      log[_key] = arguments[_key];
    }

    return (_console = console).log.apply(_console, ["[listenDevices]"].concat(log));
  } : function () {};
};

TransportNodeHid.listen = function (observer) {
  var unsubscribed = false;
  _promise2.default.resolve((0, _getDevices2.default)()).then(function (devices) {
    // this needs to run asynchronously so the subscription is defined during this phase
    var _iteratorNormalCompletion = true;
    var _didIteratorError = false;
    var _iteratorError = undefined;

    try {
      for (var _iterator = (0, _getIterator3.default)(devices), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
        var device = _step.value;

        if (!unsubscribed) {
          var descriptor = device.path;
          observer.next({ type: "add", descriptor: descriptor, device: device });
        }
      }
    } catch (err) {
      _didIteratorError = true;
      _iteratorError = err;
    } finally {
      try {
        if (!_iteratorNormalCompletion && _iterator.return) {
          _iterator.return();
        }
      } finally {
        if (_didIteratorError) {
          throw _iteratorError;
        }
      }
    }
  });

  var _listenDevices = (0, _listenDevices3.default)(listenDevicesDebounce, listenDevicesPollingSkip, listenDevicesDebug),
      events = _listenDevices.events,
      stop = _listenDevices.stop;

  var onAdd = function onAdd(device) {
    if (unsubscribed || !device) return;
    observer.next({ type: "add", descriptor: device.path, device: device });
  };
  var onRemove = function onRemove(device) {
    if (unsubscribed || !device) return;
    observer.next({ type: "remove", descriptor: device.path, device: device });
  };
  events.on("add", onAdd);
  events.on("remove", onRemove);
  function unsubscribe() {
    unsubscribed = true;
    events.removeListener("add", onAdd);
    events.removeListener("remove", onRemove);
    stop();
  }
  return { unsubscribe: unsubscribe };
};

exports.default = TransportNodeHid;
//# sourceMappingURL=TransportNodeHid.js.map