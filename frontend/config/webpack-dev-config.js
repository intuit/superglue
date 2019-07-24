// webpack-dev-config.js

// configuration data related to development only
const path = require('path');
const webpack = require('webpack');
const merge = require('webpack-merge');
const paths = require('./paths');

require('dotenv').config({
  path: path.join(__dirname, '.env.dev'),
});

// import common webpack config
const common = require('./webpack-common-config');
module.exports = merge(common, {
  entry: [paths.appIndexJs],
  mode: 'development',
  // devtool option controls if and how source maps are generated.
  // see https://webpack.js.org/configuration/devtool/
  // If you find that you need more control of source map generation,
  // see https://webpack.js.org/plugins/source-map-dev-tool-plugin/
  devtool: 'eval',
  plugins: [
    new webpack.HotModuleReplacementPlugin(),
    //new webpack.NamedModulesPlugin(),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development'),
    }),
  ],
  module: {
    rules: [
      {
        // look for .js or .jsx files
        test: /\.(js|jsx)$/,
        // in the `src` directory
        include: path.resolve(paths.appSrc),
        exclude: /(node_modules)/,
        use: {
          // use babel for transpiling JavaScript files
          loader: 'babel-loader',
          options: {
            presets: ['@babel/react'],
          },
        },
      },
      {
        // look for .css or .scss files
        test: /\.(css|scss)$/,
        // in the `src` directory
        include: [path.resolve(paths.appSrc), /node_modules/],
        use: [
          {
            loader: 'style-loader',
          },
          {
            loader: 'css-loader',
            options: {
              modules: false,
            },
          },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: true,
            },
          },
        ],
      },
    ],
  },
});
