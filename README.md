silkdeduplication
=================

An example application for interlinking of spatial data

The purpose of the application is to enable a user to post an RDF data set about locations in the Province of Trento
and get the result of the interlinking process against a local data set through a SPARQL endpoint. Both data sets,
the source that must be imported in Jena, and the target that must be sent via http POST to the server can be downloaded 
from the Open Data Portal of the Province of Trento (PAT).
The target data file, that represents the local knowledge base against which the interlinking will be performed, can be downloaded from the url

http://dati.trentino.it/dataset/localita-geografiche-1991-671556

It contains information about 1800 locations in the province of Trento, from the municipalities, to small villages and farms like area, perimeter, and a polygon in UTM coordinates. The same file is available in extractor/src/test/resources/pat-locgeo.rdf



