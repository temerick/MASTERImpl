// Project configuration.
//
module.exports = {
    
    // Where the project is built during development.
    buildDir: '../../../target/bullseye-webapp-0.0.2-SNAPSHOT',

    // The location of the html page that will be built.
    // Change if needed...
    indexDest: 'WEB-INF/views/index.ssp',

    // Where project is compiled for deployment.
    compileDir: 'deploy',

    // App file patterns used by the build system.
    appFiles: {
        js : [ 'src/**/*.js', '!src/**/*.spec.js', '!src/assets/**/*.js' ],
        specs: [ 'src/**/*.spec.js' ],
        tpl: [ 'src/app/**/*.tpl.html' ],
        html: [ 'src/index.html' ],
        stylus: 'src/stylus/main.styl'
    },
    
    // Dependencies needed for tests.
    testFiles: {
        js: [
            'vendor/bower/angular-mocks/angular-mocks.js'
        ]
    },

    // A list of vendor files needed for development. As you add new libraries
    // add a path to the files here. The 'index' task will append script tag
    // for them to 'indexDest' file specified above.
    vendorFiles: {
        js: [
            'vendor/bower/d3/d3.js',
            'vendor/bower/lodash/dist/lodash.js',
            'vendor/bower/jquery/dist/jquery.js',
            'vendor/bower/angular/angular.js',
            'vendor/bower/angular-route/angular-route.js',
            'vendor/bower/angular-animate/angular-animate.js',
            'vendor/bower/angular-resource/angular-resource.js',
            'vendor/bower/d3/d3.min.js',
            'vendor/bower/vis/dist/vis.js',
            'vendor/bower/angular-ui-bootstrap-bower/ui-bootstrap.js',
            'vendor/bower/angular-ui-bootstrap-bower/ui-bootstrap-tpls.js',
            'vendor/bower/momentjs/moment.js',
            // TODO - test only
            'vendor/bower/angular-mocks/angular-mocks.js',
        ],
        css: [],
        assets: [
            'vendor/bower/font-awesome/fonts/*',
            'vendor/bower/bootstrap/fonts/*'
        ]
    }
}
