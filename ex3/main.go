package main

import (
	"math/rand"
	"time"

	"ex3/model"
	"ex3/player"
	"ex3/referee" // Update this path if your referee package is local, e.g. "./referee"
)

func main() {
	rand.Seed(time.Now().UnixNano())

	player1Chan := make(chan chan model.Move)
	player2Chan := make(chan chan model.Move)

	go player.Player("Player 1", player1Chan)
	go player.Player("Player 2", player2Chan)

	go referee.Referee(player1Chan, player2Chan)

	select {}
}
