angular.module('bullseye.http', [
    'ngResource'
])
    .factory('SearchTypes', ['$resource', function ($resource) {
        return $resource('rest/data/searchtypes', {}, {
            query: {method: 'GET', isArray: true}
        });
    }])
    .factory('EntityOps', ['$resource', function ($resource) {
        return $resource('rest/data/entity', {}, {
            search: {method: 'GET', isArray: false, url: 'rest/data/search'},
            resolve: {method: 'GET', isArray: true, url: 'rest/data/resolve'},
            deduplicate: {method: 'GET', isArray: true, url: 'rest/data/deduplicate'},
            merge: {method: 'POST', isArray: false, url: 'rest/data/merge'},
            split: {method: 'POST', isArray: true, url: 'rest/data/split'},
            get: {method: 'GET', isArray: false, url: 'rest/data/entity/:eId'},
            neighborhood: {method: 'GET', isArray: false, url: 'rest/data/entity/:eId/neighborhood'}
        });
    }]);