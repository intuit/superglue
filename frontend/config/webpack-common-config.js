// webpack-common-config.js

// This file will contain configuration data that
// is shared between development and production builds.

const HtmlWebpackPlugin = require('html-webpack-plugin');
const webpack = require('webpack');
const path = require('path');

const paths = require('./paths');

module.exports = {
  plugins: [
    new HtmlWebpackPlugin({
      inject: true,
      template: paths.appHtml,
    }),
    new webpack.DefinePlugin({
      'process.env.GRAPH_HOST': JSON.stringify(process.env.GRAPH_HOST),
      'process.env.ELASTICSEARCH_HOST': JSON.stringify(
        process.env.ELASTICSEARCH_HOST,
      ),
      'process.env.ELASTICSEARCH_PROTOCOL': JSON.stringify(
        process.env.ELASTICSEARCH_PROTOCOL,
      ),
      'process.env.ELASTICSEARCH_PORT': JSON.stringify(
        process.env.ELASTICSEARCH_PORT,
      ),
    }),
  ],
  resolve: {
    // File extensions. Add others and needed (e.g. scss, json)
    extensions: ['.js', '.jsx', '.scss', '.css'],
    modules: ['node_modules'],
    // Aliases help with shortening relative paths
    // 'Components/button' === '../../../components/button'
    alias: {
      Actions: path.resolve(paths.appSrc, 'actions'),
      Components: path.resolve(paths.appSrc, 'components'),
      Constants: path.resolve(paths.appSrc, 'constants'),
      Containers: path.resolve(paths.appSrc, 'containers'),
      Store: path.resolve(paths.appSrc, 'store'),
      Styles: path.resolve(paths.appSrc, 'styles'),
      Utils: path.resolve(paths.appSrc, 'utils'),
    },
  },
  module: {
    rules: [
      {
        test: /\.(png|svg|jpg)$/,
        use: ['file-loader'],
      },
    ],
  },
  devServer: {
    historyApiFallback: true,
  },
};
