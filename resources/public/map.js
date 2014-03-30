var map;
var markers = [];

function map_init(lat, lng) {
    var myOptions = {
	zoom: 12,
	center: new google.maps.LatLng(lat,lng),
	mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);

    // load data
    $.ajax("/api/geos", {
	//	data: {lat: lat, lng: lng},
	success:
	function(response) {
	    displayIncidents(map, response);
	},
	error: 
	function(XHR, textStatus, errorThrown) {alert("error: " + textStatus + "; " + errorThrown);}
    });
}

// left out of maps api
function clearMarkers() {
    for(var i=0; i < markers.length; i++){
        markers[i].setMap(null);
    }
    markers = new Array();
};


// really should take other params, prob in a map
function map_update(params) {
    // load data
    $.ajax("/api/geos", {
	data: params,
	success:
	function(response) {
	    displayIncidents(map, response, params);
	},
	error: 
	function(XHR, textStatus, errorThrown) {alert("error: " + textStatus + "; " + errorThrown);}
    });
}


function displayIncidents(map, incidents, params) {
    clearMarkers();
    var z = 1000;
    var w = new google.maps.InfoWindow();
    for (idx in incidents) {
	var incident = incidents[idx];
	if ('geo' in incident && null !== incident.geo) {
	    var loc = incident.geo;
	    var marker = new google.maps.Marker({
		position: new google.maps.LatLng(loc.lat,loc.lng),
		map: map,
		zIndex: z--,
		title: incident.name});
	    markers.push(marker);
	    prepMarker(marker, incident, map, w, params);
	}
    }
    $("#loading_data").hide();
}

function incidentTimeString(incident) {
    return new Date(incident.time).toLocaleString();
}

function formatIncidentList(incidents){
    var acc = "";
    $('.gm-style').removeClass('gm-style');
    for (idx in incidents) {
	var incident = incidents[idx];
	acc = acc  + "<div class='incident'><span class='incidenttype'>" + incident.type + '</span> ' 
	    + "<span class='datestamp'>" + incidentTimeString(incident) + '</span><br/>' 
	    + "<span class='description'>" + incident.description + "</span></div>";
    }
    return acc;
}

function showIncidentDetails(response, map, marker, w) {
    w.setContent(formatIncidentList(response));
    w.open(map, marker);
};

function prepMarker(marker, incident, map, w, params) {
    google.maps.event.addListener(marker, 'click', 
				  function() {
				      var data = $.extend({}, params, {lat: incident.geo.lat, lng: incident.geo.lng});
				      $.ajax("/api",
					     { data: data,
					       success:
					       function(response) {
						   showIncidentDetails(response, map, marker, w);
					       },
					       error: 
					       function(XHR, textStatus, errorThrown) {
						   alert("error: " + textStatus + "; " + errorThrown);}
					     });
				  });};

function get_datepickers_as_timestamps(){
    var start = $('#start_datepicker').datepicker("getDate");
    var end = $('#end_datepicker').datepicker("getDate");
    return {start: start !== null ? start.getTime() : 0,
	    end:  end !== null ? end.getTime() : 0};
}

function prepare_form() {
    $("#start_datepicker").datepicker();
    $("#end_datepicker").datepicker();
    $("#update").click(function(event) {
        var start_end = get_datepickers_as_timestamps();
	var text = $('#text_search').val();
	var params = {};
	// I know this sucks, but it's gotta happen somewhere, might as well be here.
	if(start_end !== undefined && 
	   start_end.end !== null && start_end.end > 0 && 
	   start_end.start !== null && start_end.start > 0 ){
		params.min = start_end.start;
		params.max = start_end.end;
	}
	if(text !== undefined && text !== ""){
		params.search = text;
	}
	map_update(params);
	return false;
    });
}

function fillDetails(api_data) {
    $("#start_datepicker").datepicker("setDate", new Date(api_data["min"]));
    $("#end_datepicker").datepicker("setDate", new Date(api_data["max"]));
}

$(document).ready(function() {
    prepare_form();
    map_init(37.621592, -122.4885218);
    $.ajax("/api/dates",
	   { success:
	   function(response) {
	       fillDetails(response);
	   },
	     error: 
	     function(XHR, textStatus, errorThrown) {
		 alert("error: " + textStatus + "; " + errorThrown);}
	   });
    });

