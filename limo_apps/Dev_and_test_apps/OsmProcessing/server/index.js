$(document).ready(function() {
    $("#wait").hide();
    var request;
    $("#theForm").submit(function(event) {
	var $form = $(this);
	var formdata = $form.serialize();
	$.ajax({
	    url: "/cgi-bin/get_region.py",
	    type: "POST",
	    data: formdata,
	    success: function(response) {
		$("#result").html(response)
		$.ajax({
		    url: "/cgi-bin/is_it_mapped.py",
		    type: "POST",
		    data: formdata,
		    success: function(response_2) {
			$("#wait").hide();
			var text = $("#result").html();
			$("#result").html(text + response_2)
		    }
		});
	    },
	    error: function(response) {
		$("#result").html("An error occured!");
	    }

	});
	$("#wait").show();
	$("#result").html("");
	event.preventDefault();
    });
});
