angular.module('bullseye.masthead', [
    'ngResource',
    'bullseye.dataService'
])
    .directive('masthead', ['$location', 'DataService', function ($location, DataService) {
        var searchData = {
            filters: {
                searchTypes: [],
                searchType: null,
                query: ''
            },
            isSearching: false,
            firstSearchKey: null
        };
        if ('searchK' in $location.search()) {
            searchData.firstSearchKey = $location.search().searchK;
        }
        if ('searchV' in $location.search()) {
            searchData.filters.query = $location.search().searchV;
        }
        
        var doSearch = function () {
            searchData.isSearching = true;
            DataService.search(searchData.filters.query, searchData.filters.searchType, function() {
                $location.search({
                    'searchK': searchData.filters.searchType.id,
                    'searchV': searchData.filters.query
                });
                searchData.isSearching = false;
            });
        };
        
        return {
            restrict: 'E',
            scope: {
                deduplicate: '&deduplicate',
                isDeduping: '&isDeduping'
            },
            templateUrl: 'masthead/masthead.tpl.html',
            link: function (scope, element, attrs) {
                scope.searchData = searchData;
                scope.$watch(DataService.getSearchTypes, function (types) {
                    var hasIdSearch = false;
                    scope.searchData.filters.searchTypes = types.sort(function (a, b) {
                        if ((a.id === "OSERAF:search/id") || (b.id === "OSERAF:search/id")) {
                            hasIdSearch = true;
                        }
                        return (a.name < b.name) ? -1 : ((a.name > b.name) ? 1 : 0);
                    });
                    scope.searchData.filters.searchType = scope.searchData.filters.searchTypes[0];
                    if (scope.searchData.firstSearchKey !== null) {
                        scope.searchData.filters.searchType =
                            _.find(scope.searchData.filters.searchTypes, { 'id': scope.searchData.firstSearchKey });
                        if ('searchV' in $location.search() && scope.searchData.filters.searchTypes.length > 0) {
                            doSearch();
                        }
                    } else if (hasIdSearch) {
                        for (var idx = scope.searchData.filters.searchTypes.length - 1; idx >= 0; idx--) {
                            if (scope.searchData.filters.searchTypes[idx].id === "OSERAF:search/id") {
                                scope.searchData.filters.searchType = scope.searchData.filters.searchTypes[idx];
                                break;
                            }
                        }
                    }
                });
                scope.doSearch = doSearch;
            }
        };
    }])

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
            templateUrl: 'masthead/deduplicate.tpl.html'
        };
    }]);