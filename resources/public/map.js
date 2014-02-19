function map_init(lat, lng) {
    var myOptions = {
		zoom: 12,
		center: new google.maps.LatLng(lat,lng),
		mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    var map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);

    // var image = '/fpublic/beachflag.png';
    // new google.maps.Marker({
    // 	position: new google.maps.LatLng(lat,lng),
    // 	map: map,
    // 	icon: image,
    // 	title: "You are here",
	
    // });

    // load data
    $.ajax("/api?count=900", {
		//	data: {lat: lat, lng: lng},
		success:
		function(response) {
			displayIncidents(map, response);
		},
		error: 
		function(XHR, textStatus, errorThrown) {alert("error: " + textStatus + "; " + errorThrown);}
    });

}

function displayIncidents(map, incidents) {
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
			prepMarker(marker, incident, map, w);
		}
    }
}

function incidentTimeString(incident) {
    return new Date(incident.time).toLocaleString();
}


function prepMarker(marker, incident, map, w) {
    google.maps.event.addListener(marker, 'click', function() {
	w.setContent(incident.type + ' ' + incidentTimeString(incident) + '<br/>' + incident.description);
	w.open(map, marker);
    });
}

$(document).ready(function() { map_init(37.621592, -122.4885218)});
				   
