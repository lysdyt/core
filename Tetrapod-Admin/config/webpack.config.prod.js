var autoprefixer = require('autoprefixer');
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var ManifestPlugin = require('webpack-manifest-plugin');
var InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
var url = require('url');
var paths = require('./paths');
var getClientEnvironment = require('./env');


function ensureSlash(path, needsSlash) {
   var hasSlash = path.endsWith('/');
   if (hasSlash && !needsSlash) {
      return path.substr(path, path.length - 1);
   } else if (!hasSlash && needsSlash) {
      return path + '/';
   } else {
      return path;
   }
}

var env = getClientEnvironment();
var publicPath = "admin";
if (process.argv && process.argv.length > 1) {
   for (var i=1; i<process.argv.length; i++){
      if (process.argv[i].startsWith("--server=")) {
         publicPath = process.argv[i].substring("--server=".length);
      }
   }
}

publicPath = ensureSlash(publicPath, true);

// Assert this just to be safe.
// Development builds of React are slow and not intended for production.
if (env['process.env'].NODE_ENV !== '"production"') {
   throw new Error('Production builds must have NODE_ENV=production.');
}

// This is the production configuration.
// It compiles slowly and is focused on producing a fast and minimal bundle.
// The development configuration is different and lives in a separate file.
module.exports = {
   // Don't attempt to continue if there are any errors.
   bail: true,
   // We generate sourcemaps in production. This is slow but gives good results.
   // You can exclude the *.map files from the build during deployment.
   devtool: 'source-map',
   // In production, we only want to load the polyfills and the app code.
   entry: [
      require.resolve('./polyfills'),
      paths.appIndexJs
   ],
   output: {
      // The build folder.
      path: paths.appBuild,
      // Generated JS file names (with nested folders).
      // There will be one main bundle, and one file per asynchronous chunk.
      // We don't currently advertise code splitting but Webpack supports it.
      filename: 'static/js/[name].[chunkhash:8].js',
      chunkFilename: 'static/js/[name].[chunkhash:8].chunk.js',
      // We inferred the "public path" (such as / or /my-project) from homepage.
      publicPath: publicPath
   },
   resolve: {
      // This allows you to set a fallback for where Webpack should look for modules.
      // We read `NODE_PATH` environment variable in `paths.js` and pass paths here.
      // We use `fallback` instead of `root` because we want `node_modules` to "win"
      // if there any conflicts. This matches Node resolution mechanism.
      // https://github.com/facebookincubator/create-react-app/issues/253
      fallback: paths.nodePaths.concat([paths.appSrc]),
      // These are the reasonable defaults supported by the Node ecosystem.
      // We also include JSX as a common component filename extension to support
      // some tools, although we do not recommend using it, see:
      // https://github.com/facebookincubator/create-react-app/issues/290
      extensions: ['.ts', '.tsx', '.js', '.json', '.jsx', ''],
      alias: {
         // Support React Native Web
         // https://www.smashingmagazine.com/2016/08/a-glimpse-into-the-future-with-react-native-for-web/
         'react-native': 'react-native-web'
      }
   },

   module: {
      // First, run the linter.
      // It's important to do this before Babel processes the JS.
      preLoaders: [{test: /\.(ts|tsx)$/, loader: 'tslint', include: paths.appSrc}
      ],
      loaders: [
         // Default loader: load all assets that are not handled
         // by other loaders with the url loader.
         // Note: This list needs to be updated with every change of extensions
         // the other loaders match.
         // E.g., when adding a loader for a new supported file extension,
         // we need to add the supported extension to this loader too.
         // Add one new line in `exclude` for each loader.
         //
         // "file" loader makes sure those assets end up in the `build` folder.
         // When you `import` an asset, you get its filename.
         // "url" loader works just like "file" loader but it also embeds
         // assets smaller than specified size as data URLs to avoid requests.
         {
            exclude: [
               /\.html$/,
               /\.(js|jsx)$/,
               /\.(ts|tsx)$/,
               /\.scss$/,
               /\.css$/,
               /\.json$/
            ],
            loader: 'url',
            query: {
               limit: 10000,
               name: 'static/media/[name].[hash:8].[ext]'
            }
         },
         {test: /\.(ts|tsx)$/, include: paths.appSrc, loader: 'ts'},
         {test: /\.scss$/, loader: ExtractTextPlugin.extract('style', 'css?importLoaders=1!postcss!sass')},
         {test: /\.css$/, loader: ExtractTextPlugin.extract('style', 'css?importLoaders=1!postcss')},
         {test: /\.json$/,loader: 'json'},
      ]
   },
   // We use PostCSS for autoprefixing only.
   postcss: function () {
      return [
         autoprefixer({
            browsers: [
               '>1%',
               'last 4 versions',
               'Firefox ESR',
               'not ie < 9', // React doesn't support IE8 anyway
            ]
         }),
      ];
   },
   plugins: [
      // Generates an `index.html` file with the <script> injected.
      new HtmlWebpackPlugin({
         inject: true,
         template: paths.appHtml,
         minify: {
            removeComments: true,
            collapseWhitespace: true,
            removeRedundantAttributes: true,
            useShortDoctype: true,
            removeEmptyAttributes: true,
            removeStyleLinkTypeAttributes: true,
            keepClosingSlash: true,
            minifyJS: true,
            minifyCSS: true,
            minifyURLs: true
         }
      }),
      // Makes some environment variables available to the JS code, for example:
      // if (process.env.NODE_ENV === 'production') { ... }. See `./env.js`.
      // It is absolutely essential that NODE_ENV was set to production here.
      // Otherwise React will be compiled in the very slow development mode.
      new webpack.DefinePlugin(env),
      // This helps ensure the builds are consistent if source hasn't changed:
      new webpack.optimize.OccurrenceOrderPlugin(),
      // Try to dedupe duplicated modules, if any:
      new webpack.optimize.DedupePlugin(),
      // Minify the code.
      new webpack.optimize.UglifyJsPlugin({
         compress: {
            screw_ie8: true, // React doesn't support IE8
            warnings: false
         },
         mangle: {
            screw_ie8: true
         },
         output: {
            comments: false,
            screw_ie8: true
         }
      }),
      // Note: this won't work without ExtractTextPlugin.extract(..) in `loaders`.
      new ExtractTextPlugin('static/css/[name].[contenthash:8].css'),
      // Generate a manifest file which contains a mapping of all asset filenames
      // to their corresponding output file so that tools can pick it up without
      // having to parse `index.html`.
      new ManifestPlugin({
         fileName: 'asset-manifest.json'
      })
   ],
   // Some libraries import Node modules but don't use them in the browser.
   // Tell Webpack to provide empty mocks for them so importing them works.
   node: {
      fs: 'empty',
      net: 'empty',
      tls: 'empty'
   }
};