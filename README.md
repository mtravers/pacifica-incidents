# incidents

Web service to display and map incident reports from the [Pacifica police blotter](http://www.cityofpacifica.org/depts/police/media/media_bulletin.asp).

[See it](http://pacifica-incidents.herokuapp.com/). Note: because it՚s hosted on Heroku՚s free tier, it now can take up to 20 seconds to start up after being idle. 

## STATUS

Rewritten:
- to use AWS text extraction
- to have a new stupid text "database"
- to conform with Maps API (needs a key now)
- to not group geos

TODO:
- integrate the scrape / analyze / geo stuff into an automated pipeline
- redo geo grouping if necessary
- redo frontend in cljs (if it gets any more complicated)
- clean up, there's lots of dead code


## Usage

Start server: (TODO Not working, use lein run?)
> lein ring server

### Dev notes

Database is in s3://incidents/b/latest.edn (should be versioned)

    aws s3 ls s3://incidents --profile=incidents

## License

Copyright © 2014-2021 Concerned Hackers of Pacifica

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
