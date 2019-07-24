// webpack-prod-config.js

// contains configuration data related to prod build

const path = require('path');

const webpack = require('webpack');
const merge = require('webpack-merge');
const TerserPlugin = require('terser-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

const paths = require('./paths');
const common = require('./webpack-common-config.js');

module.exports = merge(common, {
  entry: {
    // Split vendor code into separate bundles
    vendor: ['react'],
    app: paths.appIndexJs,
  },
  mode: 'production',
  // Set the name of our JS bundle using a chuckhash
  // (e.g. '5124f5efa5436b5b5e7d_app.js')
  // Location where built files will go.
  output: {
    filename: '[chunkhash]_[name].js',
    path: paths.appBuild,
    publicPath: '/',
  },
  optimization: {
    minimizer: [new TerserPlugin()],
  },
  plugins: [
    // Set process.env.NODE_ENV to production
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify('production'),
      },
    }),
    // Extract text/(s)css from a bundle, or bundles, into a separate file.
    new ExtractTextPlugin('styles.css'),
  ],
  module: {
    rules: [
      {
        // look for .js or .jsx files
        test: /\.(js|jsx)$/,
        // in the `src` directory
        include: path.resolve(paths.appSrc),
        exclude: /node_modules/,
        use: {
          // use babel for transpiling JavaScript files
          loader: 'babel-loader',
          options: {
            presets: ['@babel/react'],
          },
        },
      },
      {
        // look for .css or .scss files.
        test: /\.(css|scss)$/,
        // in the `src` directory
        include: [path.resolve(paths.appSrc), /node_modules/],
        use: ExtractTextPlugin.extract({
          fallback: 'style-loader',
          use: [
            {
              loader: 'css-loader',
              options: {
                modules: false,
              },
            },
            {
              loader: 'sass-loader',
              options: {
                sourceMap: false,
              },
            },
          ],
        }),
      },
    ],
  },
});
