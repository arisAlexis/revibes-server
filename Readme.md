# Introduction

**Revibes** is a social network with a purpose.

Users can be part of an altruistic community in which people help each other. This game awards you with 1 *good vibe* for every person that you help somehow with the actual outcome not being important.

Meeting your peers, your neighbors and asking them for help or helping them out makes human bonding easier and fills you with good vibes that you can show off!  
The most important feature and the gist of all the network is to ask for help to your friends and their circle,neighbours and people that are close.  
It is much easier to get help from someone that is connected to you in some way.

In the spirit of [couchsurfing](http://www.couchsurfing.com) and [timebanks](https://en.wikipedia.org/wiki/Time-based_currency).

**Main features**

* Connect with friends,follow and chat with them
* Create standard events, ask for help or offer
* Use tags to describe what you need and what you can do
* Geolocation for streams in your neighbourhood
* Streams from friends, geolocation or topics/tags you subscribe

Please read more about how it works and technical information at  the detailed [wiki](https://github.com/arisAlexis/revibes-server/wiki/) section.

# Questions

**Is the project alive?**

This project started with a goal of forming a local startup in Amsterdam. That did not happen with the most obvious
reason of having no monetization plan and we concluded it would serve better as an open platform and involve other people.
This project can be continued or used as a base for ideas and code.

**How can I help?**

What the project needs:

* backend Java developer
* frontend web developer
* mobile developer for any platform
* ui/ux designer
* **PULL REQUESTS**

**How can I use your code?**

Any way you like. The code itself may be a bit complex as only one dev was working on it and it didn't start
as OSS (so sloppy code was allowed :) but you could fork it and take ideas about building a social network.

Or you could just contribute here.

**What would the future look like if people joined in the project?**

Revibes could be a not-for-profit open source organization.
Given enough users money to run the servers could be contributed from Accelerators or cloud services.

**What happened to the frontend?**

With limited time, I started the frontend on tools that in the Javascript world are considered ancient right now
and then a lot of changes happened at the backend. I decided not to rewrite it.

# Technical

This repo contains the backend REST(ish) server.

The main technologies being used are:

* [OrientDB](http://www.orientdb.com) as a multi-paradigm NoSQL Graph Database
* [Gremlin](https://github.com/tinkerpop/gremlin) Graph traversal language
* [Restfb](http://restfb.com) library for Facebook interaction
* Amazon services S3,SES for file uploading and emails
* Stanford's [Core NLP](http://stanfordnlp.github.io/CoreNLP/)
* Java 8 (including streams,lambda expressions etc)
* Jersey for handling the REST endpoints
* Jackson as a POJO mapper
* Hibernate (validator only)
* Apache Tika

Read more at the [wiki](https://github.com/arisAlexis/revibes-server/wiki/).

# Quick Instructions

Prerequisites:

* maven3
* Java 8 JDK

`git clone`

`mvn test`
This will run all integration tests using an in-memory orientdb database. More in the wiki tests section.

# Installation

1. Download and install the OrientDB server.
2. Run in the console the commands included in the file src/test/resources/test_data/orient_exchange_structure.script
3. Populate the database running the commands in the console of file src/test/resources/test_data/orient_exchange_insert.script
4. Edit src/main/webapp/web.xml file with all the configuration and passwords
5. `mvn package -Dmaven.test.skip=true`
6. Download and install Tomcat 8 or any other application server to run the rest.war that should be mounted on the /exchange path
7. Install an SSL certificate for your application server for all paths
8. Pray and point your browser at *https://localhost:8443/exchange/rest/activities*

# License

The MIT License (MIT)

Copyright (c) 2014-2015 Aris Giachnis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
