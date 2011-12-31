MSquareHttpd 
============

Philosophy
----------

I love Node.JS, but I like proper IDE support too. So, when I read this [blog-post on M2HTTP by Matt Might](http://matt.might.net/articles/pipelined-nonblocking-extensible-web-server-with-coroutines/), it sparked my interest. 

The goal of this project is to create a Scala webserver that is meant for developers to start as a basis for programming their backend. It is not an attempt to recreate NodeJs in Scala, but I do strive for the same simplicity of NodeJs. Instead of the event-driven javascript, Scala has very good Actors which are the building blocks of MSquareHttpd.

Design
------

The original design in M2HTTPD by Matt is based on coroutines, implemented using queues. MSquareHttpd uses the actors to communicate messages. This maintains the non-blocking nature of each building-block, but hides the queues and adds an event-driven aspect.

As in coroutines, there are Producers and Consumers. A Producer holds a list of Consumers, to wich it sends messages, i.e. the subscriber pattern. A Consumer has a Receive function that is called to process a Message. Combining them yields a Transducer which both sends and receives.

Messages have type parameters, and all producers and consumers declare which types of messages they can receive. This way building-blocks are composeable while maintaining type safety. 


