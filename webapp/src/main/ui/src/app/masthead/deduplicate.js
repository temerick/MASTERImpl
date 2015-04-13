angular.module('bullseye.masthead.deduplicate', [
    'ngResource',
    'bullseye.modal',
    'bullseye.dataService'
])
    .controller(
        'DedupeController',
        [
            '$scope', 'EntityOps', 'Modals', 'DataService',
            function ($scope, EntityOps, Modals, DataService) {
                $scope.isDeduping = false;

                $scope.deduplicate = function () {
                    $scope.isDeduping = true;
                    EntityOps.deduplicate().$promise.then(function (deduplications) {
                        var sortedDedupes = deduplications.sort(function (a, b) { return b.score - a.score; });
                        $scope.isDeduping = false;
                        Modals.deduplicate
                            .openModal(deduplications)
                            .result.then(DataService.deduplicate);
                    });
                };
            }
        ])
    .directive('deduplicate', [function () {
        return {
            restrict: 'E',
            templateUrl: 'masthead/tpls/deduplicate.tpl.html'
        };
    }]);