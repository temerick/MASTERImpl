angular.module('bullseye.modal.deduplicate', [
    'ngResource',
    'bullseye.util'
])
    .service(
        'DeduplicateModal',
        function (ModalUtils, UtilService) {
            var modalController = function ($scope, $modalInstance, deduplications) {
                $scope.deduplications = _.map(deduplications, function(resource) {
                    _.each(resource.entities, UtilService.addDisplayNameLabel);
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
                            nodes.push({entity: e, score: n.score});
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
            };
            this.openModal = function (deduplications) {
                return ModalUtils.open(
                    'modal/tpls/deduplicateModal.tpl.html',
                    modalController,
                    'deduplicate-modal-window',
                    { 'deduplications': deduplications }
                );
            };
        });