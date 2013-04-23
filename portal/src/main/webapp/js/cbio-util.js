var cbio = {};

cbio.util = (function() {

    var toPrecision = function(number, precision, threshold) {
        // round to precision significant figures
        // with threshold being the upper bound on the numbers that are
        // rewritten in exponential notation

        if (0.000001 <= number && number < threshold) {
            return number.toExponential(precision);
        }
        
        return number.toPrecision(precision).replace(/\.*0+$/,''); // trim 0s
    };
    
    var size = function(obj) {
        var n = 0, key;
        for (key in obj) {
            if (obj.hasOwnProperty(key)) n++;
        }
        return n;
    };

    return {
        toPrecision: toPrecision,
        size: size
    };

})();
