This is my back-dated mail processing application.

Background
----------
The idea is that I like to keep all my incoming email in the Inbox so I don't have to switch folders to see new stuff. However, later on I would like it to be sorted and filtered in some ways.

This program runs a set of filters against email, but only on messages older than a configured age. It is intended to be run regularly from cron.

It talks directly to the mail server so it does not require any particular client software. In theory it supports any mail server supported by Java Mail, but I have only tried it with IMAP servers.

Configuration Languages
-----------------------
The program is designed to support different languages for specifying the configuration. 

Currently: Java properties files, or Javascript. (Javascript uses the Rhino engine since I wrote it years ago, so isn't that up to date)

Requirements
------------
Requires a recent Java JDK.

Installation
------------

	git clone git://github.com/edward3h/mymailtool.git
	cd mymailtool
	./gradlew install

Setup
-----
MyMailtool can be run with command line options, but it is expected that most users will use a configuration file or files.

By default it will try and read `.mymailtoolrc.properties` in the user's home directory.

To run, use the `bin/mymailtool2` script. Pass `--help` to see command line options.

