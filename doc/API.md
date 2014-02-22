# API Documentation for Incidents

## Endpoints

### /api
	Returns all of the incidents
#### Parameters
#####	count (integer)
	Max number of records to return
#####	min (unix/javascript timestamp)
	Minimum date of record set to return
	(Params must include max as well in order to be effective)
#####	max (unix/javascript timestamp)
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
	Returns of the geocodes in the database, and the most recent incident for each geocode.
#### Parameters
#####	count (integer)
	Max number of records to return
#####	min (unix/javascript timestamp)
	Minimum date of record set to return
	(Params must include max as well in order to be effective)
#####	max (unix/javascript timestamp)
	Maximum date of record set to return
	(Params must include min as well in order to be effective)
#### Response
```javascript
[{"geo":null,
  "address":null,
  "time":1392541320000,
  "type":"Traffic Law Vehicle",
  "id":140216013,
  "disposition":null,
  "description":null},
  // ...
  ]
```
### /api/dispositions
	Returns all of the set of dispositions in the database
### /api/dispositions/stats
	Returns the count of each disposition in the database, sorted by count, in reverse order.
### /api/types 
	Returns all of the set of types in the database
### /api/types/stats 
	Returns the count of each type in the database, sorted by count, in reverse order.
### /api/dates
	Returns the min and max timestamps in the database
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

