angular.module('bullseye.masthead.search', [
    'ngResource',
    'bullseye.dataService',
    'bullseye.http'
])
    .factory('SearchService', ['$location', 'SearchTypes', 'DataService', function ($location, SearchTypes, DataService) {
        var firstSearchKey = null;
        var service = {
            searchData: {
                filters: {
                    searchTypes: [],
                    searchType: null,
                    query: '',
                },
                isSearching: false
            }
        };

        if ('searchK' in $location.search()) {
            firstSearchKey = $location.search().searchK;
        }
        if ('searchV' in $location.search()) {
            service.searchData.filters.query = $location.search().searchV;
        }

        service.doSearch = function () {
            service.searchData.isSearching = true;
            DataService.search(service.searchData.filters.query, service.searchData.filters.searchType, function() {
                $location.search({
                    'searchK': service.searchData.filters.searchType.id,
                    'searchV': service.searchData.filters.query
                });
                service.searchData.isSearching = false;
            });
        };


        SearchTypes.query().$promise.then(function (types) {
            var hasIdSearch = false;
            service.searchData.filters.searchTypes = types.sort(function (a, b) {
                if ((a.id === "OSERAF:search/id") || (b.id === "OSERAF:search/id")) {
                    hasIdSearch = true;
                }
                return (a.name < b.name) ? -1 : ((a.name > b.name) ? 1 : 0);
            });
            service.searchData.filters.searchType = service.searchData.filters.searchTypes[0];
            if (firstSearchKey !== null) {
                service.searchData.filters.searchType =
                    _.find(service.searchData.filters.searchTypes, { 'id': firstSearchKey });
                if ('searchV' in $location.search() && service.searchData.filters.searchTypes.length > 0) {
                    service.doSearch();
                }
            } else if (hasIdSearch) {
                for (var idx = service.searchData.filters.searchTypes.length - 1; idx >= 0; idx--) {
                    if (service.searchData.filters.searchTypes[idx].id === "OSERAF:search/id") {
                        service.searchData.filters.searchType = service.searchData.filters.searchTypes[idx];
                        break;
                    }
                }
            }
        });

        return service;
    }])
    .controller('SearchController', ['$scope', 'SearchService', function ($scope, SearchService) {
        $scope.searchData = SearchService.searchData;
        $scope.doSearch = SearchService.doSearch;
    }])
    .directive('search', ['SearchService', function (SearchService) {
        return {
            restrict: 'E',
            templateUrl: 'masthead/tpls/search.tpl.html'
//            link: function (scope, element, attrs) {
//                scope.searchData = SearchService.searchData;
//                scope.$watch(DataService.getSearchTypes, function (types) {
//                    var hasIdSearch = false;
//                    scope.searchData.filters.searchTypes = types.sort(function (a, b) {
//                        if ((a.id === "OSERAF:search/id") || (b.id === "OSERAF:search/id")) {
//                            hasIdSearch = true;
//                        }
//                        return (a.name < b.name) ? -1 : ((a.name > b.name) ? 1 : 0);
//                    });
//                    scope.searchData.filters.searchType = scope.searchData.filters.searchTypes[0];
//                    if (scope.searchData.firstSearchKey !== null) {
//                        scope.searchData.filters.searchType =
//                            _.find(scope.searchData.filters.searchTypes, { 'id': scope.searchData.firstSearchKey });
//                        if ('searchV' in $location.search() && scope.searchData.filters.searchTypes.length > 0) {
//                            doSearch();
//                        }
//                    } else if (hasIdSearch) {
//                        for (var idx = scope.searchData.filters.searchTypes.length - 1; idx >= 0; idx--) {
//                            if (scope.searchData.filters.searchTypes[idx].id === "OSERAF:search/id") {
//                                scope.searchData.filters.searchType = scope.searchData.filters.searchTypes[idx];
//                                break;
//                            }
//                        }
//                    }
//                });
//                scope.doSearch = SearchService.doSearch;
//            }
        };
    }]);
