const Merge = require("webpack-merge");
const Web = require("./prod.webpack.config");
const PacktrackerPlugin = require("@packtracker/webpack-plugin");

const PackTracker = Merge(Web, {
  plugins: [
    new PacktrackerPlugin({
      project_token: process.env.PACKTRACKER_TOKEN,
      upload: true,
      fail_build: true,
      branch: process.env.GITHUB_REF.split("/")[2],
      exclude_assets: [/explore-opt.*/]
    })
  ]
});

module.exports = PackTracker;
