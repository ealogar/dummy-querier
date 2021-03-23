# dummy-querier
A simple docker java application to run queries against a postgresql database.
It will provide itself the required stuff, run 10 queries and 1 insert in a loop.
It will maintain a single connection opened during the configured time againsta database and will retry
if the connection is lost.

# Configuration
* JDBC_URL: the connection query (it must include the database name), by default jdbc:postgresql://localhost:5432/oic?socketTimeout=10
* DB_USERNAME: the username to conne t
* DB_PASSWORD: the password to connect
* SLEEP_TIME: time to sleep before running queries, by default 10000 milliseconds
* MAXLIFE_TIME: maximum life of connection, by default 30000 milliseconds

# Delivery

## Building

Run the command 
```
docker build -t ealogar/dummy-querier:1.0 .
```

## Publishing

Run the command 
```
docker push ealogar/dummy-querier:1.0
```
