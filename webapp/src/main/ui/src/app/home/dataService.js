angular.module('bullseye.dataService', [
    'bullseye.http',
    'bullseye.util'
])
    .factory('DataService', ['SearchTypes', 'EntityOps', 'UtilService', function (SearchTypes, EntityOps, UtilService) {
        var searchTypes = [],
        entityData = [],
        linkData = [];
        SearchTypes.query().$promise.then(function (types) {
            searchTypes = types;
        });
        return {
            showNeighborhood: function (entityScore) {
                EntityOps.neighborhood({eId: entityScore.entity.id}).$promise.then(function (neighborhoodGraph) {
                    neighborhoodGraph.nodes = _.map(neighborhoodGraph.nodes, UtilService.addDisplayNameLabel);
                    var newHashedEntityData = {};
                    neighborhoodGraph.nodes.forEach(function (node) {
                        return newHashedEntityData[node.id] = {entity: node, score: 0};
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
                    cbEarly && cbEarly();
                    entityData = graph.nodes.map(function (d) {
                        d.entity.label = UtilService.formatDisplayName(d.entity);
                        return d;
                    });
                    linkData = graph.edges;
                    cbLate && cbLate();
                });
            },
            deduplicate: function(nodes) {
                entityData = nodes;
                linkData = [];
            },
            split: function (id, splits) {
                entityData = entityData.concat(splits.map(function (d) {
                    return {entity: UtilService.addDisplayNameLabel(d), score: null};
                }));
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
                entityData.push({entity: UtilService.addDisplayNameLabel(mergedEntity), score: null});
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
    }]);