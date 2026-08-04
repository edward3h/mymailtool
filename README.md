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

Scan Cache
----------
Since it's meant to run from cron against the same mailboxes over and over, mymailtool remembers how far it got in each folder (a UID high-water-mark) and skips old messages it's already looked at on the next run. This is what keeps it fast even on a big mailbox with years of mail sitting in it.

If your config files change, it forgets everything and does a full rescan next run. It also does a full rescan of a folder if the folder's UIDVALIDITY changes (e.g. it was rebuilt on the server). Pass `--no-scan-cache` (or set `mymailtool.noscancache`/`disableScanCache` in your config, depending on which config format you're using) to force a full rescan every time.

Known limitation: once mymailtool has looked at a message and it didn't match anything, that's it - it won't look at that message again, even if something about it changes later (e.g. you go and flag it manually in your normal mail client, and you've got a rule that matches on flags). If you hit this, run once with `--no-scan-cache` to force a fresh look.

The scan state is stored in a small properties file, by default somewhere sensible for your OS (e.g. `~/.cache/mymailtool/scan-state.properties` on Linux). You can point it elsewhere with `mymailtool.scanstatefile`/`scanStateFile` in your config.

