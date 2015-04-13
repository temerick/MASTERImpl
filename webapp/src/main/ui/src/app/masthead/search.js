angular.module('bullseye.masthead.search', [
    'ngResource',
    'bullseye.dataService'
])
    .directive('search', ['$location', 'DataService', function ($location, DataService) {
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
            templateUrl: 'masthead/tpls/search.tpl.html',
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
    }]);
