PCD a.y. 2024-2025 - ISI LM UNIBO - Cesena Campus

# Assignment #03 -  Message Passing and Distributed Computing

v1.0.0-20250605

**1) About Asyncronous Message Passing with Actors (not distributed)**

Develop an actor-based solution of the *Concurrent Boids* problem, presented in Assignment #01, with the same GUI. Requirements (analogous to the first assignment):
- The solutions should exploit as much as possible  the key features of the actor programming model, both at the design and implementation level. 
- The solution should promote modularity, encapsulation as well as performance, reactivity. 
- The suggested framework to use is Akka, either using the Scala or Java API, following the guidelines presented in Lab. Nevertheless, a different framework and different languages than Scala or Java can be used, as far as an actor-based approach is adopted.

For every aspect not specified, students are free to choose the best approach for them.


**2) Distributed Computing** 

Develop a distributed implementation of Agar.io game, starting from a not distributed version (by G. Aguzzi)
- [Full description](https://github.com/cric96/pcd-assignment-2025/tree/main), based on Akka Distributed Actors
- Alternatively, the distributed implemementation can be based on a MOM (e.g. RabbitMQ), as presented in [Lab Activity 20250606](https://github.com/pcd-2024-2025/lab-10)


**3) Facultative points - mandatory only for getting max grade (30 or 30L)** 

- *About Synchronous Message Passing in Go* -- Develop a simple simulation of the [Rock-Paper-Scissors game](https://it.wikipedia.org/wiki/Morra_cinese), with a referee and two (bot) players playing the game for ever, turn by turn. At each turn: 
  - the referee asks the moves and communicate the winner;
  - the players randomly select and communicate their move, and print if either they won and lose, including their current score (incremented everytime they win).      


- *About Java RMI* -- Develop a distributed implementation of the game presented in point (2) using also Java RMI, as presented in [Lab Activity 20250606](https://github.com/pcd-2024-2025/lab-10)


### The deliverable

The deliverable must be a zipped directory `Assignment-03`, to be submitted on the course web site, including a different sub-directory for each points developed.  

Each sub-directory should be structured as follows:
- `src` directory with sources
- `doc` directory with a short report in PDF (`report.pdf`). The report should include:
	- A brief analsysis of the problem, focusing in particular aspects that are relevant from concurrent point of view.
	- A description of the adopted design, the strategy and architecture.


