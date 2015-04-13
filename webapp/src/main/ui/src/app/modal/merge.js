angular.module('bullseye.modal.merge', [
    'ngResource',
    'bullseye.util'
])
    .service(
        'MergeModal',
        function (ModalUtils, UtilService) {
            var modalController = function($scope, $modalInstance, entities) {
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
                     _.keys(ent.attrs).forEach(function (attr) {
                        var containsKey = _.some($scope.conflictingAttrs, function(item) {
                                           return _.isEqual(item.key, attr);
                                       });
                        var containsVal = _.some($scope.conflictingAttrs, function(item) {
                                              return _.isEqual(item.val, ent.attrs[attr]);
                                          });
                        if(containsKey && !containsVal) {
                            $scope.conflictingAttrs.push({key: attr, val: ent.attrs[attr]});
                        }
                        else if($scope.finalAttrs[attr]) {
                            if($scope.finalAttrs[attr] != ent.attrs[attr]) {
                                $scope.conflictingAttrs.push({key: attr, val: ent.attrs[attr]});
                                $scope.conflictingAttrs.push({key: attr, val: $scope.finalAttrs[attr]});
                                delete $scope.finalAttrs[attr];
                            }
                        }
                        else {
                            $scope.finalAttrs[attr] = ent.attrs[attr];
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

                $scope.finalIncomingEdges = _.flatten(entities.map(function(ent){return ent.edges.filter(function(edge){
                    return ent.id == edge.target;
                });}));

                $scope.finalOutgoingEdges = _.flatten(entities.map(function(ent){return ent.edges.filter(function(edge){
                    return ent.id == edge.source;
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
            };
            this.openModal = function (entities) {
                return ModalUtils.open(
                    'modal/tpls/mergeModal.tpl.html',
                    modalController,
                    'merge-modal-window',
                    { 'entities': entities }
                );
            };
        });