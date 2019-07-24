// webpack-dev-server.js

// dev-server is responsible for running your project locally

const WebpackDevServer = require("webpack-dev-server");
const webpack = require("webpack");
const paths = require("./paths");
const config = require("./webpack-dev-config.js");

// Change port to suit your preference
const Port = 3000;
const Host = "0.0.0.0";

const options = {
  host: Host,
  // Enable webpack's Hot Module Replacement feature
  hot: true,
  // full-screen overlay in the browser for compiler errors or warnings
  overlay: {
    warnings: false,
    errors: true
  },
  // Show errors and warnings in console
  quiet: false,
  // Hide the build info
  noInfo: false,
  // Tell the server where to serve static files from.
  // Set this is the `paths.js` file.
  contentBase: paths.appAssets,
  // If static content changes, reload the page.
  // In other words, editing a photo within the assets
  // directory will force the page to reload.
  watchContentBase: true,
  after() {
    process.stdout.write(`dev server is running: http://${Host}:${Port}\n`);
  }
};

WebpackDevServer.addDevServerEntrypoints(config, options);
const compiler = webpack(config);
const server = new WebpackDevServer(compiler, options);

server.listen(Port, Host, () => {});
