angular.module('bullseye.masthead', [
    'bullseye.masthead.deduplicate',
    'bullseye.masthead.search'
])
    .directive('masthead', function () {
        return {
            restrict: 'E',
            templateUrl: 'masthead/tpls/masthead.tpl.html'
        };
    });
