# SDLE

## Description
Development of a command line local-first shopping list application. This application allows users to create/edit shopping lists and share them through a cloud componet. The local-first approach allows users to edit their shopping list in offline mode. The cloud componet was designed using the concept of consistent hashing allowing the addition and removal of nodes/servers for high availability and scalability. This architecture was inspired by the [Amazon DynamoDB paper](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf). To ensure seamless collaboration, Conflict-free Replicated Data Types (CRDTs) were implemented.

## Presentation/ Demo

https://github.com/Moreira-26/FEUP-SDLE/assets/93392622/a8e43d61-f375-4066-aefa-bd907a2e20af

## Group G77

| Name             | Number    |
| ---------------- | --------- |
| Bruna Marques         | 202007191 |
| Hugo Gomes         | 202004343 |
| João Moreira         | 202005035 |
| Lia Vieira         | 202005042 |


## Prerequisites

- Install maven 
- Install openJDK 21

## Compile 

From the ShoppingListApp directory

```
    mvn package
```

## Run

From the ShoppingListApp directory

```
    java -cp target/ShoppingList-1.0-SNAPSHOT.jar <className>
```

Example:

Node:

```
    java -cp target/ShoppingList-1.0-SNAPSHOT.jar sdle.server.Server 5000 node1
```

Client:
```
    java -cp target/ShoppingList-1.0-SNAPSHOT.jar sdle.client.Cli -u user1 create shop1
```

