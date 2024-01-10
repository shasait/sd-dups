# sd-dups

[![Maven Central](https://img.shields.io/maven-central/v/de.hasait.sd-dups/sd-dups.svg?label=Maven%20Central)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.hasait.sd-dups%22%20AND%20a%3A%22sd-dups%22)

Search and Destroy duplicate files.

# Installation / How to start

* Download latest jar.
* Create `users.json` in same directory. Use that one as starting point: [users.json](users.json) 
* Launch jar using Java 17: `java -jar sd-dups-*.jar`
* Open browser and connect to [http://localhost:8083/](http://localhost:8083/)

# Usage

* Log in
* On the main screen enter path to search for duplicate files:
```
/home/my-user
/srv/groups/my-team
```
* Optionally add some regexps for excluding files:
```
.*/tmp/.*
.*\.bak
```
* Click on `Scan`
* Wait and click `Refresh` to check if scanning is already finished
* Select one or multiple found entries
* Click on `Delete` to delete them permanently
* Click on `Clear` to restart from the beginning
