angular.module('bullseye.modal.split', [
    'ngResource',
    'bullseye.util'
])
    .service(
        'SplitModal',
        function (ModalUtils, UtilService) {
            var makeSplit = function (id, entity) {
                return {
                    id: id,
                    attrs: _.clone(entity.attrs),
                    edges: entity.edges.map(function (dat) {
                        var d = _.clone(dat);
                        if (d.source === entity.id) {
                            d.source = id;
                        }
                        if (d.target === entity.id) {
                            d.target = id;
                        }
                        return d;
                    }),
                    custom: {
                        key: '',
                        value: ''
                    }
                };
            };
            var modalController = function ($scope, $modalInstance, entity) {
                var s1 = 'split-' + new Date().getTime(),
                    s2 = 'split-' + new Date().getTime() + 1;
                $scope.entity = entity;
                $scope.entity.label = UtilService.formatDisplayName(entity.entity);
                $scope.splits = [
                    makeSplit(s1, entity.entity),
                    makeSplit(s2, entity.entity)
                ];
                $scope.ok = function () {
                    var splits = $scope.splits;
                    _.each(splits, function(split) {
                        split.label = UtilService.formatDisplayName(split);
                    });
                    $modalInstance.close(splits);
                };
                $scope.cancel = function () {
                    $modalInstance.dismiss();
                };
                $scope.addSplit = function () {
                    $scope.splits.push(makeSplit('split-' + new Date().getTime(), entity.entity));
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
            this.openModal = function (entity) {
                return ModalUtils.open(
                    'modal/tpls/splitModal.tpl.html',
                    modalController,
                    'split-modal-window',
                    { 'entity': entity }
                );
            };
        });