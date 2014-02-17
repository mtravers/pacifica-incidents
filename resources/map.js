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
    $.ajax("/map_data", {
	data: {lat: lat, lng: lng},
	success:
	function(response) {
	    var markets = JSON.parse(response);
//	    displayMarkets(map, markets);
	},
	error: 
	function(XHR, textStatus, errorThrown) {alert("error: " + textStatus + "; " + errorThrown);}
    });

}
