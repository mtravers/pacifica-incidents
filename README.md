# incidents

Web service to display and map incident reports from the [Pacifica police blotter](http://www.cityofpacifica.org/depts/police/media/media_bulletin.asp).

[See it](http://pacifica-incidents.herokuapp.com/). Note: because it՚s hosted on Heroku՚s free tier, it now can take up to 20 seconds to start up after being idle. 

## STATUS

Broken, the version here has been updated to the point where it compiles.

known problems:
- data is now at https: and slurp won't work on it
- the postgres library is broken (currently using a file persistence hack which may or may not work)
- google map api seems unhappy


## Usage

Start server:
> lein ring server

## License

Copyright © 2014-2015 Concerned Hackers of Pacifica

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
