angular.module('bullseye.util', [
])
    .service('UtilService', function() {
        var displayName = function(entity) {
            return entity.attrs.displayName || entity.attrs.Name || entity.attrs.name || entity.attrs.actual_name ||
                entity.attrs.url || entity.attrs.title ||
                entity.attrs.attribute_value ||
                (entity.attrs.entity_type ? entity.id + " (" + entity.attrs.entity_type + ")" : false) ||
                entity.attrs.disambiguated_name ||
                entity.attrs.label ||
                entity.id;
        };
        this.formatDisplayName = displayName;
        this.addDisplayNameLabel = function(entity) {
            entity.label = displayName(entity);
            return entity;
        };
    });