angular.module('bullseye.home', [])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/home', {
            templateUrl: 'home/home.tpl.html',
        });
    }])
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
    }])
    .controller('HomeController', ['$scope', '$modal', 'DataService', 'EntityOps', function($scope, $modal, DataService, EntityOps) {
        var resolveModal,
        splitModal,
        deduplicateModal,
        mergeModal,
        resolveModalController = function ($scope, $modalInstance, resolutions, entity) {
            $scope.resolutions = _.map(resolutions, function(resource) {
                                     resource.entity.label = resource.entity.attrs.Name || resource.entity.attrs.name || resource.entity.attrs.actual_name || resource.entity.attrs.displayName || resource.entity.attrs.title || resource.entity.id;
                                     return resource;
                                 });
            $scope.entity = entity;
            $scope.selected = {
                items: []
            };
            $scope.ok = function () {
                $modalInstance.close($scope.selected.items);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss();
            };
            $scope.toggleSelect = function (d) {
                var idx = $scope.selected.items.indexOf(d);
                if (idx < 0) {
                    $scope.selected.items.push(d);
                } else {
                    $scope.selected.items.splice(idx, 1);
                }
            };
            $scope.getToggleClass = function (r) {
                return $scope.selected.items.indexOf(r) < 0 ? 'danger' : 'success';
            };
            $scope.getToggleIconClass = function (r) {
                return $scope.getToggleClass(r) === 'success' ? 'fa-check-circle' : 'fa-times-circle';
            };
            $scope.getScoreClass = function (score) {
                if (score >= 0.85) {
                    return 'success';
                } else if (score >= 0.70) {
                    return 'warning';
                }
                return 'danger';
            };
            $scope.round = function(score) {
                return Math.round(100.0 * score);
            };
        },
        deduplicateModalController = function ($scope, $modalInstance, deduplications) {
            $scope.deduplications =  _.map(deduplications, function(resource) {
                                         _.map(resource.entities, function(entity) {
                                             entity.label = entity.attrs.Name || entity.attrs.name || entity.attrs.actual_name || entity.attrs.title || entity.attrs.displayName || entity.id;
                                             return entity;
                                         });
                                         return resource;
                                    });

            $scope.selected = {
                items: []
            };
            $scope.round = function (x) {
                return Math.round(100.0 * x);
            };
            $scope.ok = function () {
                var nodes = [];
                $scope.selected.items.forEach(function(n) {
                    n.entities.forEach(function (e){
                        nodes.push({entity: e, score: Math.round(n.score)});
                    });
                });
                $modalInstance.close(nodes);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss();
            };
            $scope.toggleSelect = function (dd) {
                var idx = $scope.selected.items.indexOf(dd);
                if (idx < 0) {
                    $scope.selected.items.push(dd);
                } else {
                    $scope.selected.items.splice(idx, 1);
                }
            };
            $scope.getToggleClass = function (dd) {
                return $scope.selected.items.indexOf(dd) < 0 ? 'danger' : 'success';
            };
            $scope.getToggleIconClass = function (dd) {
                return $scope.getToggleClass(dd) === 'success' ? 'fa-check-circle' : 'fa-times-circle';
            };
            $scope.getScoreClass = function (score) {
                // scores are slightly different from resolve, with an expected cutoff of 82 for dedupe matching
                if (score >= 0.90) {
                    return 'success';
                } else if (score >= 0.85) {
                    return 'warning';
                }
                return 'danger';
            };
        },
        mergeModalController = function($scope, $modalInstance, entities) {
            $scope.finalAttrs = {};
            $scope.finalIncomingEdges = [];
            $scope.finalOutgoingEdges = [];
            $scope.conflictingAttrs = [];
            $scope.custom = {
                key: '',
                value: ''
            };

            $scope.addAttribute = function (attr) {
                if(_.has($scope.finalAttrs, attr.key)) {
                    var insertIdx = _.sortedIndex($scope.conflictingAttrs, {key: attr.key, val: $scope.finalAttrs[attr.key]}, 'key');
                    $scope.conflictingAttrs.splice(insertIdx, 0, {key: attr.key, val: $scope.finalAttrs[attr.key]});
                    delete $scope.finalAttrs[attr.key];
                }

                _.remove($scope.conflictingAttrs, function(item) {
                     return (item.key == attr.key && item.val == attr.val);
                });
                $scope.finalAttrs[attr.key] = attr.val;
            };

            entities.forEach(function (ent) {
                 _.keys(ent.entity.attrs).forEach(function (attr) {
                    var containsKey = _.some($scope.conflictingAttrs, function(item) {
                                       return _.isEqual(item.key, attr);
                                   });
                    var containsVal = _.some($scope.conflictingAttrs, function(item) {
                                          return _.isEqual(item.val, ent.entity.attrs[attr]);
                                      });
                    if(containsKey && !containsVal) {
                        $scope.conflictingAttrs.push({key: attr, val: ent.entity.attrs[attr]});
                    }
                    else if($scope.finalAttrs[attr]) {
                        if($scope.finalAttrs[attr] != ent.entity.attrs[attr]) {
                            $scope.conflictingAttrs.push({key: attr, val: ent.entity.attrs[attr]});
                            $scope.conflictingAttrs.push({key: attr, val: $scope.finalAttrs[attr]});
                            delete $scope.finalAttrs[attr];
                        }
                    }
                    else {
                        $scope.finalAttrs[attr] = ent.entity.attrs[attr];
                    }
                });                               
            });

            $scope.conflictingAttrs = _.sortBy($scope.conflictingAttrs, function(item) {return item.key;});

            $scope.createEntity = function() {
                var targetId = 'merged-' + new Date().getTime();
                var incoming = _.map($scope.finalIncomingEdges, function (inEdge) {
                    return {
                        source: inEdge.source,
                        target: targetId,
                        attrs: {}
                    };
                });
                var outgoing = _.map($scope.finalOutgoingEdges, function (outEdge) {
                    return {
                        source: targetId,
                        target: outEdge.target,
                        attrs: {}
                    };
                });
                return  {
                    id: targetId,
                    attrs: $scope.finalAttrs,
                    edges: incoming.concat(outgoing)
                };
            };

            $scope.finalIncomingEdges = _.flatten(entities.map(function(ent){return ent.entity.edges.filter(function(edge){
                return ent.entity.id == edge.target;
            });}));

            $scope.finalOutgoingEdges = _.flatten(entities.map(function(ent){return ent.entity.edges.filter(function(edge){
                return ent.entity.id == edge.source;
            });}));


            $scope.removeAttribute = function (k,v) {
                delete $scope.finalAttrs[k];
                var insertIdx = _.sortedIndex($scope.conflictingAttrs, {key: k, val: v}, 'key');
                $scope.conflictingAttrs.splice(insertIdx, 0, {key: k, val: v});
            };
            $scope.addCustomAttribute = function () {
                if ($scope.custom.key && $scope.custom.value) {
                    $scope.addAttribute({key: $scope.custom.key, val: $scope.custom.value});
                    $scope.custom.key = "";
                    $scope.custom.value = "";
                }
            };
            $scope.removeEdge = function (edge) {
                var idx = null;
                $scope.finalIncomingEdges.forEach(function (d, i) {
                    if (d === edge) {
                        idx = i;
                    }
                });

                if (idx !== null) {
                    $scope.finalIncomingEdges.splice(idx, 1);
                } else {
                    $scope.finalOutgoingEdges.forEach(function (d, i) {
                        if (d === edge) {
                            idx = i;
                        }
                    });
                    $scope.finalOutgoingEdges.splice(idx, 1);
                }
            };
            $scope.ok = function() {
                var entity = $scope.createEntity();
                $modalInstance.close(entity);
            };
            $scope.cancel = function() {
                $modalInstance.dismiss();
            };
        },
        splitModalController = function ($scope, $modalInstance, entity) {
            var s1 = 'split-' + new Date().getTime(),
            s2 = 'split-' + new Date().getTime() + 1;
            entity.label = entity.entity.attrs.Name || entity.entity.attrs.name || entity.entity.attrs.actual_name || entity.entity.attrs.displayName || entity.entity.attrs.title || entity.entity.attrs.id;
            $scope.entity = entity;
            $scope.splits = [
                {
                    id: s1,
                    attrs: _.clone(entity.entity.attrs),
                    edges: entity.entity.edges.map(function (dat) {
                        var d = _.clone(dat);
                        if (d.source === entity.entity.id) {
                            d.source = s1;
                        }
                        if (d.target === entity.entity.id) {
                            d.target = s1;
                        }
                        return d;
                    }),
                    custom: {
                        key: '',
                        value: ''
                    }
                },
                {
                    id: s2,
                    attrs: _.clone(entity.entity.attrs),
                    edges: _.clone(entity.entity.edges).map(function (dat) {
                        var d = _.clone(dat);
                        if (d.source === entity.entity.id) {
                            d.source = s2;
                        }
                        if (d.target === entity.entity.id) {
                            d.target = s2;
                        }
                        return d;
                    }),
                    custom: {
                        key: '',
                        value: ''
                    }
                }
            ];
            $scope.ok = function () {
                $modalInstance.close($scope.splits);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss();
            };
            $scope.addSplit = function () {
                $scope.splits.push({
                    id: 'split-' + new Date().getTime(),
                    attrs: _.clone(entity.entity.attrs),
                    edges: entity.entity.edges.map(function (dat) {
                        var d = _.clone(dat);
                        if (d.source === entity.entity.id) {
                            d.source = s1;
                        }
                        if (d.target === entity.entity.id) {
                            d.target = s1;
                        }
                        return d;
                    }),
                    custom: {
                        key: '',
                        value: ''
                    }
                });
            };
            $scope.getIncomingEdges = function (s) {
                return s.edges.filter(function (e) {
                    return e.target === s.id;
                });
            };
            $scope.getOutgoingEdges = function (s) {
                return s.edges.filter(function (e) {
                    return e.source === s.id;
                });
            };
            $scope.addCustomAttribute = function (s) {
                if (s.custom.key && s.custom.value) {
                    s.attrs[s.custom.key] = s.custom.value;
                    s.custom.key = '';
                    s.custom.value = '';
                }
            };
            $scope.deleteAttr = function (attrs, key) {
                delete attrs[key];
            };
            $scope.deleteEdge = function (edges, edge) {
                var idx = edges.indexOf(edge);
                edges.splice(idx, 1);
            };
        };
        $scope.data = {
            raw: [],
            selection: [],
            network: []
        };
        $scope.searchData = {
            filters: {
                searchTypes: [],
                searchType: null,
                query: ''
            },
            isSearching: false
        };
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
            if (hasIdSearch) {
                for (var idx = $scope.searchData.filters.searchTypes.length - 1; idx >= 0; idx--) {
                    if ($scope.searchData.filters.searchTypes[idx].id === "OSERAF:search/id") {
                        $scope.searchData.filters.searchType = $scope.searchData.filters.searchTypes[idx];
                        break;
                    }
                }
            }
        });
        $scope.nickSearch = function() {
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
                $scope.searchData.isSearching = false;
            });
        };
        $scope.resolve = DataService.resolve; // TODO unused?
        $scope.resolveItem = function (d) {
            EntityOps.resolve({eId: d.entity.id}).$promise.then(function (resolutions) {
                resolveModal = $modal.open({
                    templateUrl: 'home/views/resolveModal.tpl.html',
                    controller: resolveModalController,
                    windowClass: 'resolve-modal-window',
                    backdrop: 'static',
                    resolve: {
                        resolutions: function () {
                            return resolutions;
                        },
                        entity: function () {
                            return d;
                        }
                    }
                });
                resolveModal.result.then(DataService.addNodes);
            });

        };
        $scope.deduplicate = function (d) {
            $scope.dedupeIsRunning = true;
            EntityOps.deduplicate().$promise.then(function (deduplications) {
                var sortedDedupes = deduplications.sort(function (a, b) { return b.score - a.score; });
                $scope.dedupeIsRunning = false;
                dedupeModal = $modal.open({
                    templateUrl: 'home/views/deduplicateModal.tpl.html',
                    controller: deduplicateModalController,
                    windowClass: 'deduplicate-modal-window',
                    backdrop: 'static',
                    resolve: { // angular resolve, not bullseye resolve
                        deduplications: function () {
                            return deduplications;
                        }
                    }
                });
                dedupeModal.result.then(DataService.deduplicate);
            });
        };
        $scope.splitItem = function (d) {
            splitModal = $modal.open({
                templateUrl: 'home/views/splitModal.tpl.html',
                controller: splitModalController,
                windowClass: 'split-modal-window',
                backdrop: 'static',
                resolve: { // angular resolve, not bullseye resolve
                    entity: function () {
                        return d;
                    }
                }
            });
            splitModal.result.then(function (splits) {
                EntityOps.split({eId: d.entity.id, entities: splits}).$promise.then(function (resultSplits) {
                    console.log(resultSplits);
//                    resultSplits = _.map(resultSplits, function)
                    DataService.split(d.entity.id, resultSplits);
                });
            });
        };
        $scope.mergeItems = function (entities) {
            mergeModal = $modal.open({
                templateUrl: 'home/views/mergeModal.tpl.html',
                controller: mergeModalController,
                windowClass: 'merge-modal-window',
                backdrop: 'static',
                resolve: { // angular resolve, not bullseye resolve
                    entities: function () {
                        return entities;
                    }
                }
            });
            mergeModal.result.then(function (ent) {
                var ids = entities.map(function (ent) {
                    return ent.entity.id;
                });
                
                EntityOps.merge({eIds: ids, entity: ent}).$promise.then(function (mergedEntity) {
                    DataService.merge(mergedEntity);
                });
            });
        };
        $scope.showNeighborhood = DataService.showNeighborhood;
        $scope.removeItem = DataService.removeItem;
    }])
    .factory('DataService', ['SearchTypes', 'EntityOps', function (SearchTypes, EntityOps) {
        var searchTypes = [],
        entityData = [],
        linkData = [];
        SearchTypes.query().$promise.then(function (types) {
            searchTypes = types;
        });
        return {
            showNeighborhood: function (entityScore) {
                EntityOps.neighborhood({eId: entityScore.entity.id}).$promise.then(function (neighborhoodGraph) {
                    neighborhoodGraph.nodes = _.map(neighborhoodGraph.nodes, function(node) {
                                                  node.label = node.attrs.Name || node.attrs.name || node.attrs.actual_name || node.attrs.displayName || node.attrs.title || node.attrs.id;
                                                  return node;
                                              });
                    var newHashedEntityData = {};
                    neighborhoodGraph.nodes.forEach(function (node) {
                        return newHashedEntityData[node.id] = {entity: node, group: "unselected", score: 0};
                    });
                    entityData.forEach(function (ent) {
                        if(newHashedEntityData[ent.entity.id]) {
                            delete newHashedEntityData[ent.entity.id];
                        }
                    });
                    var newEntities = _.map(newHashedEntityData, function(v,k){return v;});
                    var newHashedEdgeData = {};
                    neighborhoodGraph.edges.forEach(function (edge) {
                        return newHashedEdgeData[edge.source + edge.target] = {attrs: edge.attrs, source: edge.source, target: edge.target};
                    });
                    linkData.forEach(function(edge) {
                        if(newHashedEdgeData[edge.id]) {
                            delete newHashedEdgeData[edge.id];
                        }
                    });
                    var newEdges = _.map(newHashedEdgeData, function(v,k){return v;});
                    entityData = entityData.concat(newEntities);
                    linkData = linkData.concat(newEdges);
                });
            },
            removeItem: function(entityScore) {
                var edx = entityData.indexOf(entityScore);
                entityData.splice(edx, 1);

                linkData = linkData.filter(function (e) {
                    return e.source.id != entityScore.entity.id && e.target.id != entityScore.entity.id;
                });
            },
            getSearchTypes: function () {
                return searchTypes;
            },
            getEntityData: function () {
                return entityData;
            },
            search: function (query, searchType, cbEarly, cbLate) {
                return EntityOps.search({query: query, searchTypeId: searchType.id}).$promise.then(function (graph) {
                           graph.nodes = _.map(graph.nodes, function(node) {
                                             node.entity.label = node.entity.attrs.Name || node.entity.attrs.name || node.entity.attrs.actual_name || node.entity.attrs.displayName || node.entity.attrs.title || node.entity.attrs.id;
                                             return node;
                                         });
                    cbEarly && cbEarly();
                    entityData = graph.nodes.map(function (d) { d.group = 'unselected'; return d; });
                    linkData = graph.edges;
                    cbLate && cbLate();
                });
            },
            resolve: function (selection) { // TODO this method appears to be unused
                EntityOps.resolve({eId: selection.entity.id}).$promise.then(function (res) {
                    entityData = res.sort(function (a, b) { return b.score - a.score; }).map(function (d) { d.group = 'unselected'; return d; });
                    linkData = [];
                });
            },
            deduplicate: function(nodes) {
                entityData = nodes;
                linkData = [];
            },
            split: function (id, splits) {
                entityData = entityData.concat(splits.map(function (d) { return {entity: d, score: null}; }));
                var curEnts = {};
                _.forEach(entityData, function (node) {
                    curEnts[node.entity.id] = true;
                });
                _.forEach(splits, function (splitEnt) {
                    _.forEach(splitEnt.edges, function (edge) {
                        if (curEnts[edge.source] && curEnts[edge.target]) {
                            linkData.push(_.cloneDeep(edge));
                        }
                    });
                });
            },
            merge: function (mergedEntity) {
                entityData.push({entity: mergedEntity, score: null});
                var curEnts = {};
                _.forEach(entityData, function (node) {
                    curEnts[node.entity.id] = true;
                });
                _.forEach(mergedEntity.edges, function (edge) {
                    if (curEnts[edge.source] && curEnts[edge.target]) {
                        linkData.push(_.cloneDeep(edge));
                    }
                });
            },
            transformForNetwork: function () {
                return [entityData.map(function(d) { return d.entity; }), linkData];
            },
            addNodes: function (nodes) {
                var curIds = entityData.map(function (d) {
                    return d.entity.id;
                });
                if (nodes.length > 0) {
                    nodes.forEach(function (n) {
                        if (curIds.indexOf(n.entity.id) < 0) {
                            entityData.push(n);
                            curIds.push(n.entity.id);
                        }
                    });
                }
            }
        };
    }])
    .directive('networkView', [function () {
        return {
            restrict: 'E',
            scope: {
                data: '=',
                selection: '='
            },
            link: function (scope, element, attrs) {
                var viz,
                nodes = new vis.DataSet(),
                edges = new vis.DataSet(),
                el = d3.select(element[0])
                    .append('div')
                    .classed('network-view', true)
                    .style('position', 'relative')
                    .style('height', '100%')
                    .node();
                // it seems to be a little tricky to get node selection to cause the item to be selected in the listView
                // while unfortunate, for now, let's just turn off selection of nodes.
                // they still highlight when you select one in the listView.
                viz = new vis.Network(el, { nodes: nodes, edges: edges, options: { selectable: false } });
                scope.$watch('data', function (data) {
                    var removeNodes = nodes.getIds();
                    var newNodeData = _.map(data[0], function(node) {
                        var existsIndex = removeNodes.indexOf(node.id);
                        if (existsIndex >= 0) {
                            removeNodes.splice(existsIndex, 1);
                        }
                        return {
                            id: node.id,
                            label: node.attrs.Name || node.attrs.name || node.attrs.actual_name || node.attrs.displayName || node.attrs.title || node.attrs.id,
                            group: node.group || "unselected"
                        };
                    });
                    var newEdgeData = _.map(data[1], function(edge) {
                        var edgeId = "" + edge.source + "-" + edge.target;
                        return {
                            id: edgeId,
                            from: edge.source,
                            to: edge.target
                        };
                    });
                    nodes.remove(removeNodes);
                    nodes.update(newNodeData);
                    edges.update(newEdgeData);
                });
            }
        };
    }])
    .directive('listView', ['EntityOps', function (EntityOps) {
        return {
            restrict: 'E',
            scope: {
                data: '=',
                selection: '=',
                resolveItem: '&resolveitem',
                splitItem: '&splititem',
                showNeighborhood: '&showneighborhood',
                remove: '&'
            },
            templateUrl: 'home/views/listView.tpl.html',
            link: function (scope, element, attrs) {
                scope.select = function (d, event) {
                    var idx = scope.selection.indexOf(d);
                    if (event.ctrlKey || event.metaKey) {
                        if (idx < 0) {
                            d.entity.group = 'selected';
                            scope.selection.push(d);
                        } else {
                            d.entity.group = 'unselected';
                            scope.selection.splice(idx, 1);
                        }
                    } else {
                        scope.selection.forEach(function (d) { d.entity.group = 'unselected'; });
                        d.entity.group = 'selected';
                        scope.selection = [d];
                    }
                };
                scope.getScoreClass = function (score) {
                    if (score >= 0.85) {
                        return 'success';
                    } else if (score >= 0.70) {
                        return 'warning';
                    }
                    return 'danger';
                };
                scope.round = function(score) {
                    return Math.round(100.0 * score);
                };
            }
        };
    }])
    .directive('detailView', [function () {
        return {
            restrict: 'E',
            scope: {
                data: '=',
                mergeItems: '&mergeitems'
            },
            templateUrl: 'home/views/detailView.tpl.html',
            link: function (scope, element, attrs) {
                scope.getHeader = function () {
                    if (scope.data.length === 0) {
                        return 'Selected Entity Details';
                    } 
                    else if (scope.data.length == 1) {
                        return scope.data[0].entity.label.substring(0, 75);
                    }
                    else {
                        return 'Merge Selected Entities';
                    }
                };
            }
        };
    }]);
