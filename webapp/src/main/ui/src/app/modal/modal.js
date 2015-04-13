angular.module('bullseye.modal', [
    'bullseye.modal.resolve',
    'bullseye.modal.deduplicate',
    'bullseye.modal.merge',
    'bullseye.modal.split'
])
    .service('Modals', function($modal, ResolveModal, DeduplicateModal, MergeModal, SplitModal) {
        this.resolve = ResolveModal;
        this.deduplicate = DeduplicateModal;
        this.merge = MergeModal;
        this.split = SplitModal;
    })
    .service('ModalUtils', function ($modal) {
        var makeResolveData = function (modalData) {
            var rdata = {};
            _.each(_.keys(modalData), function (key) {
                rdata[key] = function () { return modalData[key]; };
            });
            return rdata;
        };
        this.open = function (templateUrl, controller, windowClass, modalData) {
            var modalArgs = {
                templateUrl: templateUrl,
                controller: controller,
                windowClass: windowClass,
                backdrop: 'static',
                resolve: makeResolveData(modalData)
            };
            return $modal.open(modalArgs);
        };
    });