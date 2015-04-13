angular.module('bullseye.home', [
    'ngResource',
    'bullseye.http',
    'bullseye.util',
    'bullseye.modal',
    'bullseye.dataService',
    'bullseye.home.views'
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
                $scope.searchData = {
                    filters: {
                        searchTypes: [],
                        searchType: null,
                        query: ''
                    },
                    isSearching: false,
                    firstSearchKey: null
                };
                if ('searchK' in $location.search()) {
                    $scope.searchData.firstSearchKey = $location.search().searchK;
                }
                if ('searchV' in $location.search()) {
                    $scope.searchData.filters.query = $location.search().searchV;
                }
                $scope.searchFilterQuery = '';
                $scope.dedupeIsRunning = false;
                $scope.$watch(DataService.getSearchTypes, function (types) {
                    var hasIdSearch = false;
                    $scope.searchData.filters.searchTypes = types.sort(function (a, b) {
                        if ((a.id === "OSERAF:search/id") || (b.id === "OSERAF:search/id")) {
                            hasIdSearch = true;
                        }
                        return (a.name < b.name) ? -1 : ((a.name > b.name) ? 1 : 0);
                    });
                    $scope.searchData.filters.searchType = $scope.searchData.filters.searchTypes[0];
                    if ($scope.searchData.firstSearchKey !== null) {
                        $scope.searchData.filters.searchType =
                            _.find($scope.searchData.filters.searchTypes, { 'id': $scope.searchData.firstSearchKey });
                        if ('searchV' in $location.search() && $scope.searchData.filters.searchTypes.length > 0) {
                            $scope.searchClickHandler();
                        }
                    } else if (hasIdSearch) {
                        for (var idx = $scope.searchData.filters.searchTypes.length - 1; idx >= 0; idx--) {
                            if ($scope.searchData.filters.searchTypes[idx].id === "OSERAF:search/id") {
                                $scope.searchData.filters.searchType = $scope.searchData.filters.searchTypes[idx];
                                break;
                            }
                        }
                    }
                });
                $scope.searchClickHandler = function() {
                    $scope.search($scope.searchData.filters.query);
                };
                $scope.isSearching = function() {
                    return $scope.searchData.isSearching;
                };
                $scope.hasSearchField = function() {
                    return $scope.searchFilterQuery.length > 0;
                };
                $scope.isDeduping = function() {
                    return $scope.dedupeIsRunning;
                };
                $scope.$watch(DataService.getEntityData, function (data) {
                    $scope.data.raw = data;
                });
                $scope.$watch(DataService.transformForNetwork, function (data) {
                    $scope.data.network = data;
                }, true);
                $scope.search = function (query) {
                    $scope.searchData.isSearching = true;
                    DataService.search(query, $scope.searchData.filters.searchType, function() {
                        $location.search({
                            'searchK': $scope.searchData.filters.searchType.id,
                            'searchV': query
                        });
                        $scope.searchData.isSearching = false;
                    });
                };
                $scope.resolveItem = function (d) {
                    EntityOps.resolve({eId: d.entity.id}).$promise.then(function (resolutions) {
                        Modals.resolve
                            .openModal(d, resolutions)
                            .result.then(DataService.addNodes);
                    });
                };
                $scope.deduplicate = function () {
                    $scope.dedupeIsRunning = true;
                    EntityOps.deduplicate().$promise.then(function (deduplications) {
                        var sortedDedupes = deduplications.sort(function (a, b) { return b.score - a.score; });
                        $scope.dedupeIsRunning = false;
                        Modals.deduplicate
                            .openModal(deduplications)
                            .result.then(DataService.deduplicate);
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
