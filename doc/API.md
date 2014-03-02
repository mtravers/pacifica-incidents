# API Documentation for Incidents

## Endpoints

### /api
	Returns all of the incidents, sorted by date of incident in reverse order
#### Parameters
	All parameters are optional. With no parameters, returns all.
#####	count (integer)
	Max number of records to return
#####	min (javascript timestamp)
	Minimum date of record set to return
	(Params must include max as well in order to be effective)
#####	max (javascript timestamp)
	Maximum date of record set to return
	(Params must include min as well in order to be effective)
#####	lat (double)
	Latitude of geos to constrain results by (only at ths location)
	(Params must include lng as well in order to be effective)
#####	lng (double)
	Longitude of geos to constrain results by (only at ths location)
	(Params must include lat as well in order to be effective)
#### Response
```javascript
[{"geo":{"lat":37.5908217,
		 "lng":-122.4710365},
  "address":"Kathleen Ct. , Pacifica, CA",
  "time":1392709980000,
  "type":"Suspicious Circumstances",
  "id":140217231,
  "disposition":"Log Note Only.",
  "description":"Occurred on Kathleen Ct. , Pacifica. GARAGE DOOR IS WIDE OPEN \/\/ ALL HOUSE LIGHTS OFF \/\/ "},
  // ...
]
```
### /api/geos
	Returns of the geocodes in the database, and the most recent incident for each geocode, sorted by date of most recent incident at each geocode, in reverse order
#### Parameters
	All parameters are optional. With no parameters, returns all.
#####	count (integer)
	Max number of records to return
#####	min (javascript timestamp)
	Minimum date of record set to return
	(Params must include max as well in order to be effective)
#####	max (javascript timestamp)
	Maximum date of record set to return
	(Params must include min as well in order to be effective)
#### Response
```javascript
[{"geo":{"lat":37.5874805,
		 "lng":-122.4984811},
  "address":"Bower Rd, Pacifica, CA",
  "time":1391559600000,
  "type":"Traffic Law Abandoned",
  "id":140204177,
  "disposition":"Log Note Only.",
  "description":"Occurred on Bower Rd, Pacifica. Motor home parked in roadway \/\/ vehicle has been there since the weekend "}
  ...
  ]
```
### /api/keys/disposition
	Returns all of the set of dispositions in the database
#### Parameters
	None
#### Response
```javascript
[null,
 "",
 "False  Alarm.",
 "Report Taken. .",
 "Field Interview  from Incident.",
 " Cover Unit.",
 "Cover Unit.",
 // ...
 ]
```
### /api/stats/disposition
	Returns the count of each disposition in the database, sorted by count, in reverse order.
#### Parameters
	None yet (TBD, will support date, count, and geo)
#### Response
```javascript
[["Log Note Only.",7153],
 ["Report Taken.",3994],
 ["Gone On Arrival.",2508],
 ["Checks Ok.",2346],
 ["Abated\/Advised.",1347],
 ["Citation.",852],
 // ...
 ]
```
### /api/keys/type
	Returns all of the set of types in the database
#### Parameters
	None
#### Response
```javascript
["Dist Noise",
 "Missing Adult",
 "Damaged Property",
 "Citizen Assist",
 "Assault and Battery",
 //...
 ]
```
### /api/stats/type
	Returns the count of each type in the database, sorted by count, in reverse order.
#### Parameters
	None Yet (TBD, will support date, count, and geo)
#### Response
```javascript
[["Traffic Law Vehicle",3076],
 ["Traffic Law Abandoned",2281],
 ["Citizen Assist",1469],
 ["School Check",1440],
 ["Subject Stop",1427],
 ["SPCA Case",1252],
 ["Suspicious Circumstances",1160],
 ["Alarm",1055],
 // ..
 ]
```
### /api/dates
	Returns the min and max timestamps in the database
#### Parameters
	None
#### Response
```javascript
{"max":1392709980000,
 "min":1338366000000}
```
### /api/docs
	Returns this document, formatted as HTML
### /api/status
	Returns useful status of the database, various total counts, mins and maxes, etc.
#### Parameters
	None
#### Response
```javascript
{"total-incidents":26652,
 "total-types":26652,
 "total-dispositions":26362,
 "total-descriptions":26362,
 "total-geos":24874,
 "total-addresses":24889,
 "min-max-days":{"max":"2014-02-17",
				 "min":"2012-05-30"},
 "min-max-timestamps":{"max":1392709980000,
					   "min":1338366000000}}
```

## General
### JSONP
	Just add callback=foo argument to any query in the above endpoints, and you'll get back jsonp, i.e the result wrapped as an argument to foo()
### Authentication
	Nothing is authenticated at this time.
