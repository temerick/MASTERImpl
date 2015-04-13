angular.module('bullseye.modal.resolve', [
    'ngResource',
    'bullseye.util'
])
    .service(
        'ResolveModal',
        function (ModalUtils, UtilService) {
            var cleanResolutions = function (resolutions) {
                return _.map(resolutions, function(resource) {
                    resource.entity.label = UtilService.formatDisplayName(resource.entity);
                    return resource;
                });
            };
            var getScoreClass = function (score) {
                if (score >= 0.85) {
                    return 'success';
                } else if (score >= 0.70) {
                    return 'warning';
                }
                return 'danger';
            };
            var round = function(score) {
                return Math.round(100.0 * score);
            };
            var modalController = function ($scope, $modalInstance, entity, resolutions) {
                $scope.resolutions = cleanResolutions(resolutions);
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
                $scope.getScoreClass = getScoreClass;
                $scope.round = round;
            };
            this.openModal = function (entity, resolutions) {
                return ModalUtils.open(
                    'modal/tpls/resolveModal.tpl.html',
                    modalController,
                    'resolve-modal-window',
                    { 'entity': entity, 'resolutions': resolutions }
                );
            };
        });
