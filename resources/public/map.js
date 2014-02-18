function map_init(lat, lng) {
    var myOptions = {
		zoom: 10,
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
    $.ajax("/api?count=250", {
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
			var loc = incident.geo.geometry.location;
			var marker = new google.maps.Marker({
				position: new google.maps.LatLng(loc.lat,loc.lng),
				map: map,
				zIndex: z--,
				title: incident.name});
			prepMarker(marker, incident, map, w);
		}
    }
}

// not working at all yet, placeholder +++
function prepMarker(marker, incident, map, w) {
    google.maps.event.addListener(marker, 'click', function() {
		//	w.setContent(incident.name + '<br/>' + incident.phone);
		$.ajax("/marker-info", {
			data: {m: incident.index},
			success:
			function(response) {
				w.setContent(response);	    
			}});
		w.open(map, marker);
    });
}


$(document).ready(function() { map_init(37.6687476,-122.4836863)});
				   
