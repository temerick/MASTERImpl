angular.module('bullseye.home', [
    'ngResource',
    'bullseye.http',
    'bullseye.util',
    'bullseye.modal',
    'bullseye.dataService',
    'bullseye.home.views',
    'bullseye.masthead'
])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/home', {
            templateUrl: 'home/home.tpl.html',
            reloadOnSearch: false
        });
    }])
    .controller(
        'HomeController',
        [   '$scope', '$location',
            'DataService', 'EntityOps', 'UtilService', 'Modals',
            function(
                $scope, $location,
                DataService, EntityOps, UtilService, Modals
                ) {
                $scope.data = {
                    raw: [],
                    selection: [],
                    richSelection: [],
                    network: []
                };

                $scope.$watch(DataService.getEntityData, function (data) {
                    $scope.data.raw = data;
                });
                $scope.$watch(DataService.transformForNetwork, function (data) {
                    $scope.data.network = data;
                }, true);
                $scope.resolveItem = function (d) {
                    EntityOps.resolve({eId: d.entity.id}).$promise.then(function (resolutions) {
                        Modals.resolve
                            .openModal(d, resolutions)
                            .result.then(DataService.addNodes);
                    });
                };
                $scope.splitItem = function (entity) {
                    Modals.split
                        .openModal(entity)
                        .result.then(function (splits) {
                            EntityOps.split({eId: entity.entity.id, entities: splits}).$promise.then(function (resultSplits) {
                                DataService.split(entity.entity.id, resultSplits);
                            });
                        });
                };
                $scope.mergeItems = function (entities) {
                    var entityIds = _.pluck(entities, 'id');
                    Modals.merge
                        .openModal(entities)
                        .result.then(function (ent) {
                            EntityOps.merge({eIds: entityIds, entity: ent}).$promise.then(function (mergedEntity) {
                                DataService.merge(mergedEntity);
                            });
                        });
                };
                $scope.select = function (entityIds) {
                    $scope.data.selection = entityIds;
                    $scope.data.richSelection = _.map($scope.data.selection, function (eid) {
                        return _.find($scope.data.network[0], { 'id': eid });
                    });
                };
                $scope.showNeighborhood = DataService.showNeighborhood;
                $scope.removeItem = DataService.removeItem;
    }]);
