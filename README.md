# Learn Fauna a Hands on Approach
In this series I will take you through the examples I used to get a handle on how to use FaunaDB. These examples are available in a number of languages include Python, Java, Scala and Go at this time. As you can see here: https://fauna.com/documentation/reference, Fauna supports a variety of languages and if you have a favorite, perhaps you could recreate these examples and share them back.

Note that each lesson is in a separate **git branch**

## Prerequisites
The early examples will be based on a local "Developers" copy of Fauna. This is a single node that is quite easy to work with as it only requires a JDK v1.8 or newer be installed on the host machine. The download can be found [here](https://fauna.com/releases). Download and extract to a directory of your choice. Once in that directory, execute the command below and the outlook should look like this.

```
$ java -jar faunadb-developer.jar

aunaDB Developer Edition 2.5.2-2777f20
======================================
Starting...
No configuration file specified; loading defaults...
Data path: ./data
Temp path: ./data/tmp
FaunaDB is ready.
API endpoint: 127.0.0.1:8443

```
An alternative is to use the FaunaDB Cloud. You can follow the instructions [here](https://fauna.com/serverless) and create a developers account. We will cover how to alter the endpoints later if you choose to go this route.

(Optional) It is also very instructive to use the fauna-dashboard to examine the results of the code examples. You can get a copy of the dashboard from GitHub [here](https://github.com/fauna/dashboard). This requires that you have npm installed. The additional instructions needed to run the dashboard are contained in the "README.md" file.

These  examples were developed using the various versions of the JetBrains tools including IntelliJ. If you use these tools you should be able to import the projects directly.

## [Lesson1 - Connect and Create a DB](https://github.com/retroryan/learn-fauna-scala/tree/lesson1)

In branch **lesson1**

Introduced in this example is a connection to the FaunaDb using the "admin" client. We also use this client to create and delete a database.

The remaining lessons are developed in the context of a simple ledger data model. The examples will leverage the concept of 'customers' and 'transactions'.

## Lesson2 - Connect, Create DB, Schema, Index and perform basic CRUD activities.

In branch **lesson2**

Securely connect to a specific DB and build out a simple ledger schema including an index that allows us to access records by id. We create a record, read it, update it, read it, and then delete the record. So simple CRUD activity.
Of note, notice that DB creation takes advantage of the ability to include logic in a query request. In this case we are checking the existence of the database before creating and deleting if necessary.

## Lesson3 - Query Patterns and New Index Types

In branch **lesson3**

Deeper dive into query patterns using indexes. Specifically we add a new index type using values as opposed to terms. This will allow us to perform range style queries. Many of the examples take advantage of this approach. Also included are examples of using the various composite commands including mapping functions within the client query. Finally in this Lesson exmplore aa more general example of paging across all the instances in a calss.

## Lesson4 - Complex Transactions and Instance Member Access

In branch **lesson4**

This lessons presents a couple of new more advanced interactions. The first is a general approach to creating a larger set of instances by passing logic down to the DB. Also developed is an example of accessing individual members returned from a query. Finally we introduce a simple version of a complex transaction that demonstrates a double entry ledger style transaction. This example introduces a number of new Fauna Query Language commands.
