/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


var GetEvidence = (function(){
    function init(data, callback) {
        var accessableFunc = function(){
            getEvidence(data, callback);
        };
        var unaccessableFunc = function() {
            callback(data);
        };
        oncokbAccess(accessableFunc, unaccessableFunc);
    }

    function oncokbAccess(accessableFunc, unaccessableFunc) {
        $.get('http://miso-dev.cbio.mskcc.org:28080/oncokb/',accessableFunc)
            .fail(unaccessableFunc);
    }
    
    function getEvidence(genomicEventObs, callback) {
        var mutationEventIds = genomicEventObs.mutations.getEventIds(false),
            genes = [],
            alterations = [];
    
        for(var i=0, mutationL = mutationEventIds.length; i < mutationL; i++) {
            genes.push(genomicEventObs.mutations.getValue(mutationEventIds[i], 'gene'));
            alterations.push(genomicEventObs.mutations.getValue(mutationEventIds[i], 'aa'));
        }
        $.get('http://miso-dev.cbio.mskcc.org:28080/oncokb/evidence.json?hugoSymbol=' + 
                genes.join(',') + 
                '&alteration='+
                alterations.join(','),
            function(data) {
                console.log(data);
                callback(genomicEventObs);
            });
    }
    return {
        init: init
    };
})();