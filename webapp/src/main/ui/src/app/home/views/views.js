angular.module('bullseye.home.views', [
    'bullseye.util',
    'bullseye.http'
])
    .directive('networkView', ['UtilService', function (UtilService) {
        return {
            restrict: 'E',
            scope: {
                data: '=',
                selection: '=',
                select: '&select',
                showNeighborhood: '&showneighborhood'
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
                viz = new vis.Network(el, { nodes: nodes, edges: edges });
                viz.on('select', function (properties) {
                    scope.select()(properties.nodes);
                    try {
                        scope.$root.$digest(); // sinner
                    } catch (e) {
                    }
                });
                viz.on('doubleClick', function (properties) {
                    if (_.size(properties.nodes) > 0) {
                        scope.showNeighborhood()({ entity: { id: properties.nodes[0] } });
                    }
                });
                scope.$watch('selection', function (entityIds) {
                    viz.selectNodes(entityIds);
                });
                scope.$watch('data', function (data) {
                    var removeNodes = nodes.getIds();
                    var newNodeData = _.map(data[0], function(node) {
                        var existsIndex = removeNodes.indexOf(node.id);
                        if (existsIndex >= 0) {
                            removeNodes.splice(existsIndex, 1);
                        }
                        return {
                            id: node.id,
                            label: UtilService.formatDisplayName(node),
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
                select: '&select',
                resolveItem: '&resolveitem',
                splitItem: '&splititem',
                showNeighborhood: '&showneighborhood',
                remove: '&'
            },
            templateUrl: 'home/views/tpls/listView.tpl.html',
            link: function (scope, element, attrs) {
                scope.handleClick = function (d, event) {
                    var idx = scope.selection.indexOf(d.entity.id);
                    if (event.ctrlKey || event.metaKey) {
                        if (idx < 0) {
                            scope.select()(scope.selection.concat([d.entity.id]));
                        } else {
                            scope.select()(scope.selection.slice(0, idx).concat(scope.selection.slice(idx + 1)));
                        }
                    } else {
                        scope.select()([d.entity.id]);
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
            templateUrl: 'home/views/tpls/detailView.tpl.html',
            link: function (scope, element, attrs) {
                scope.getHeader = function () {
                    if (scope.data.length === 0) {
                        return 'Selected Entity Details';
                    }
                    else if (scope.data.length == 1) {
                        return scope.data[0].label;
                    }
                    else {
                        return 'Merge Selected Entities';
                    }
                };
            }
        };
    }]);