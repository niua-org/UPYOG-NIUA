$(document).ready(function () {
    var functionName = new Bloodhound({
        datumTokenizer: function (datum) {
            return Bloodhound.tokenizers.whitespace(datum.value);
        },
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        remote: {
            url: '/services/EGF/function/getByNameOrCode?query=%QUERY', // '/services/EGF/common/ajaxfunctionnames?name=%QUERY',
            filter: function (data) {
                return $.map(data, function (ct) {
                    return {
                        code: ct.split("~")[0].split("-")[0],
                        name: ct.split("~")[0].split("-")[1],
                        id: ct.split("~")[1],
                        codeName: ct
                    };
                });
            }
        }
    });

    functionName.initialize();
    $('#function').typeahead({
        hint: true,
        highlight: true,
        minLength: 3
    }, {
        displayKey: 'name',
        source: functionName.ttAdapter()
    }).on('typeahead:selected', function (event, data) {
        $("#functionId").val(data.id);
    });

});
//$('#function').blur(function () {
//    if ($('.cfunction').val() == "") {
//        bootbox.alert("Please select function from dropdown values", function () {
//            $('#function').val("");
//        });
//    }
//});

// Clear ID if user manually edits
$('#functionName').on('input', function () {
    $('#functionId').val("");
});