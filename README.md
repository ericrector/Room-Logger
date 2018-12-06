# About this project

## Why 

The reason for this project is two-fold.  First and main is I found that mold was forming in the corners of my bedroom in a new flat that I am renting and I wanted to find the cause. To do this I would have to track the room temperature and humididy. After analying the data you can determine is you have heat more, air the room more or if there moisture coming thought the wall.

Second is I love programming and automation so I thought this would be a great change to combined them all together to produce a usefull soultion.

## Prereqs

To get things running you need Java, Maven and Docker. Which version.... this topic always sucks. I have gone through so many tutoial and they never work because my version is newer than the versions used when the tutioial was written.  I don't really have a solution other that you should know a bit of:

- Java
- Maven
- Raspberry pi
- Databases
- Nginx / PHP

and later:

- Go
- Spring boot


## Desription

So, the idea is that in the end I would have a Raspberry Pi that is running a db, some sort of service that accepts connections form a sensor and adds the values to the db. Then there should be some sort of web front end to view the temperature and humidity.

For the db I choose Postgresql, why? does is really matter? Whether you used H2, SQL lite, Mysql, Maria, Postgres for such simple project you can take your pick.  I also plan to use Hiberate so then the db is really not an issue.

Next lets look at a sensor.  I started with the wemos d1 mini the a DHT22 temperature-humidity sensor. Then to make things I bit more interesting and easier I moved to the Sonnoff Basic. Theo Arends has a great project that makes using the sensor just a matter of configureation.  Plus with the sonoff you can do other things like switch a light off and on. Another added extra is the MQTT support and installing a broker on the Pi is also a no brainer.

Here is a link to his project:
https://github.com/arendst/Sonoff-Tasmota

Then I wrote a very simple java service that listens for messages published from the sonoff and inserts the data in to the database.

For the frontend to make this quick and easy I used Highcharts js libraies to visualize the data. Adding nginx and PHP-5 to the Pi again no problem. All you have to do is write a simple php and html script to link it all together and now I can easily monitor the temperature and humidity in every room of my flat.

Here is a link to their stuff:
https://www.highcharts.com/stock/demo

To develope the system what better way than to use Docker.

## How to uses this project

- Check it out
- Build the service using maven
- Run docker compose
- either pre populate the db with come code or run some dummy sensors for a real-time effect
Then when it all works:
- deploy every thing on a Pi and Sonoff(s)

Details at soon to come...

# Next steps are to use

- Use Go for the sevice instead of java.
- Use Spring Boot, GraphQL and Highcharts to access and visualize the data.
- Try and deploy the project on Kubernetes (I already have a cluster of Pis running ;-)

