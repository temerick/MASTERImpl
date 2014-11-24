angular.module('bullseye', [
    'ngRoute',
    'ngAnimate',
    'ngResource',
    'ui.bootstrap',
    'templates-app',
    'bullseye.home'
])
    .config(['$routeProvider', function ($routeProvider) {
        // Configure route provider to transform any undefined hashes to /home.
        $routeProvider.otherwise({redirectTo: '/home'});
    }])

    .constant('appInfo', {
        name: 'bullseye',
        title: 'Bullseye'
    })

    .controller('AppController', ['$scope', 'appInfo', function ($scope, appInfo) {
        $scope.appModel = {
            appName: appInfo.name,
            appTitle: appInfo.title
        };
    }]);
