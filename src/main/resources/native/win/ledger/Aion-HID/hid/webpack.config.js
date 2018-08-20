const path = require('path');

module.exports = {
  mode: 'none',
  devServer: {
    contentBase: path.join(__dirname, 'test'),
    compress: true,
    https: true,
    index: 'index.html',
    open: true
  },
  entry: './test/u2f.js',
  output: {
    filename: 'bundle.js'
  }
};