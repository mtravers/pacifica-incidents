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
function map_update(start_end) {
    // load data
    $.ajax("/api/geos", {
	data: {min: start_end.start, max: start_end.end},
	success:
	function(response) {
	    displayIncidents(map, response, start_end);
	},
	error: 
	function(XHR, textStatus, errorThrown) {alert("error: " + textStatus + "; " + errorThrown);}
    });
}


function displayIncidents(map, incidents, start_end) {
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
	    prepMarker(marker, incident, map, w, start_end);
	}
    }
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

function prepMarker(marker, incident, map, w, start_end) {
    google.maps.event.addListener(marker, 'click', 
				  function() {
					  var data = {lat: incident.geo.lat, lng: incident.geo.lng};
					  if(start_end.start !== null && start_end.start > 0 &&
						 start_end.end !== null && start_end.end > 0){
						  data.min = start_end.start;
						  data.max = start_end.end;
					  }
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
    $("#update").click(function(event) {
		var start_end = get_datepickers_as_timestamps();
		map_update(start_end);
		return false;
    });
}

$(document).ready(function() {
    $( "#start_datepicker" ).datepicker();
    $( "#end_datepicker" ).datepicker();
    prepare_form();
    map_init(37.621592, -122.4885218)
    ;});

