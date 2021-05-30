var map;
var markers = [];


function map_init(lat, lng) {
    var myOptions = {
	zoom: 12,
	center: new google.maps.LatLng(lat,lng),
	mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);
    map_update(defaultParams());
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
    $.ajax("/api", {
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
    cache = {};
    var z = 1000;
    var w = new google.maps.InfoWindow();
    for (idx in incidents) {
	var incident = incidents[idx];
	var loc = incident.geo;
	var marker = new google.maps.Marker({
	    position: new google.maps.LatLng(loc.lat,loc.lng),
	    map: map,
	    zIndex: z--,
	    title: incident.type});
	markers.push(marker);
	prepMarker(marker, incident, map, w, params);
    }
    $("#loading_data").hide();
}

function incidentTimeString(incident) {
    return new Date(incident.datime).toLocaleString();
}

function formatIncident(incident) {
    var acc = "";
    $('.gm-style').removeClass('gm-style');
    acc = acc  + "<div class='incident'><span class='incidenttype'>" + incident.type + '</span> ' 
	    + "<span class='datestamp'>" + incidentTimeString(incident) + '</span><br/>' 
	    + "<span class='description'>" + incident.location + "</span></div>";
    return acc;
}

function showIncidentDetails(incident, map, marker, w) {
    w.setContent(formatIncident(incident));
    w.open(map, marker);
};

function prepMarker(marker, incident, map, w, params) {
    google.maps.event.addListener(marker, 'click', 
				  function() {
				      // console.log(incident)
				      showIncidentDetails(incident, map, marker, w);
				  });
};

function get_datepickers_as_timestamps(){
    var start = $('#start_datepicker').datepicker("getDate");
    var end = $('#end_datepicker').datepicker("getDate");
    return {start: start && start.getTime(),
	    end:   end && end.getTime()};
}

function update() {
    map_update(params);
}

function init_datepicker(field, v) {
    $(field).datepicker();
    $(field).datepicker("setDate", new Date(v));
}

function prepare_form() {
    var params = defaultParams();
    init_datepicker("#start_datepicker", params.min);
    init_datepicker("#end_datepicker", params.max);
    $("#update").click(function(event) {
	var start_end = get_datepickers_as_timestamps();
	var text = $('#text_search').val();
	var params = {};
	if (start_end.start) { params.min = start_end.start; }
	if (start_end.end) { params.max = start_end.end; }
	if (text !== undefined && text !== ""){
	    params.search = text;
	}
	map_update(params);
	return false;
    });
}

function dateDaysAgo(date, days) {
    return new Date(date.getTime() - 1000 * 3600 * 24 * days);
}

// called after map api code is loaded
function initMap() {
    prepare_form();
    map_init(37.621592, -122.4885218);
}


function defaultParams() {
    return { "max": new Date().getTime(),
	     "min": dateDaysAgo(new Date(), 7).getTime()
	   };
}



